package com.rate.boardcastmod.export;

import com.rate.boardcastmod.BoardcastMod;
import com.rate.boardcastmod.config.BoardcastConfig;
import com.rate.boardcastmod.util.PathUtil;
import com.rate.boardcastmod.util.ScoreboardAccess;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports the client scoreboard to a single Scoreboard Helper compatible CSV
 * ({@code player,score} rows). Optional extra columns can reference other
 * scoreboard objectives: every objective listed in
 * {@link BoardcastConfig#exportExtraObjectives} becomes one additional column
 * (3rd, 4th, ...) containing that player's score in the referenced objective.
 */
public final class ScoreboardExporter {
    private static final Object WRITE_LOCK = new Object();
    private static final String CSV_SEPARATOR = ",";

    private static int tickCounter;
    private static boolean dirty;
    private static World lastWorld;
    private static String lastSnapshot;
    private static Path lastCsvPath;
    private static boolean lastExportEnabled;

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
            return;
        }
        if (!lastExportEnabled) {
            lastExportEnabled = true;
            lastSnapshot = null;
            dirty = true;
        }

        if (lastWorld != client.world) {
            lastWorld = client.world;
            lastSnapshot = null;
            dirty = true;
        }

        Path csvPath = PathUtil.resolveGamePath(cfg.exportHelperCsvPath).toAbsolutePath().normalize();
        if (!csvPath.equals(lastCsvPath)) {
            lastCsvPath = csvPath;
            lastSnapshot = null;
            dirty = true;
        }
        // A blank path resolves to a directory; there is nothing sensible to write.
        if (csvPath.getFileName() == null) {
            return;
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

        Scoreboard scoreboard = ScoreboardAccess.best(client);
        if (scoreboard == null) {
            return;
        }
        CsvSnapshot snapshot = buildPlayerScore(scoreboard, cfg);
        if (!snapshot.data.equals(lastSnapshot)) {
            if (writeAtomically(csvPath, snapshot.data)) {
                lastSnapshot = snapshot.data;
            } else {
                dirty = true;
            }
        }
    }

    public static void requestExport() {
        dirty = true;
        lastSnapshot = null;
    }

    /**
     * Builds the CSV in Scoreboard Helper's format: a {@code Player,Score}
     * header followed by one {@code name,rawScore} line per sidebar entry. No
     * display styling (hearts, timers, compact numbers) is applied here.
     * <p>
     * Rows always come from the sidebar objective (the {@code Score} column).
     * Every objective listed in {@link BoardcastConfig#exportExtraObjectives}
     * adds one extra column after {@code Score}, filled with that player's raw
     * score in the referenced objective (empty when the player has no entry).
     */
    private static CsvSnapshot buildPlayerScore(Scoreboard scoreboard, BoardcastConfig cfg) {
        StringBuilder sb = new StringBuilder(2048);

        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        List<ScoreboardObjective> extraObjectives = resolveExtraObjectives(scoreboard, cfg);
        // The sidebar is already the "Score" column, so it is never repeated.
        extraObjectives.removeIf(objective -> objective == sidebar);

        if (cfg.exportIncludeHeader) {
            sb.append("Player").append(CSV_SEPARATOR).append("Score");
            for (ScoreboardObjective extra : extraObjectives) {
                sb.append(CSV_SEPARATOR).append(csv(extra.getName()));
            }
            sb.append('\n');
        }

        if (sidebar == null) {
            return new CsvSnapshot(sb.toString());
        }

        // One lookup map per extra objective, keyed exactly like the row keys
        // below so players match regardless of which objective holds the score.
        Map<ScoreboardObjective, Map<String, Integer>> extraScores = new HashMap<>();
        for (ScoreboardObjective extra : extraObjectives) {
            extraScores.put(extra, buildScoreLookup(scoreboard, extra));
        }

        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(sidebar));
        entries.sort(Comparator.comparingInt(ScoreboardEntry::value).reversed()
                .thenComparing(ScoreboardEntry::owner));
        for (ScoreboardEntry entry : sidebarVisibleEntries(entries, cfg)) {
            String rowKey = rowKey(entry);
            sb.append(csv(rowKey)).append(CSV_SEPARATOR).append(entry.value());
            for (ScoreboardObjective extra : extraObjectives) {
                Integer value = extraScores.get(extra).get(rowKey);
                sb.append(CSV_SEPARATOR).append(value == null ? "" : value);
            }
            sb.append('\n');
        }
        return new CsvSnapshot(sb.toString());
    }

    /**
     * Resolves the configured extra-objective names to live
     * {@link ScoreboardObjective} instances, preserving the configured column
     * order and skipping names that are blank, unknown or duplicated.
     */
    private static List<ScoreboardObjective> resolveExtraObjectives(Scoreboard scoreboard, BoardcastConfig cfg) {
        List<ScoreboardObjective> extraObjectives = new ArrayList<>();
        if (cfg.exportExtraObjectives == null) {
            return extraObjectives;
        }
        for (String raw : cfg.exportExtraObjectives) {
            String name = raw == null ? "" : raw.trim();
            if (name.isEmpty()) {
                continue;
            }
            ScoreboardObjective objective = scoreboard.getNullableObjective(name);
            if (objective != null && !extraObjectives.contains(objective)) {
                extraObjectives.add(objective);
            }
        }
        return extraObjectives;
    }

    /** Maps display row keys to the highest raw score present in an objective. */
    private static Map<String, Integer> buildScoreLookup(Scoreboard scoreboard, ScoreboardObjective objective) {
        Map<String, Integer> lookup = new HashMap<>();
        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
            lookup.merge(rowKey(entry), entry.value(), Math::max);
        }
        return lookup;
    }

    /** The same key used for CSV rows: the display name when available, else the raw owner. */
    private static String rowKey(ScoreboardEntry entry) {
        if (entry.name() != null) {
            return entry.name().getString();
        }
        return entry.owner() == null ? "" : entry.owner();
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
