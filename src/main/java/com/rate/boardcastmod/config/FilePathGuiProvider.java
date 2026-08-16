package com.rate.boardcastmod.config;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/**
 * Builds a {@link FilePathEntry} for every field annotated with {@link FilePath},
 * so the Cloth Config GUI shows a "Browse" button for path options.
 */
public final class FilePathGuiProvider implements GuiProvider {

    public static final FilePathGuiProvider INSTANCE = new FilePathGuiProvider();

    private FilePathGuiProvider() {
    }

    @Override
    public List<AbstractConfigListEntry> get(String i13n, Field field, Object config, Object defaults,
                                             GuiRegistryAccess registry) {
        String value = Utils.getUnsafely(field, config, "");
        String defaultValue = Utils.getUnsafely(field, defaults, "");

        FilePathEntry entry = new FilePathEntry(
                Text.translatable(i13n),
                value == null ? "" : value,
                defaultValue == null ? "" : defaultValue,
                newValue -> Utils.setUnsafely(field, config, newValue)
        );
        return Collections.singletonList(entry);
    }
}
