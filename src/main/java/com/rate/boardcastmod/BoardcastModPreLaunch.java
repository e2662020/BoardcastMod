package com.rate.boardcastmod;

import com.rate.boardcastmod.util.AwtHeadless;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs as early as possible in the Fabric lifecycle.
 *
 * <p>Some launchers (PCL2, HMCL, ...) start the JVM with
 * {@code -Djava.awt.headless=true}, which makes {@link java.awt.FileDialog} (the
 * native "Browse" file picker used by the config GUI) throw a
 * {@code HeadlessException}. Re-enable AWT here, before anything else touches it.</p>
 */
public final class BoardcastModPreLaunch implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger("BoardcastMod-PreLaunch");

    @Override
    public void onPreLaunch() {
        AwtHeadless.forceNonHeadless(LOGGER);
    }
}
