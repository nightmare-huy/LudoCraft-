package ru.ludocraft.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.ludocraft.gameplay.LudkaCompatibility;
import ru.ludocraft.gameplay.LudkaRules;
import ru.ludocraft.gameplay.LudkaValues;
import ru.ludocraft.menu.LudkaMenu;
import ru.ludocraft.network.SelectLudkaTarget;

/** Two views of one menu: inventory/roulette and a modal, ghost-item reward catalogue. */
public final class LudkaScreen extends AbstractContainerScreen<LudkaMenu> {
    private static final int BG = 0xFF16161D, NEON = 0xFFA578FF, GOLD = 0xFFFFD76A;
    private static final int GREEN = 0xFF27BB7C, RED = 0xFFB63853;
    private static final int[] TIER_COLORS = {0xFFB8BFCC, 0xFF67D99D, 0xFF65B8FF, 0xFFC28AFF, GOLD};
    private static final int PAGE_SIZE = 32;
    private final List<AbstractWidget> catalogueWidgets = new ArrayList<>();
    private List<Entry> entries = List.of(), filtered = List.of();
    private EditBox search;
    private Button choose, upgrade, previous, next;
    private boolean catalogueOpen, suppressRelease;
    private int page, tierFilter = -1;
    private record Entry(ResourceLocation id, ItemStack stack, int value, int tier, String search) {}

