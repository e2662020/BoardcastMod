package com.rate.boardcastmod.util;

import org.slf4j.Logger;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;

/**
 * Works around third-party launchers (PCL2, HMCL, ...) that start the game JVM
 * with {@code -Djava.awt.headless=true}. AWT caches the headless flag in the
 * private static {@code GraphicsEnvironment#headless} field the first time it is
 * read, so simply calling {@code System.setProperty("java.awt.headless",
 * "false")} is not enough once that flag has been cached. This resets both the
 * property and the cached field so the native AWT {@code FileDialog} (the
 * "Browse" button) can open.
 */
public final class AwtHeadless {
    private AwtHeadless() {
    }

    public static void forceNonHeadless(Logger logger) {
        System.setProperty("java.awt.headless", "false");
        boolean reset = resetCachedFlag(logger);

        boolean headless;
        try {
            headless = GraphicsEnvironment.isHeadless();
        } catch (Throwable t) {
            headless = true;
        }
        logger.info("[AWT] headless property='{}', isHeadless()={}, cachedFlagReset={}",
                System.getProperty("java.awt.headless"), headless, reset);
    }

    private static boolean resetCachedFlag(Logger logger) {
        try {
            Field field = GraphicsEnvironment.class.getDeclaredField("headless");
            field.setAccessible(true);
            field.set(null, Boolean.FALSE);
            logger.info("[AWT] reset GraphicsEnvironment.headless via setAccessible");
            return true;
        } catch (Throwable t) {
            logger.warn("[AWT] setAccessible reset failed: {}", t.toString());
        }
        return resetCachedFlagUnsafe(logger);
    }

    @SuppressWarnings("deprecation")
    private static boolean resetCachedFlagUnsafe(Logger logger) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

            Field headless = GraphicsEnvironment.class.getDeclaredField("headless");
            long offset = unsafe.staticFieldOffset(headless);
            Object base = unsafe.staticFieldBase(headless);
            unsafe.putObject(base, offset, Boolean.FALSE);
            logger.info("[AWT] reset GraphicsEnvironment.headless via sun.misc.Unsafe");
            return true;
        } catch (Throwable t) {
            logger.warn("[AWT] Unsafe reset failed: {}", t.toString());
            return false;
        }
    }
}
