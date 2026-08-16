package com.rate.boardcastmod.config;

public enum HeartDisplayStyle {
    /**
     * Draw one heart glyph per full HP pair. The optional half heart is drawn
     * with the configured half-heart colour so 7 HP is four glyphs with the
     * last glyph dimmed.
     */
    ICONS,
    /**
     * Draw "3.5 ❤" style output. This is the least ambiguous way to show half
     * hearts and is also used in CSV score_display columns.
     */
    NUMBER_WITH_HEART
}
