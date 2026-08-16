package com.rate.boardcastmod.mixin;

import com.rate.boardcastmod.BoardcastMod;
import com.rate.boardcastmod.render.SidebarRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    /**
     * Replaces the vanilla sidebar renderer with the configurable
     * Scoreboard-Overhaul-style renderer. When the OBS overlay compatibility
     * hide is active, this simply cancels vanilla drawing so OBS can show its
     * own overlay without the native sidebar being visible.
     *
     * <p>Targets {@code method_1757} (the intermediary name of
     * {@code renderScoreboardSidebar(DrawContext, ScoreboardObjective)}) with
     * {@code remap = false} because Loom 1.14.10 fails to remap the method
     * descriptor in mixin targets, leaving a half-named refmap that breaks at
     * runtime. This bypasses the broken remapping entirely.
     */
    @Inject(
            method = "method_1757",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void boardcastmod$replaceScoreboardSidebar(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (SidebarRenderer.isSidebarHidden()) {
            ci.cancel();
            return;
        }
        if (SidebarRenderer.shouldUseCustomRenderer()) {
            try {
                SidebarRenderer.render(context, objective);
            } catch (Throwable t) {
                BoardcastMod.LOGGER.warn("Custom sidebar rendering failed: {}", t.toString());
            }
            ci.cancel();
        }
    }
}
