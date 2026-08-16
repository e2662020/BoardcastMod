package com.rate.boardcastmod.render;

import com.rate.boardcastmod.BoardcastMod;
import com.rate.boardcastmod.config.BoardcastConfig;
import com.rate.boardcastmod.export.ScoreboardFormatting;
import com.rate.boardcastmod.util.ColorUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SidebarRenderer {
    private static final int SIDE_PADDING = 5;
    private static final int VERTICAL_PADDING = 3;
    private static final int HEART_SIZE = 9;
    private static final int ROW_GAP = 4;
    private static final int BACKGROUND_INSET = 4;

    private static final Identifier FULL_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HALF_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/half");

    private SidebarRenderer() {
    }

    public static boolean isSidebarHidden() {
        BoardcastConfig cfg = BoardcastMod.config();
        if (!cfg.showSidebar) {
            return true;
        }
        if (cfg.obsOverlayAutoHide && isObsOverlayLoaded()) {
            return true;
        }
        return false;
    }

    public static boolean shouldUseCustomRenderer() {
        return BoardcastMod.config().useCustomStyle && !isSidebarHidden();
    }

    private static boolean isObsOverlayLoaded() {
        // zziger/obs-overlay has used the id obs_overlay; accept the dashed
        // variant as well so renamed builds still trigger the compatibility
        // hide.
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("obs_overlay") || loader.isModLoaded("obs-overlay");
    }

    public static void render(DrawContext context, ScoreboardObjective objective) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || objective == null) {
            return;
        }

        BoardcastConfig cfg = BoardcastMod.config();
        Scoreboard scoreboard = client.world.getScoreboard();
        TextRenderer font = client.textRenderer;

        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(objective));
        // Match Scoreboard Overhaul: internal fake-player entries are not drawn.
        entries.removeIf(entry -> entry.owner() != null && entry.owner().startsWith("#"));
        entries.sort(Comparator.comparingInt(ScoreboardEntry::value).reversed()
                .thenComparing(ScoreboardEntry::owner));

        String titleString = cfg.sidebarTitleOverride == null || cfg.sidebarTitleOverride.isBlank()
                ? objective.getDisplayName().getString()
                : cfg.sidebarTitleOverride;
        Text title = ColorUtil.styled(titleString, cfg.titleColor, Formatting.RED);
        boolean drawTitle = cfg.showTitle && !titleString.isBlank();

        int maxRows = Math.min(30, Math.max(0, cfg.maxRows));
        int rowsToDraw = Math.min(maxRows, entries.size());
        if (rowsToDraw <= 0 && !drawTitle) {
            return;
        }
        List<Row> rows = new ArrayList<>(rowsToDraw);
        for (int i = 0; i < rowsToDraw; i++) {
            rows.add(buildRow(scoreboard, objective, entries.get(i), font, cfg));
        }

        int maxLineWidth = 0;
        for (Row row : rows) {
            int lineWidth = row.leftWidth();
            if (row.rightWidth() > 0) {
                lineWidth += ROW_GAP + row.rightWidth();
            }
            maxLineWidth = Math.max(maxLineWidth, lineWidth);
        }
        if (drawTitle) {
            maxLineWidth = Math.max(maxLineWidth, font.getWidth(title));
        }

        int rowHeight = Math.min(20, Math.max(8, cfg.rowHeight));
        int width = Math.max(Math.max(cfg.sidebarMinWidth, 10), maxLineWidth + SIDE_PADDING * 2);
        if (cfg.sidebarMaxWidth > 0) {
            width = Math.min(width, cfg.sidebarMaxWidth);
        }
        // Scoreboard Overhaul-style global scale: push a scaled matrix, then
        // measure the window in the same scaled space so the sidebar keeps its
        // on-screen position and size at any zoom level.
        float scale = cfg.sidebarScale / 100f;
        if (scale <= 0f) {
            scale = 1f;
        }
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.scale(scale, scale, 1f);
        try {
            int scaledWidth = Math.round(client.getWindow().getScaledWidth() / scale);
            int x = scaledWidth - width - cfg.sidebarRightMargin + cfg.sidebarXOffset;
            int y = cfg.sidebarTopMargin + cfg.sidebarYOffset;
            int height = VERTICAL_PADDING * 2 + rows.size() * rowHeight + (drawTitle ? rowHeight : 0);

            if (cfg.renderBackground) {
                context.fill(x + BACKGROUND_INSET, y + BACKGROUND_INSET,
                        x + width - BACKGROUND_INSET, y + height - BACKGROUND_INSET,
                        ColorUtil.parseArgb(cfg.backgroundColor, 0x66000000));
            }

            int cursorY = y + VERTICAL_PADDING;
            boolean shadow = cfg.sidebarTextShadow;
            if (drawTitle) {
                int titleWidth = font.getWidth(title);
                context.drawText(font, title, x + Math.max(0, (width - titleWidth) / 2), cursorY, 0xFFFFFFFF, shadow);
                cursorY += rowHeight;
            }

            for (Row row : rows) {
                int leftX = x + SIDE_PADDING;
                int rightX = x + width - SIDE_PADDING - row.rightWidth();
                if (!row.left().getString().isEmpty()) {
                    Text leftText = row.left();
                    if (cfg.truncateScoreNames) {
                        int availWidth = rightX - leftX - ROW_GAP;
                        if (availWidth > 0 && row.leftWidth() > availWidth) {
                            leftText = Text.literal(font.trimToWidth(leftText.getString(), availWidth));
                        }
                    }
                    context.drawText(font, leftText, leftX, cursorY, 0xFFFFFFFF, shadow);
                }
                if (row.hearts() != null) {
                    drawHearts(context, row.hearts(), rightX, cursorY, rowHeight);
                } else if (!row.right().getString().isEmpty()) {
                    context.drawText(font, row.right(), rightX, cursorY, 0xFFFFFFFF, shadow);
                }
                cursorY += rowHeight;
            }
        } finally {
            matrices.pop();
        }
    }

    private static void drawHearts(DrawContext context, ScoreboardFormatting.HeartSprites hearts,
                                   int x, int y, int rowHeight) {
        int heartY = y + (rowHeight - HEART_SIZE) / 2;
        for (int i = 0; i < hearts.full(); i++) {
            context.drawGuiTexture(FULL_HEART_TEXTURE, x + i * HEART_SIZE, heartY, HEART_SIZE, HEART_SIZE);
        }
        if (hearts.half() > 0) {
            context.drawGuiTexture(HALF_HEART_TEXTURE, x + hearts.full() * HEART_SIZE, heartY, HEART_SIZE, HEART_SIZE);
        }
    }

    private static Row buildRow(Scoreboard scoreboard, ScoreboardObjective objective, ScoreboardEntry entry,
                                TextRenderer font, BoardcastConfig cfg) {
        Text left = Text.empty();
        if (cfg.showScoreHolder) {
            left = decorateHolder(scoreboard, entry, cfg);
        }

        Text right = Text.empty();
        ScoreboardFormatting.HeartSprites hearts = null;
        if (cfg.showScoreValue) {
            hearts = ScoreboardFormatting.sidebarHeartSprites(objective, entry, cfg);
            if (hearts == null) {
                right = ScoreboardFormatting.formatForSidebar(objective, entry, cfg);
            }
        }
        int rightWidth = hearts == null ? font.getWidth(right) : hearts.width();
        return new Row(left, right, font.getWidth(left), rightWidth, hearts);
    }

    private static Text decorateHolder(Scoreboard scoreboard, ScoreboardEntry entry, BoardcastConfig cfg) {
        Text plainName = entry.name() == null ? Text.literal(entry.owner()) : entry.name();
        String rawName = plainName.getString();
        boolean defaultName = entry.owner() != null && rawName.equals(entry.owner());
        if (!cfg.showTeamAffixes) {
            return defaultName
                    ? ColorUtil.styled(rawName, cfg.holderColor, Formatting.WHITE)
                    : plainName;
        }
        Team team = scoreboard.getScoreHolderTeam(entry.owner());
        if (team == null) {
            // Keep custom server-provided names untouched, but apply the
            // configured holder colour to the default "owner as name" case.
            return defaultName
                    ? ColorUtil.styled(rawName, cfg.holderColor, Formatting.WHITE)
                    : plainName;
        }
        return Team.decorateName(team, plainName);
    }

    private record Row(Text left, Text right, int leftWidth, int rightWidth, ScoreboardFormatting.HeartSprites hearts) {
    }
}
