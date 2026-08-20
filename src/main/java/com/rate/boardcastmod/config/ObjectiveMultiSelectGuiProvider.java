package com.rate.boardcastmod.config;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Builds an {@link ObjectiveMultiSelectEntry} for the
 * {@code exportExtraObjectives} config field: instead of typing objective
 * names, the player ticks checkboxes for objectives collected live from the
 * current world's scoreboard.
 */
public final class ObjectiveMultiSelectGuiProvider implements GuiProvider {

    public static final ObjectiveMultiSelectGuiProvider INSTANCE = new ObjectiveMultiSelectGuiProvider();

    private ObjectiveMultiSelectGuiProvider() {
    }

    @Override
    public List<AbstractConfigListEntry> get(String i13n, Field field, Object config, Object defaults,
                                             GuiRegistryAccess registry) {
        List<String> value = Utils.getUnsafely(field, config, Collections.emptyList());
        List<String> defaultValue = Utils.getUnsafely(field, defaults, Collections.emptyList());

        ObjectiveMultiSelectEntry entry = new ObjectiveMultiSelectEntry(
                Text.translatable(i13n),
                value == null ? Collections.emptyList() : value,
                defaultValue == null ? Collections.emptyList() : defaultValue,
                Optional::empty,
                newValue -> Utils.setUnsafely(field, config, newValue)
        );
        return Collections.singletonList(entry);
    }
}
