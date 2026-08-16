package com.rate.boardcastmod.key;

import com.rate.boardcastmod.BoardcastMod;
import com.rate.boardcastmod.config.BoardcastConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public final class KeyBindings {
    private static KeyBinding toggleTimerSeconds;

    private KeyBindings() {
    }

    public static void register() {
        toggleTimerSeconds = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.boardcastmod.toggle_timer_seconds",
                InputUtil.Type.KEYSYM,
                InputUtil.GLFW_KEY_N,
                "category.boardcastmod"
        ));
    }

    public static void tick(MinecraftClient client) {
        if (toggleTimerSeconds == null || client.currentScreen != null || !toggleTimerSeconds.wasPressed()) {
            return;
        }

        BoardcastConfig cfg = BoardcastMod.config();
        cfg.timerUseSeconds = !cfg.timerUseSeconds;
        BoardcastMod.saveConfig();

        if (client.player != null) {
            client.player.sendMessage(Text.translatable(
                    "message.boardcastmod.timer_seconds",
                    cfg.timerUseSeconds
                            ? Text.translatable("message.boardcastmod.state_on")
                            : Text.translatable("message.boardcastmod.state_off")
            ), false);
        }
    }
}
