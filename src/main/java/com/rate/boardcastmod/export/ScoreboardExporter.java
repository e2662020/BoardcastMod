package com.rate.boardcastmod.export;

import com.rate.boardcastmod.BoardcastMod;
import com.rate.boardcastmod.config.BoardcastConfig;
import com.rate.boardcastmod.util.PathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ScoreboardExporter {
    private static final Object WRITE_LOCK = new Object();
    private static final String CSV_SEPARATOR = ",";

    private static int tickCounter;
    private static boolean dirty;
    private static World lastWorld;
    private static String lastSnapshot;
    private static String lastHelperSnapshot;
    private static Path lastCsvPath;
    private static Path lastHelperCsvPath;
    private static boolean lastExportEnabled;
    private static boolean lastHelperEnabled;

    private ScoreboardExporter() {
    }

    public static void tick(MinecraftClient client) {
        try {
            tickInternal(client);
        } catch (Throwable t) {
            BoardcastMod.LOGGER.warn("Scoreboard CSV export tick failed: {}", t.toString());
        }
    }

    private static void tickInternal(MinecraftClient client) {
        BoardcastConfig cfg = BoardcastMod.config();
        if (!cfg.exportEnabled || client.world == null) {
            lastExportEnabled = false;
            lastHelperEnabled = false;
            return;
        }
        if (!lastExportEnabled) {
            lastExportEnabled = true;
            lastSnapshot = null;
            lastHelperSnapshot = null;
            dirty = true;
        }

        if (lastWorld != client.world) {
            lastWorld = client.world;
            lastSnapshot = null;
            lastHelperSnapshot = null;
            dirty = true;
        }

        Path csvPath = PathUtil.resolveGamePath(cfg.exportCsvPath).toAbsolutePath().normalize();
        if (!csvPath.equals(lastCsvPath)) {
            lastCsvPath = csvPath;
            lastSnapshot = null;
            dirty = true;
        }

        Path helperPath = null;
        if (cfg.exportHelperCsv) {
            if (!lastHelperEnabled) {
                lastHelperEnabled = true;
                lastHelperSnapshot = null;
                dirty = true;
            }
            helperPath = PathUtil.resolveGamePath(cfg.exportHelperCsvPath).toAbsolutePath().normalize();
            if (!helperPath.equals(lastHelperCsvPath)) {
                lastHelperCsvPath = helperPath;
                lastHelperSnapshot = null;
                dirty = true;
            }
        } else {
            lastHelperEnabled = false;
        }

        tickCounter++;
        boolean intervalHit = cfg.exportIntervalTicks <= 1 || tickCounter % Math.max(1, cfg.exportIntervalTicks) == 0;
        if (!intervalHit && !dirty) {
            return;
        }
        if (tickCounter >= 1200) {
            tickCounter = 0;
        }
        dirty = false;

        Scoreboard scoreboard = client.world.getScoreboard();
        CsvSnapshot snapshot = buildPlayerScore(scoreboard, cfg);
        if (!snapshot.data.equals(lastSnapshot)) {
            if (writeAtomically(csvPath, snapshot.data)) {
                lastSnapshot = snapshot.data;
            } else {
                dirty = true;
            }
        }

        if (cfg.exportHelperCsv && helperPath != null && !helperPath.equals(csvPath)) {
            if (!snapshot.data.equals(lastHelperSnapshot)) {
                if (writeAtomically(helperPath, snapshot.data)) {
                    lastHelperSnapshot = snapshot.data;
                } else {
                    dirty = true;
                }
            }
        }
    }

    public static void requestExport() {
        dirty = true;
        lastSnapshot = null;
        lastHelperSnapshot = null;
    }

    /**
     * Builds the CSV in Scoreboard Helper's format: a {@code player,score}
     * header followed by one {@code name,rawScore} line per entry. No display
     * styling (hearts, timers, compact numbers) is applied here.
     */
    private static CsvSnapshot buildPlayerScore(Scoreboard scoreboard, BoardcastConfig cfg) {
        StringBuilder sb = new StringBuilder(2048);
        if (cfg.exportIncludeHeader) {
            sb.append("Player").append(CSV_SEPARATOR).append("Score").append('\n');
        }

        List<ScoreboardObjective> objectives = new ArrayList<>(scoreboard.getObjectives());
        objectives.sort(Comparator.comparing(ScoreboardObjective::getName));

        if (!cfg.exportAllObjectives || cfg.exportOnlySidebarEntries) {
            ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            objectives.removeIf(objective -> objective != sidebar);
        }

        for (ScoreboardObjective objective : objectives) {
            List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(objective));
            entries.sort(Comparator.comparingInt(ScoreboardEntry::value).reversed()
                    .thenComparing(ScoreboardEntry::owner));
            for (ScoreboardEntry entry : sidebarVisibleEntries(entries, cfg)) {
                sb.append(csv(entry.name() != null ? entry.name().getString() : entry.owner()))
                        .append(CSV_SEPARATOR)
                        .append(entry.value())
                        .append('\n');
            }
        }
        return new CsvSnapshot(sb.toString());
    }

    /**
     * Narrows an already-sorted entry list down to exactly what the sidebar
     * would render: hidden internal rows are dropped and the list is capped at
     * the configured {@code maxRows}. The list is returned unchanged when
     * {@code exportOnlySidebarEntries} is disabled.
     */
    private static List<ScoreboardEntry> sidebarVisibleEntries(List<ScoreboardEntry> sorted, BoardcastConfig cfg) {
        if (!cfg.exportOnlySidebarEntries) {
            return sorted;
        }
        List<ScoreboardEntry> visible = new ArrayList<>(sorted);
        visible.removeIf(entry -> entry.owner() != null && entry.owner().startsWith("#"));
        int maxRows = Math.min(30, Math.max(0, cfg.maxRows));
        if (visible.size() > maxRows) {
            return new ArrayList<>(visible.subList(0, maxRows));
        }
        return visible;
    }

    private static boolean writeAtomically(Path target, String content) {
        synchronized (WRITE_LOCK) {
            Path temp = null;
            try {
                Path parent = target.toAbsolutePath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                temp = Files.createTempFile(parent, "boardcastmod-csv-", ".tmp");
                Files.writeString(temp, content, StandardCharsets.UTF_8);
                try {
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException atomicFailure) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                temp = null; // moved successfully, nothing left to clean up
                return true;
            } catch (IOException e) {
                BoardcastMod.LOGGER.warn("Failed to write scoreboard CSV: {}", e.toString());
                return false;
            } finally {
                if (temp != null) {
                    try {
                        Files.deleteIfExists(temp);
                    } catch (IOException ignored) {
                        // best effort; do not mask the original failure
                    }
                }
            }
        }
    }

    private static String csv(String value) {
        String s = value == null ? "" : value;
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    private record CsvSnapshot(String data) {
    }
}
