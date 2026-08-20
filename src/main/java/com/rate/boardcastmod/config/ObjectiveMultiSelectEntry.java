package com.rate.boardcastmod.config;

import com.rate.boardcastmod.util.ScoreboardAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A config entry for {@code exportExtraObjectives} that lets the player pick
 * scoreboard objectives with checkboxes instead of typing names.
 * <p>
 * The available options are collected <b>live</b> from the current world's
 * scoreboard: the list refreshes automatically whenever the set of objectives
 * changes (even on the same scoreboard instance), and a manual
 * <b>Refresh</b> button forces an immediate re-collection. Every checked
 * objective becomes one extra CSV column after {@code Player,Score} (3rd, 4th,
 * ...) in click order, filled with that player's score in the referenced
 * objective.
 */
@SuppressWarnings("deprecation") // cloth-config entry API deprecations, same as FilePathEntry
public final class ObjectiveMultiSelectEntry extends TooltipListEntry<List<String>> {

    private static final int LABEL_HEIGHT = 24;
    private static final int ROW_HEIGHT = 22;
    private static final int CHIP_HEIGHT = 20;
    private static final int SELECTED_BORDER_COLOR = 0xFF00AA00;

    private final List<String> selected = new ArrayList<>();
    private final List<String> original;
    private final List<String> defaultValue;
    private final List<Chip> chips = new ArrayList<>();
    private final ButtonWidget refreshButton;
    private final ButtonWidget clearButton;

    private List<ScoreboardObjective> liveObjectives = List.of();

    public ObjectiveMultiSelectEntry(Text fieldName, List<String> value, List<String> defaultValue,
                                     Supplier<Optional<Text[]>> tooltipSupplier,
                                     Consumer<List<String>> saveConsumer) {
        super(fieldName, tooltipSupplier);
        this.original = new ArrayList<>(value == null ? List.of() : value);
        this.defaultValue = new ArrayList<>(defaultValue == null ? List.of() : defaultValue);
        this.selected.addAll(this.original);
        this.saveCallback = saveConsumer;

        this.refreshButton = ButtonWidget.builder(
                        Text.translatable("text.autoconfig.boardcastmod.refresh_selection"),
                        button -> refreshLiveObjectives(true))
                .dimensions(0, 0, 0, 18)
                .build();
        this.clearButton = ButtonWidget.builder(
                        Text.translatable("text.autoconfig.boardcastmod.clear_selection"),
                        button -> selected.clear())
                .dimensions(0, 0, 0, 18)
                .build();
    }

    // ------------------------------------------------------------------
    // Value handling
    // ------------------------------------------------------------------

    @Override
    public List<String> getValue() {
        return new ArrayList<>(selected);
    }

    @Override
    public Optional<List<String>> getDefaultValue() {
        return Optional.of(new ArrayList<>(defaultValue));
    }

    @Override
    public boolean isEdited() {
        return !selected.equals(original);
    }

    @Override
    public void save() {
        if (saveCallback != null) {
            saveCallback.accept(getValue());
        }
    }

    // ------------------------------------------------------------------
    // Live objective collection
    // ------------------------------------------------------------------

    /**
     * Re-reads the objectives of the current world's scoreboard. The chip list
     * is rebuilt when the objective set actually changed (by name, so adding or
     * removing an objective triggers an update even on the same scoreboard
     * instance) or when {@code force} is set (the Refresh button).
     */
    private void refreshLiveObjectives(boolean force) {
        MinecraftClient client = MinecraftClient.getInstance();
        Scoreboard scoreboard = ScoreboardAccess.best(client);
        if (scoreboard == null) {
            if (!liveObjectives.isEmpty() || force) {
                liveObjectives = List.of();
                rebuildChips();
            }
            return;
        }
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        String sidebarObjectiveName = sidebar == null ? null : sidebar.getName();
        List<ScoreboardObjective> objectives = new ArrayList<>(scoreboard.getObjectives());
        // The sidebar objective is already the main "Score" column, so it is
        // not offered as an extra column.
        if (sidebarObjectiveName != null) {
            objectives.removeIf(objective -> objective.getName().equals(sidebarObjectiveName));
        }
        objectives.sort(Comparator.comparing(ScoreboardObjective::getName));
        if (!force && sameObjectives(objectives, liveObjectives)) {
            return;
        }
        liveObjectives = objectives;
        rebuildChips();
    }

