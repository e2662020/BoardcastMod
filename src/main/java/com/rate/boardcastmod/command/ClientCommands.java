package com.rate.boardcastmod.command;

import com.mojang.brigadier.context.CommandContext;
import com.rate.boardcastmod.BoardcastMod;
import com.rate.boardcastmod.config.BoardcastConfig;
import com.rate.boardcastmod.export.ScoreboardExporter;
import com.rate.boardcastmod.render.SidebarRenderer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class ClientCommands {
    private ClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("boardcastmod")
                    .executes(ClientCommands::showHelp)
                    .then(ClientCommandManager.literal("export")
                            .executes(ClientCommands::exportCsv))
                    .then(ClientCommandManager.literal("csv")
                            .executes(ClientCommands::exportCsv))
                    .then(ClientCommandManager.literal("sidebar")
                            .then(ClientCommandManager.literal("hide").executes(ctx -> setSidebar(ctx, false)))
                            .then(ClientCommandManager.literal("show").executes(ctx -> setSidebar(ctx, true)))
                            .then(ClientCommandManager.literal("toggle").executes(ctx -> toggleSidebar(ctx))))
                    .then(ClientCommandManager.literal("timer")
                            .then(ClientCommandManager.literal("seconds").executes(ctx -> toggleTimerSeconds(ctx)))
                            .then(ClientCommandManager.literal("clock").executes(ctx -> toggleTimerClock(ctx))))
            );
        });
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.translatable("message.boardcastmod.help"));
        return 1;
    }

    private static int exportCsv(CommandContext<FabricClientCommandSource> context) {
        ScoreboardExporter.requestExport();
        context.getSource().sendFeedback(Text.translatable("message.boardcastmod.export_queued"));
        return 1;
    }

    private static int toggleSidebar(CommandContext<FabricClientCommandSource> context) {
        return setSidebar(context, !SidebarRenderer.isSidebarHidden());
    }

    private static int setSidebar(CommandContext<FabricClientCommandSource> context, boolean visible) {
        BoardcastConfig cfg = BoardcastMod.config();
        cfg.showSidebar = visible;
        if (visible) {
            // "show" is explicit, even when obs-overlay is installed.
            cfg.obsOverlayAutoHide = false;
        }
        BoardcastMod.saveConfig();
        context.getSource().sendFeedback(Text.translatable(visible
                ? "message.boardcastmod.sidebar_visible"
                : "message.boardcastmod.sidebar_hidden"));
        return 1;
    }

    private static int toggleTimerSeconds(CommandContext<FabricClientCommandSource> context) {
        BoardcastConfig cfg = BoardcastMod.config();
        cfg.timerUseSeconds = !cfg.timerUseSeconds;
        BoardcastMod.saveConfig();
        context.getSource().sendFeedback(Text.translatable(
                "message.boardcastmod.timer_seconds",
                cfg.timerUseSeconds
                        ? Text.translatable("message.boardcastmod.state_on")
                        : Text.translatable("message.boardcastmod.state_off")
        ));
        return 1;
    }

    private static int toggleTimerClock(CommandContext<FabricClientCommandSource> context) {
        BoardcastConfig cfg = BoardcastMod.config();
        cfg.timerUseClockFormat = !cfg.timerUseClockFormat;
        if (cfg.timerUseClockFormat) {
            // A clock only makes sense when seconds conversion is active.
            cfg.timerUseSeconds = true;
        }
        BoardcastMod.saveConfig();
        context.getSource().sendFeedback(Text.translatable(
                "message.boardcastmod.timer_clock",
                cfg.timerUseClockFormat
                        ? Text.translatable("message.boardcastmod.state_on")
                        : Text.translatable("message.boardcastmod.state_off")
        ));
        return 1;
    }
}
