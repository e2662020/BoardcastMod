package com.rate.boardcastmod.config;

public enum HealthDisplayMode {
    /**
     * Always render the raw score as a number.
     */
    NUMBER,
    /**
     * Always render the score as hearts. One heart equals two HP.
     */
    HEARTS,
    /**
     * Render hearts when the objective uses the vanilla hearts render type or
     * its name/criterion looks like a health objective, otherwise use a number.
     */
    AUTO
}