    private static boolean sameObjectives(List<ScoreboardObjective> a, List<ScoreboardObjective> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).getName().equals(b.get(i).getName())) {
                return false;
            }
        }
        return true;
    }

    private void rebuildChips() {
        chips.clear();
        for (ScoreboardObjective objective : liveObjectives) {
            String name = objective.getName();
            ButtonWidget button = ButtonWidget.builder(Text.literal(name), btn -> toggle(name))
                    .dimensions(0, 0, 0, CHIP_HEIGHT)
                    .build();
            chips.add(new Chip(name, button));
        }
    }

    private void toggle(String name) {
        if (!selected.remove(name)) {
            selected.add(name);
        }
    }

    // ------------------------------------------------------------------
    // Widget plumbing
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        refreshLiveObjectives(false);
    }

    @Override
    public List<? extends Element> children() {
        List<Element> children = new ArrayList<>(chips.size() + 2);
        for (Chip chip : chips) {
            children.add(chip.button);
        }
        children.add(refreshButton);
        children.add(clearButton);
        return children;
    }

    @Override
    public List<? extends Selectable> narratables() {
        return children().stream().map(e -> (Selectable) e).toList();
    }

    @Override
    public int getItemHeight() {
        refreshLiveObjectives(false);
        return LABEL_HEIGHT + (chips.isEmpty() ? 20 : chips.size() * ROW_HEIGHT) + 4;
    }

    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean isHovered, float delta) {
        refreshLiveObjectives(false);
        super.render(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

        MinecraftClient client = MinecraftClient.getInstance();
        boolean editable = isEditable();

        // Option label (left), [Refresh] [Clear] buttons (right).
        context.drawTextWithShadow(client.textRenderer, getDisplayedFieldName(), x, y + 6, getPreferredTextColor());
        String refreshLabel = Text.translatable("text.autoconfig.boardcastmod.refresh_selection").getString();
        String clearLabel = Text.translatable("text.autoconfig.boardcastmod.clear_selection").getString();
        int refreshWidth = Math.max(40, client.textRenderer.getWidth(refreshLabel) + 12);
        int clearWidth = Math.max(40, client.textRenderer.getWidth(clearLabel) + 12);
        int buttonsWidth = refreshWidth + clearWidth + 4;

        refreshButton.setX(x + entryWidth - buttonsWidth);
        refreshButton.setY(y + 3);
        refreshButton.setWidth(refreshWidth);
        refreshButton.active = editable;
        refreshButton.render(context, mouseX, mouseY, delta);

        clearButton.setX(x + entryWidth - clearWidth);
        clearButton.setY(y + 3);
        clearButton.setWidth(clearWidth);
        clearButton.active = editable;
        clearButton.render(context, mouseX, mouseY, delta);

        int chipY = y + LABEL_HEIGHT;
        if (chips.isEmpty()) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.translatable("text.autoconfig.boardcastmod.no_objectives"),
                    x + 4, chipY + 5, 0xFFAAAAAA);
            return;
        }

        int chipX = x + 4;
        int chipWidth = Math.max(60, entryWidth - 8);
        for (Chip chip : chips) {
            ButtonWidget button = chip.button;
            boolean checked = selected.contains(chip.objectiveName);
            button.setX(chipX);
            button.setY(chipY);
            button.setWidth(chipWidth);
            button.setHeight(CHIP_HEIGHT);
            button.active = editable;
            button.setMessage(Text.literal(chipLabel(chip.objectiveName, checked)));
            button.render(context, mouseX, mouseY, delta);
            if (checked) {
                drawSelectedBorder(context, button);
            }
            chipY += ROW_HEIGHT;
        }
    }

    private String chipLabel(String name, boolean checked) {
        if (!checked) {
            return "\u2610 " + name; // ☐
        }
        int column = selected.indexOf(name) + 3; // columns start at 3 (Player, Score, ...)
        return "\u2611 " + column + ". " + name; // ☑ n. name
    }

    private void drawSelectedBorder(DrawContext context, ButtonWidget button) {
        int bx = button.getX();
        int by = button.getY();
        int bw = button.getWidth();
        int bh = button.getHeight();
        context.fill(bx - 1, by - 1, bx + bw + 1, by, SELECTED_BORDER_COLOR);
        context.fill(bx - 1, by + bh, bx + bw + 1, by + bh + 1, SELECTED_BORDER_COLOR);
        context.fill(bx - 1, by, bx, by + bh, SELECTED_BORDER_COLOR);
        context.fill(bx + bw, by, bx + bw + 1, by + bh, SELECTED_BORDER_COLOR);
    }

    private record Chip(String objectiveName, ButtonWidget button) {
    }
}
