package com.rate.boardcastmod;

import com.rate.boardcastmod.chat.ChatCapture;
import com.rate.boardcastmod.command.ClientCommands;
import com.rate.boardcastmod.config.BoardcastConfig;
import com.rate.boardcastmod.config.FilePath;
import com.rate.boardcastmod.config.FilePathGuiProvider;
import com.rate.boardcastmod.config.ObjectiveMultiSelectGuiProvider;
import com.rate.boardcastmod.export.ScoreboardExporter;
import com.rate.boardcastmod.key.KeyBindings;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BoardcastMod implements ClientModInitializer {
    public static final String MOD_ID = "boardcastmod";
    public static final Logger LOGGER = LoggerFactory.getLogger("BoardcastMod");

    @Override
    public void onInitializeClient() {
        AutoConfig.register(BoardcastConfig.class, GsonConfigSerializer::new);

        // Render a "Browse" button for every @FilePath option in the Cloth Config GUI.
        AutoConfig.getGuiRegistry(BoardcastConfig.class)
                .registerAnnotationProvider(FilePathGuiProvider.INSTANCE, FilePath.class);

        // "Extra columns from other objectives" is edited by ticking live-collected
        // scoreboard objectives instead of typing names.
        AutoConfig.getGuiRegistry(BoardcastConfig.class)
                .registerPredicateProvider(ObjectiveMultiSelectGuiProvider.INSTANCE,
                        field -> "exportExtraObjectives".equals(field.getName()));

        // Touch the config once so the file is created immediately.
        BoardcastConfig cfg = BoardcastMod.config();

        ChatCapture.register();
        KeyBindings.register();
        ClientCommands.register();

        ClientTickEvents.END_CLIENT_TICK.register(ScoreboardExporter::tick);
        ClientTickEvents.END_CLIENT_TICK.register(KeyBindings::tick);

        LOGGER.info("BoardcastMod initialized. export={}, chat={}, customSidebar={}",
                cfg.exportEnabled, cfg.chatCaptureEnabled, cfg.useCustomStyle);
    }

    public static BoardcastConfig config() {
        return AutoConfig.getConfigHolder(BoardcastConfig.class).getConfig();
    }

    public static void saveConfig() {
        try {
            AutoConfig.getConfigHolder(BoardcastConfig.class).save();
            ScoreboardExporter.requestExport();
        } catch (Throwable t) {
            LOGGER.warn("Failed to save BoardcastMod config", t);
        }
    }
}
