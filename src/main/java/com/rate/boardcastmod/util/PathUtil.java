package com.rate.boardcastmod.util;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class PathUtil {
    private PathUtil() {
    }

    /**
     * Relative paths are resolved against {@code .minecraft} (FabricLoader's
     * game directory), so {@code config/boardcastmod/foo.csv} behaves exactly
     * like users expect it to.
     */
    public static Path resolveGamePath(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            value = "config/boardcastmod/";
        }
        try {
            Path path = Path.of(value);
            if (path.isAbsolute()) {
                return path.normalize();
            }
            return FabricLoader.getInstance().getGameDir().resolve(path).normalize();
        } catch (RuntimeException e) {
            return FabricLoader.getInstance().getGameDir().resolve("config/boardcastmod").normalize();
        }
    }
}
