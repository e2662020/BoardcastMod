package com.rate.boardcastmod.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link String} config field as a file path so the Cloth Config GUI
 * renders a "Browse" button next to the text field, opening the native file
 * picker (Windows Explorer / file manager) to select a path.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FilePath {
}
