package ru.ludocraft.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import ru.ludocraft.gameplay.LudkaRules;
import ru.ludocraft.menu.LudkaMenu;

/** Entire GUI (including buttons and slot frames) is drawn with fill; no GUI texture assets. */
public final class LudkaScreen extends AbstractContainerScreen<LudkaMenu> {
    private static final int BG = 0xFF16161D, NEON = 0xFFA578FF, GOLD = 0xFFFFD76A;
    private static final int GREEN = 0xFF27BB7C, RED = 0xFFB63853;
    private final Button[] riskButtons = new Button[4];
    private Button upgrade;

    public LudkaScreen(LudkaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 240;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 4; i++) {
            final int risk = i;
            riskButtons[i] = addRenderableWidget(new NeonButton(leftPos + 12 + i * 70, topPos + 66, 66, 19,
                    Component.literal(LudkaRules.chance(i) + "% (x" + LudkaRules.multiplier(i) + ")"),
                    button -> sendButton(risk), () -> menu.risk() == risk));
            riskButtons[i].setTooltip(Tooltip.create(Component.translatable("screen.ludocraft.risk_" + i)));
        }
        upgrade = addRenderableWidget(new NeonButton(leftPos + 104, topPos + 128, 92, 20,
                Component.translatable("screen.ludocraft.upgrade"), button -> {
                    if (!menu.canStart()) return;
                    // Mask immediately, before the server can send the populated output slot.
                    menu.beginClientRequest();
                    sendButton(4);
                }, () -> true));
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (Button button : riskButtons) button.active = !menu.isBusy();
        upgrade.active = menu.canStart();
        if (menu.takeCompletionSound() && minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    menu.won() ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.ITEM_BREAK, 1.0F));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        // Inactive output slots are neither rendered nor hovered by AbstractContainerScreen.
        renderTooltip(graphics, mouseX, mouseY);
        if (!menu.isBusy() && isHovering(24, 38, 16, 16, mouseX, mouseY)
                && !menu.getSlot(LudkaMenu.INPUT).hasItem() && menu.getCarried().isEmpty()) {
            graphics.renderTooltip(font, Component.translatable("screen.ludocraft.input_hint"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        frame(g, x, y, imageWidth, imageHeight, NEON, BG);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 25, 0xFF211C30);
        g.drawCenteredString(font, title, x + 150, y + 8, GOLD);
        g.drawString(font, Component.translatable("screen.ludocraft.input"), x + 16, y + 27, 0xFFADA8BF, false);
        g.drawString(font, Component.translatable("screen.ludocraft.output"), x + 250, y + 27, 0xFFADA8BF, false);
        frame(g, x + 22, y + 36, 20, 20, NEON, 0xFF0E0E14);
        frame(g, x + 258, y + 36, 20, 20, GOLD, 0xFF0E0E14);
        if (menu.isBusy()) g.drawCenteredString(font, "?", x + 268, y + 42, GOLD);
        Component status;
        int color = 0xFFADA8BF;
        if (menu.pending()) status = Component.translatable("screen.ludocraft.waiting");
        else if (menu.isBusy()) status = Component.translatable("screen.ludocraft.spinning");
        else if (menu.hasResult()) {
            status = Component.translatable(menu.won() ? "screen.ludocraft.win" : "screen.ludocraft.loss", menu.roll());
            color = menu.won() ? GREEN : RED;
        } else if (menu.unsupportedInput()) {
            status = Component.translatable("screen.ludocraft.unsupported");
            color = RED;
        } else status = Component.translatable("screen.ludocraft.choose");
        g.drawCenteredString(font, status, x + 150, y + 39, color);
        g.drawCenteredString(font, Component.translatable("screen.ludocraft.one_item"), x + 150, y + 52, 0xFF777286);

        // 280 is divisible by 20: all four green widths are exact integers (140, 70, 28, 14).
        int barX = x + 10, barY = y + 94, barWidth = 280;
        frame(g, barX - 1, barY - 1, barWidth + 2, 18, 0xFF61536F, RED);
        int greenWidth = barWidth * LudkaRules.chance(menu.risk()) / 100;
        g.fill(barX, barY, barX + greenWidth, barY + 16, GREEN);
        g.fill(barX + greenWidth, barY, barX + barWidth, barY + 16, RED);
        for (int step = 1; step < 10; step++) {
            int mark = barX + barWidth * step / 10;
            g.fill(mark, barY + 12, mark + 1, barY + 16, 0x55000000);
        }
        double position = 0;
        if (menu.pending()) {
            double phase = (System.nanoTime() / 8_000_000.0) % 200.0;
            position = phase < 100 ? phase : 200 - phase;
        } else if (menu.hasResult()) {
            position = LudkaRules.runner(menu.clientProgress(), menu.roll());
        }
        int needle = barX + (int) Math.round(barWidth * position / 100.0);
        g.fill(needle - 1, barY - 3, needle + 2, barY + 19, 0xFFFFFFFF);
        g.fill(needle - 3, barY - 4, needle + 4, barY - 2, GOLD);
        g.drawString(font, "0", barX, y + 115, 0xFFADA8BF, false);
        g.drawCenteredString(font, Component.translatable("screen.ludocraft.chance", LudkaRules.chance(menu.risk())),
                x + 150, y + 115, 0xFFADA8BF);
        g.drawString(font, "100", barX + barWidth - font.width("100"), y + 115, 0xFFADA8BF, false);

        g.drawString(font, playerInventoryTitle, x + 68, y + 151, 0xFF777286, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) slotFrame(g, x + 69 + col * 18, y + 162 + row * 18);
        }
        for (int col = 0; col < 9; col++) slotFrame(g, x + 69 + col * 18, y + 218);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Drawn above in absolute coordinates; suppress the vanilla overlapping titles.
    }

    private static void slotFrame(GuiGraphics g, int x, int y) {
        frame(g, x - 1, y - 1, 18, 18, 0xFF353142, 0xFF101016);
    }
    private static void frame(GuiGraphics g, int x, int y, int w, int h, int border, int fill) {
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private static final class NeonButton extends Button {
        private final BooleanSupplier selected;
        NeonButton(int x, int y, int width, int height, Component label, OnPress press, BooleanSupplier selected) {
            super(x, y, width, height, label, press, DEFAULT_NARRATION);
            this.selected = selected;
        }
        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int border = selected.getAsBoolean() ? GOLD : NEON;
            if (!active) border = 0xFF494152;
            int fill = active && isHoveredOrFocused() ? 0xFF393047 : 0xFF211C2C;
            frame(g, getX(), getY(), getWidth(), getHeight(), border, fill);
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, active ? 0xFFF1EBFF : 0xFF777080);
        }
    }
}
