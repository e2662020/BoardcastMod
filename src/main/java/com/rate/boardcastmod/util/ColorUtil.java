package com.rate.boardcastmod.util;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ColorUtil {
    private ColorUtil() {
    }

    /**
     * Parses {@code #RRGGBB}, {@code #AARRGGBB} or {@code RRGGBB}.
     */
    public static int parseArgb(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        String value = raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        try {
            long argb = Long.parseLong(value, 16);
            if (value.length() <= 6) {
                argb |= 0xFF000000L;
            }
            return (int) argb;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    /**
     * Parses an RGB colour for a text style. Returns {@code -1} when the value
     * is invalid so the caller can fall back to a vanilla formatting colour.
     */
    public static int parseRgb(String raw) {
        if (raw == null) {
            return -1;
        }
        String value = raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6 && value.length() != 8) {
            return -1;
        }
        try {
            long rgb = Long.parseLong(value, 16);
            return (int) (rgb & 0xFFFFFFL);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static Text styled(String content, String hex, Formatting fallback) {
        int rgb = parseRgb(hex);
        Style style;
        if (rgb >= 0) {
            style = Style.EMPTY.withColor(rgb);
        } else {
            style = Style.EMPTY.withFormatting(fallback);
        }
        return Text.literal(content).setStyle(style);
    }
}
