package com.rate.boardcastmod.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;

/**
 * Picks the most complete scoreboard a client can read.
 */
public final class ScoreboardAccess {
    private ScoreboardAccess() {
    }

    /**
     * Returns the scoreboard to read for both the config objective picker and
     * the CSV exporter.
     * <p>
     * In singleplayer the integrated server's scoreboard is authoritative and
     * contains every objective — including objectives never shown in a display
     * slot and never assigned a score, which the client's synced copy does not
     * learn about. On a dedicated server only the client's synced scoreboard is
     * available (objectives the server actually sent via display slots or score
     * packets), so that is used as a fallback. Returns {@code null} when the
     * client is not in a world at all.
     */
    public static Scoreboard best(MinecraftClient client) {
        MinecraftServer server = client.getServer();
        if (server != null) {
            Scoreboard scoreboard = server.getScoreboard();
            if (scoreboard != null) {
                return scoreboard;
            }
        }
        if (client.world != null) {
            return client.world.getScoreboard();
        }
        return null;
    }
}
