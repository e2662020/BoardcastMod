package com.rate.boardcastmod.config;

import com.rate.boardcastmod.BoardcastMod;
import com.rate.boardcastmod.util.AwtHeadless;
import com.rate.boardcastmod.util.PathUtil;
import me.shedaniel.clothconfig2.api.Tooltip;
import me.shedaniel.clothconfig2.gui.entries.TextFieldListEntry;
import me.shedaniel.math.Point;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * A Cloth Config text field entry for file paths, extended with a "Browse"
 * button that opens the OS-native file picker and writes the selected path back
 * into the field (relative to the game directory when possible).
 */
public final class FilePathEntry extends TextFieldListEntry<String> {

    private final ButtonWidget browseButton;

    @SuppressWarnings("deprecation")
    public FilePathEntry(Text fieldName, String value, String defaultValue, Consumer<String> saveConsumer) {
        super(fieldName, value, Text.translatable("text.cloth-config.reset_value"), () -> defaultValue);

        Text browseLabel = Text.translatable("text.autoconfig.boardcastmod.browse");
        int browseWidth = Math.max(30, MinecraftClient.getInstance().textRenderer.getWidth(browseLabel) + 12);
        this.browseButton = ButtonWidget.builder(browseLabel, button -> openFileChooser())
                .dimensions(0, 0, browseWidth, 20)
                .build();

        // children()/narratables() already return the "widgets" list, so adding
        // the button here makes it clickable and focusable automatically.
        this.widgets.add(this.browseButton);

        // StringListEntry sets the protected saveCallback directly; do the same.
        this.saveCallback = saveConsumer;
    }

    @Override
    public String getValue() {
        return this.textFieldWidget.getText();
    }

    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean isHovered, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean rtl = client.textRenderer.isRightToLeft();
        Text label = getDisplayedFieldName();

        // Field-name label (mirrors TextFieldListEntry, LTR + RTL).
        if (rtl) {
            context.drawTextWithShadow(client.textRenderer, label.asOrderedText(),
                    client.getWindow().getScaledWidth() - x - client.textRenderer.getWidth(label),
                    y + 6, getPreferredTextColor());
        } else {
            context.drawTextWithShadow(client.textRenderer, label.asOrderedText(), x, y + 6, getPreferredTextColor());
        }

        this.resetButton.active = isEditable() && getDefaultValue().isPresent()
                && !getDefaultValue().get().equals(this.textFieldWidget.getText());
        this.resetButton.setY(y);
        this.browseButton.setY(y);
        this.textFieldWidget.setY(y + 1);
        this.textFieldWidget.setEditable(isEditable());
        textFieldPreRender(this.textFieldWidget);

        int resetWidth = this.resetButton.getWidth();
        int browseWidth = this.browseButton.getWidth();

        if (rtl) {
            this.resetButton.setX(x);
            this.browseButton.setX(x + resetWidth + 2);
            this.textFieldWidget.setX(x + resetWidth + browseWidth + 4);
            this.textFieldWidget.setWidth(Math.max(16, entryWidth - resetWidth - browseWidth - 8));
        } else {
            this.resetButton.setX(x + entryWidth - resetWidth);
            this.browseButton.setX(x + entryWidth - resetWidth - browseWidth - 2);
            this.textFieldWidget.setX(x + entryWidth - 148);
            this.textFieldWidget.setWidth(Math.max(16, this.browseButton.getX() - 4 - this.textFieldWidget.getX()));
        }

        this.resetButton.render(context, mouseX, mouseY, delta);
        this.browseButton.render(context, mouseX, mouseY, delta);
        this.textFieldWidget.render(context, mouseX, mouseY, delta);

        // Tooltip (mirrors TooltipListEntry).
        if (isMouseInside(mouseX, mouseY, x, y, entryWidth, entryHeight)) {
            getTooltip(mouseX, mouseY).ifPresent(texts ->
                    getConfigScreen().addTooltip(Tooltip.of(new Point(mouseX, mouseY), wrapLinesToScreen(texts))));
        }
    }

    private void openFileChooser() {
        MinecraftClient client = MinecraftClient.getInstance();
        Path resolved = PathUtil.resolveGamePath(this.textFieldWidget.getText());
        Path startDir = resolved.getParent();
        String startFile = resolved.getFileName() == null ? "" : resolved.getFileName().toString();
        String dialogTitle = Text.translatable("text.autoconfig.boardcastmod.browse").getString();

        new Thread(() -> {
            FileDialog dialog = null;
            try {
                AwtHeadless.forceNonHeadless(BoardcastMod.LOGGER);
                dialog = new FileDialog((Frame) null, dialogTitle, FileDialog.SAVE);
                if (startDir != null && Files.isDirectory(startDir)) {
                    dialog.setDirectory(startDir.toString());
                }
                dialog.setFile(startFile);
                dialog.setVisible(true);

                String chosenDir = dialog.getDirectory();
                String chosenFile = dialog.getFile();
                if (chosenDir != null && chosenFile != null) {
                    Path chosen = Path.of(new File(chosenDir, chosenFile).getAbsolutePath())
                            .toAbsolutePath().normalize();
                    String value = relativizeToGameDir(chosen);
                    client.execute(() -> {
                        this.textFieldWidget.setText(value);
                        this.textFieldWidget.setCursorToEnd(false);
                    });
                }
            } catch (Throwable t) {
                BoardcastMod.LOGGER.warn("Failed to open the file chooser", t);
            } finally {
                if (dialog != null) {
                    dialog.dispose();
                }
            }
        }, "BoardcastMod-FileChooser").start();
    }

    /** Stores game-dir-relative paths so the config file stays portable. */
    private static String relativizeToGameDir(Path absolute) {
        Path gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        if (absolute.startsWith(gameDir)) {
            return gameDir.relativize(absolute).toString().replace('\\', '/');
        }
        return absolute.toString();
    }
}