    public LudkaScreen(LudkaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 240;
    }
    @Override
    protected void init() {
        String oldSearch = search == null ? "" : search.getValue();
        super.init();
        catalogueWidgets.clear();
        choose = addRenderableWidget(new NeonButton(leftPos + 86, topPos + 66, 128, 17,
                tr("select_target"), button -> openCatalogue(), () -> false));
        upgrade = addRenderableWidget(new NeonButton(leftPos + 104, topPos + 128, 92, 20,
                tr("upgrade"), button -> {
                    if (menu.canStart() && minecraft != null && minecraft.gameMode != null) {
                        menu.beginClientRequest();
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, LudkaMenu.START_BUTTON);
                    }
                }, () -> true));
        search = addCatalogueWidget(new EditBox(font, leftPos + 12, topPos + 31, 276, 18, tr("search")));
        search.setMaxLength(128);
        search.setHint(tr("search"));
        search.setValue(oldSearch);
        search.setResponder(value -> { page = 0; filter(); });
        addCatalogueWidget(new NeonButton(leftPos + 272, topPos + 5, 18, 18, Component.literal("×"),
                button -> closeCatalogue(), () -> false));
        for (int i = -1; i < 5; i++) {
            int filter = i;
            addCatalogueWidget(new NeonButton(leftPos + 10 + (i + 1) * 47, topPos + 55, 45, 18,
                    tr(i < 0 ? "all" : "tier_short_" + i), button -> {
                        tierFilter = filter;
                        page = 0;
                        filter();
                    }, () -> tierFilter == filter));
        }
        previous = addCatalogueWidget(new NeonButton(leftPos + 10, topPos + 213, 38, 18,
                Component.literal("<"), button -> { if (page > 0) page--; }, () -> false));
        next = addCatalogueWidget(new NeonButton(leftPos + 252, topPos + 213, 38, 18,
                Component.literal(">"), button -> { if ((page + 1) * PAGE_SIZE < filtered.size()) page++; }, () -> false));
        rebuildCatalogue();
        setView();
    }
    private <W extends AbstractWidget> W addCatalogueWidget(W widget) {
        addRenderableWidget(widget);
        catalogueWidgets.add(widget);
        return widget;
    }
    private void rebuildCatalogue() {
        if (minecraft == null || minecraft.level == null) return;
        List<Entry> all = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance().copyWithCount(1);
            if (!LudkaCompatibility.accepts(stack, minecraft.level)) continue;
            var id = BuiltInRegistries.ITEM.getKey(item);
            int value = LudkaValues.base(stack);
            all.add(new Entry(id, stack, value, LudkaRules.rarity(value),
                    (stack.getHoverName().getString() + " " + id).toLowerCase(Locale.ROOT)));
        }
        all.sort(Comparator.comparingInt(Entry::value).reversed().thenComparing(entry -> entry.id().toString()));
        entries = all;
        filter();
    }
    private void filter() {
        String query = search == null ? "" : search.getValue().strip().toLowerCase(Locale.ROOT);
        filtered = entries.stream().filter(entry -> (tierFilter < 0 || entry.tier() == tierFilter)
                && entry.search().contains(query)).toList();
        page = Math.clamp(page, 0, Math.max(0, (filtered.size() - 1) / PAGE_SIZE));
    }
    private void openCatalogue() {
        if (menu.isBusy() || menu.selecting() || !menu.getCarried().isEmpty()) return;
        catalogueOpen = true;
        rebuildCatalogue();
        setView();
        setFocused(search);
        search.setFocused(true);
    }
    private void closeCatalogue() {
        catalogueOpen = false;
        search.setFocused(false);
        setFocused(null);
        setView();
    }
    private void setView() {
        choose.visible = upgrade.visible = !catalogueOpen;
        for (var widget : catalogueWidgets) widget.visible = catalogueOpen;
    }
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        choose.active = !menu.isBusy() && !menu.selecting() && menu.getCarried().isEmpty();
        upgrade.active = menu.canStart();
        if (menu.takeCompletionSound() && minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    menu.won() ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.ITEM_BREAK, 1F));
        }
        if (catalogueOpen) {
            hoveredSlot = null;
            renderTransparentBackground(g);
            drawCatalogue(g, mouseX, mouseY, partialTick);
        } else {
            super.render(g, mouseX, mouseY, partialTick);
            renderTooltip(g, mouseX, mouseY);
            ItemStack target = menu.target();
            if (!target.isEmpty() && isHovering(142, 38, 16, 16, mouseX, mouseY)) {
                g.renderTooltip(font, List.of(target.getHoverName(),
                        tr("value", menu.targetValue()), tr("tier_" + LudkaRules.rarity(menu.targetValue()))),
                        Optional.empty(), mouseX, mouseY);
            }
        }
    }
    private Entry entryAt(double mouseX, double mouseY) {
        int x = (int) Math.floor(mouseX - leftPos - 10), y = (int) Math.floor(mouseY - topPos - 80);
        if (x < 0 || x >= 280 || y < 0 || y >= 120) return null;
        int index = page * PAGE_SIZE + (y / 30) * 8 + x / 35;
        return index < filtered.size() ? filtered.get(index) : null;
    }
    private void drawCatalogue(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos, y = topPos;
        frame(g, x, y, 300, 240, NEON, BG);
        g.drawCenteredString(font, tr("catalogue"), x + 140, y + 10, GOLD);
        ItemStack selected = menu.target();
        Entry hovered = entryAt(mouseX, mouseY);
        for (int cell = 0; cell < PAGE_SIZE; cell++) {
            int index = page * PAGE_SIZE + cell;
            if (index >= filtered.size()) break;
            Entry entry = filtered.get(index);
            int bx = x + 10 + (cell % 8) * 35, by = y + 80 + (cell / 8) * 30;
            int border = selected.is(entry.stack().getItem()) ? GOLD : TIER_COLORS[entry.tier()];
            frame(g, bx, by, 33, 28, border, entry == hovered ? 0xFF3C304B : 0xFF211C2C);
            g.renderItem(entry.stack(), bx + 8, by + 2);
            g.drawCenteredString(font, Integer.toString(entry.value()), bx + 16, by + 19, TIER_COLORS[entry.tier()]);
        }
        if (filtered.isEmpty()) g.drawCenteredString(font, tr("no_results"), x + 150, y + 130, 0xFFADA8BF);
        previous.active = page > 0;
        next.active = (page + 1) * PAGE_SIZE < filtered.size();
        g.drawCenteredString(font, tr("page", page + 1, Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE)),
                x + 150, y + 217, 0xFFADA8BF);
        for (var widget : catalogueWidgets) widget.render(g, mouseX, mouseY, partialTick);
        if (hovered != null) {
            g.renderTooltip(font, List.of(hovered.stack().getHoverName(), Component.literal(hovered.id().toString()),
                    tr("tier_" + hovered.tier()), tr("value", hovered.value()),
                    tr("chance", LudkaRules.percent(LudkaRules.chance(menu.stakeValue(), hovered.value())))),
                    Optional.empty(), mouseX, mouseY);
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!catalogueOpen) return super.mouseClicked(mouseX, mouseY, button);
        suppressRelease = true;
        for (var widget : catalogueWidgets) {
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                if (widget == search) { setFocused(search); search.setFocused(true); }
                return true;
            }
        }
        Entry entry = entryAt(mouseX, mouseY);
        if (button == 0 && entry != null && !menu.isBusy() && !menu.selecting()) {
            menu.beginSelectionRequest();
            PacketDistributor.sendToServer(new SelectLudkaTarget(menu.containerId, entry.id()));
            closeCatalogue();
        }
        return true; // Never forward ghost-item clicks to real slots behind the modal.
    }
    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (catalogueOpen || suppressRelease) {
            suppressRelease = false;
            for (var widget : catalogueWidgets) widget.mouseReleased(x, y, button);
            return true;
        }
        return super.mouseReleased(x, y, button);
    }
    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return catalogueOpen ? search.mouseDragged(x, y, button, dx, dy) : super.mouseDragged(x, y, button, dx, dy);
    }
    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (!catalogueOpen) return super.mouseScrolled(x, y, horizontal, vertical);
        page = Math.clamp(page + (vertical < 0 ? 1 : vertical > 0 ? -1 : 0), 0,
                Math.max(0, (filtered.size() - 1) / PAGE_SIZE));
        return true;
    }
    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (!catalogueOpen) return super.keyPressed(key, scan, modifiers);
        if (key == 256) closeCatalogue(); // Escape closes only the catalogue, not the container.
        else search.keyPressed(key, scan, modifiers);
        return true;
    }
    @Override
    public boolean charTyped(char character, int modifiers) {
        return catalogueOpen ? search.charTyped(character, modifiers) : super.charTyped(character, modifiers);
    }
    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        frame(g, x, y, imageWidth, imageHeight, NEON, BG);
        g.fill(x + 1, y + 1, x + 299, y + 25, 0xFF211C30);
        g.drawCenteredString(font, title, x + 150, y + 8, GOLD);
        g.drawString(font, tr("input"), x + 16, y + 27, 0xFFADA8BF, false);
        g.drawString(font, tr("output"), x + 250, y + 27, 0xFFADA8BF, false);
        frame(g, x + 22, y + 36, 20, 20, NEON, 0xFF0E0E14);
        frame(g, x + 258, y + 36, 20, 20, GOLD, 0xFF0E0E14);
        ItemStack target = menu.target();
        if (!target.isEmpty()) g.renderItem(target, x + 142, y + 38); // Ghost, NOT an inventory slot.
        if (menu.isBusy()) g.drawCenteredString(font, "?", x + 268, y + 42, GOLD);
        Component status;
        int color = 0xFFADA8BF;
        if (menu.pending() || menu.selecting()) status = tr("waiting");
        else if (menu.isBusy()) status = tr("spinning");
        else if (menu.unsupportedInput()) { status = tr("unsupported"); color = RED; }
        else if (menu.hasResult()) {
            status = tr(menu.won() ? "win" : "loss", LudkaRules.percent(menu.roll()));
            color = menu.won() ? GREEN : RED;
        } else status = tr(target.isEmpty() ? "choose" : "tier_" + LudkaRules.rarity(menu.targetValue()));
        g.drawCenteredString(font, status, x + 150, y + 27, color);
        String name = target.isEmpty() ? tr("select_target").getString() : target.getHoverName().getString();
        g.drawCenteredString(font, font.plainSubstrByWidth(name, 180), x + 150, y + 55, GOLD);
        g.drawString(font, tr("stake_value", menu.stakeValue()), x + 10, y + 84, 0xFFADA8BF, false);
        Component cost = tr("target_value", menu.targetValue());
        g.drawString(font, cost, x + 290 - font.width(cost), y + 84, 0xFFADA8BF, false);
        int barX = x + 10, barY = y + 96;
        frame(g, barX - 1, barY - 1, 282, 16, 0xFF61536F, RED);
        // Fractional-pixel geometry: do NOT inflate a tiny chance to a whole green pixel.
        g.pose().pushPose();
        g.pose().translate(barX, barY, 0);
        g.pose().scale(280F / LudkaRules.PRECISION, 1, 1);
        g.fill(0, 0, Math.clamp(menu.displayedChance(), 0, LudkaRules.PRECISION), 14, GREEN);
        g.pose().popPose();
        double position = 0;
        if (menu.pending()) {
            double phase = (System.nanoTime() / 8_000_000.0) % 200.0;
            position = phase < 100 ? phase : 200 - phase;
        } else if (menu.hasResult()) {
            position = LudkaRules.runner(menu.clientProgress(), menu.roll() * 100.0 / LudkaRules.PRECISION);
        }
        int needle = barX + (int) Math.round(280 * position / 100.0);
        g.fill(needle - 1, barY - 3, needle + 2, barY + 17, 0xFFFFFFFF);
        g.drawString(font, "0", barX, y + 115, 0xFFADA8BF, false);
        g.drawCenteredString(font, tr("chance", LudkaRules.percent(menu.displayedChance())), x + 150, y + 115, GOLD);
        g.drawString(font, "100", x + 272, y + 115, 0xFFADA8BF, false);
        g.drawString(font, playerInventoryTitle, x + 68, y + 151, 0xFF777286, false);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) slotFrame(g, x + 69 + col * 18, y + 162 + row * 18);
        for (int col = 0; col < 9; col++) slotFrame(g, x + 69 + col * 18, y + 218);
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {}
    private static Component tr(String key, Object... args) { return Component.translatable("screen.ludocraft." + key, args); }
    private static void slotFrame(GuiGraphics g, int x, int y) { frame(g, x - 1, y - 1, 18, 18, 0xFF353142, 0xFF101016); }
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
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int border = active ? (selected.getAsBoolean() ? GOLD : NEON) : 0xFF494152;
            frame(g, getX(), getY(), getWidth(), getHeight(), border, active && isHoveredOrFocused() ? 0xFF393047 : 0xFF211C2C);
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, active ? 0xFFF1EBFF : 0xFF777080);
        }
    }
}
