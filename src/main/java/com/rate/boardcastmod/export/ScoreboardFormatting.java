package com.rate.boardcastmod.export;

import com.rate.boardcastmod.config.BoardcastConfig;
import com.rate.boardcastmod.config.HealthDisplayMode;
import com.rate.boardcastmod.config.HeartDisplayStyle;
import com.rate.boardcastmod.util.ColorUtil;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

public final class ScoreboardFormatting {
    private ScoreboardFormatting() {
    }

    public static Text formatForSidebar(ScoreboardObjective objective, ScoreboardEntry entry, BoardcastConfig cfg) {
        if (entry == null) {
            return Text.empty();
        }
        int score = entry.value();
        if (shouldRenderHearts(objective, cfg)) {
            return heartText(score, cfg);
        }
        if (isTimerObjective(objective, cfg)) {
            return ColorUtil.styled(formatTimer(score, cfg), cfg.scoreColor, Formatting.RED);
        }
        if (entry.hidden()) {
            return Text.empty();
        }
        NumberFormat override = entry.numberFormatOverride();
        if (override != null) {
            return isBlankNumberFormat(override) ? Text.empty() : override.format(score);
        }
        NumberFormat objectiveFormat = objective == null ? null : objective.getNumberFormat();
        if (objectiveFormat != null && !(objectiveFormat instanceof StyledNumberFormat)) {
            return isBlankNumberFormat(objectiveFormat) ? Text.empty() : objectiveFormat.format(score);
        }
        return ColorUtil.styled(formatSidebarNumber(score, cfg), cfg.scoreColor, Formatting.RED);
    }

    private static String formatSidebarNumber(int score, BoardcastConfig cfg) {
        if (!cfg.compactScoreValues || score < 0) {
            return String.format(Locale.ROOT, "%,d", score);
        }
        if (score >= 1_000_000) {
            return (score / 1_000_000) + "M";
        }
        if (score >= 1_000) {
            return (score / 1_000) + "K";
        }
        return Integer.toString(score);
    }

    private static boolean isBlankNumberFormat(NumberFormat format) {
        return format instanceof BlankNumberFormat;
    }

    public static boolean shouldRenderHearts(ScoreboardObjective objective, BoardcastConfig cfg) {
        if (objective == null) {
            return false;
        }
        if (cfg.healthDisplay == HealthDisplayMode.NUMBER) {
            return false;
        }
        if (cfg.healthDisplay == HealthDisplayMode.HEARTS) {
            return true;
        }
        // AUTO
        if (objective.getRenderType() == ScoreboardCriterion.RenderType.HEARTS) {
            return true;
        }
        String name = objective.getName().toLowerCase(Locale.ROOT);
        String displayName = objective.getDisplayName().getString().toLowerCase(Locale.ROOT);
        String rawCriterion = objective.getCriterion().getName();
        String criterion = rawCriterion == null ? "" : rawCriterion.toLowerCase(Locale.ROOT);
        String[] keywords = {"health", "hp", "heart", "hearts", "生命", "血量", "血"};
        for (String keyword : keywords) {
            if (name.contains(keyword) || displayName.contains(keyword) || criterion.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTimerObjective(ScoreboardObjective objective, BoardcastConfig cfg) {
        if (objective == null || !cfg.timerEnabled) {
            return false;
        }
        String name = objective.getName().toLowerCase(Locale.ROOT);
        String displayName = objective.getDisplayName().getString().toLowerCase(Locale.ROOT);
        String rawCriterion = objective.getCriterion().getName();
        String criterion = rawCriterion == null ? "" : rawCriterion.toLowerCase(Locale.ROOT);
        if (criterion.contains("timer") || criterion.contains("time")) {
            return true;
        }
        if (cfg.timerObjectiveKeywords != null) {
            for (String raw : cfg.timerObjectiveKeywords) {
                String keyword = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
                if (!keyword.isEmpty() && (name.contains(keyword) || displayName.contains(keyword))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String formatTimer(int rawScore, BoardcastConfig cfg) {
        if (!cfg.timerUseSeconds) {
            return Integer.toString(rawScore);
        }

        double seconds = rawScore;
        if (cfg.timerAssumeTicks) {
            seconds = rawScore / (double) Math.max(1, cfg.timerTicksPerSecond);
        }

        String sign = seconds < 0 ? "-" : "";
        double abs = Math.abs(seconds);
        String body = cfg.timerUseClockFormat ? formatClock(abs) : formatDecimal(abs) + "s";
        return sign + body;
    }

    public static Text heartText(int hp, BoardcastConfig cfg) {
        if (hp < 0) {
            return ColorUtil.styled(Integer.toString(hp), cfg.scoreColor, Formatting.RED);
        }

        // The ICONS style is drawn as vanilla heart sprites by the sidebar
        // renderer (see SidebarRenderer), so there is no text to render here.
        if (cfg.heartDisplayStyle == HeartDisplayStyle.ICONS) {
            return Text.empty();
        }

        String fullSymbol = cfg.fullHeartSymbol == null || cfg.fullHeartSymbol.isEmpty() ? "♥" : cfg.fullHeartSymbol;
        return ColorUtil.styled(formatDecimal(hp / 2.0) + " " + fullSymbol, cfg.fullHeartColor, Formatting.RED);
    }

    /**
     * The number of full and half vanilla heart sprites to draw for a health
     * score, or {@code null} when the value should be shown as text/number.
     */
    public static HeartSprites sidebarHeartSprites(ScoreboardObjective objective, ScoreboardEntry entry, BoardcastConfig cfg) {
        if (entry == null || entry.value() < 0) {
            return null;
        }
        if (cfg.heartDisplayStyle != HeartDisplayStyle.ICONS) {
            return null;
        }
        if (!shouldRenderHearts(objective, cfg)) {
            return null;
        }
        int hp = entry.value();
        int full = Math.min(30, hp / 2);
        int half = (hp % 2 != 0 && hp < 60) ? 1 : 0;
        return new HeartSprites(full, half);
    }

    public record HeartSprites(int full, int half) {
        public int count() {
            return full + half;
        }

        public int width() {
            return count() * 9;
        }
    }

    public static String formatDecimal(double value) {
        String formatted = String.format(Locale.ROOT, "%.1f", value);
        if (formatted.endsWith(".0")) {
            return formatted.substring(0, formatted.length() - 2);
        }
        return formatted;
    }

    public static String formatClock(double seconds) {
        long totalTenths = Math.round(seconds * 10.0);
        long totalSeconds = totalTenths / 10;
        long tenths = totalTenths % 10;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs);
        }
        if (totalSeconds >= 60) {
            return String.format(Locale.ROOT, "%02d:%02d", minutes, secs);
        }
        if (tenths != 0) {
            return String.format(Locale.ROOT, "0:%02d.%d", secs, tenths);
        }
        return String.format(Locale.ROOT, "0:%02d", secs);
    }
}
