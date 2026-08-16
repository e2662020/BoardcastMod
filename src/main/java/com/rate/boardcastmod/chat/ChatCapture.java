package com.rate.boardcastmod.chat;

import com.rate.boardcastmod.BoardcastMod;
import com.rate.boardcastmod.config.BoardcastConfig;
import com.rate.boardcastmod.util.PathUtil;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ChatCapture {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Object WRITE_LOCK = new Object();

    private ChatCapture() {
    }

    public static void register() {
        // In the 1.21 message API, ALLOW_GAME carries every server-sent message;
        // `overlay` distinguishes the action bar ("GAME") from chat-line ("SYSTEM") messages.
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            BoardcastConfig cfg = BoardcastMod.config();
            if (overlay) {
                if (cfg.chatCaptureEnabled && cfg.chatCaptureGameMessages) {
                    capture(message, "GAME");
                }
            } else if (cfg.chatCaptureEnabled && cfg.chatCaptureSystemMessages) {
                capture(message, "SYSTEM");
            }
            return true;
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            BoardcastConfig cfg = BoardcastMod.config();
            if (cfg.chatCaptureEnabled && cfg.chatCaptureChatMessages) {
                capture(message, "CHAT");
            }
            return true;
        });

        ClientSendMessageEvents.CHAT.register(message -> {
            BoardcastConfig cfg = BoardcastMod.config();
            if (cfg.chatCaptureEnabled && cfg.chatCaptureOutgoingMessages) {
                captureOutgoing(message);
            }
        });
    }

    private static void capture(Text message, String kind) {
        try {
            BoardcastConfig cfg = BoardcastMod.config();
            if (matches(message.getString(), cfg)) {
                String prefix = cfg.chatIncludeKindPrefix ? "[" + kind + "] " : "";
                writeLine(prefix + message.getString(), cfg);
            }
        } catch (Throwable t) {
            BoardcastMod.LOGGER.warn("Chat capture failed: {}", t.toString());
        }
    }

    private static void captureOutgoing(String message) {
        try {
            BoardcastConfig cfg = BoardcastMod.config();
            if (matches(message, cfg)) {
                String prefix = cfg.chatIncludeKindPrefix ? "[OUT] " : "";
                writeLine(prefix + message, cfg);
            }
        } catch (Throwable t) {
            BoardcastMod.LOGGER.warn("Outgoing chat capture failed: {}", t.toString());
        }
    }

    private static boolean matches(String plain, BoardcastConfig cfg) {
        if (plain == null || cfg.chatPatterns == null || cfg.chatPatterns.isEmpty()) {
            return false;
        }
        int flags = 0;
        if (cfg.chatRegexCaseInsensitive) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        for (String raw : cfg.chatPatterns) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                Pattern pattern = Pattern.compile(raw, flags);
                if (pattern.matcher(plain).find()) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                BoardcastMod.LOGGER.warn("Invalid chat capture regex '{}': {}", raw, e.getDescription());
            }
        }
        return false;
    }

    private static void writeLine(String message, BoardcastConfig cfg) {
        String timestamp = cfg.chatIncludeTimestamp
                ? LocalDateTime.now().format(TIMESTAMP_FORMAT) + "  "
                : "";
        String line = timestamp + message + System.lineSeparator();

        synchronized (WRITE_LOCK) {
            try {
                Path target = PathUtil.resolveGamePath(cfg.chatCapturePath);
                Path parent = target.toAbsolutePath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                rotateIfNeeded(target, cfg);
                Files.writeString(target, line, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                BoardcastMod.LOGGER.warn("Failed to write chat capture file: {}", e.toString());
            }
        }
    }

    private static void rotateIfNeeded(Path target, BoardcastConfig cfg) throws IOException {
        if (cfg.chatMaxFileSizeKb <= 0 || !Files.exists(target)) {
            return;
        }
        long maxBytes = cfg.chatMaxFileSizeKb * 1024L;
        if (Files.size(target) >= maxBytes) {
            Path backup = target.resolveSibling(target.getFileName() + ".1.bak");
            Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
