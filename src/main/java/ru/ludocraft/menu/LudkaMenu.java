package ru.ludocraft.menu;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import ru.ludocraft.LudoCraft;
import ru.ludocraft.gameplay.LudkaCompatibility;
import ru.ludocraft.gameplay.LudkaRules;
import ru.ludocraft.gameplay.LudkaValues;

public final class LudkaMenu extends AbstractContainerMenu {
    public static final int INPUT = 0, OUTPUT = 1, START_BUTTON = 4;
    private final Player owner;
    private final SimpleContainer storage = new SimpleContainer(2);
    private int targetId = -1, chance, stakeValue, targetValue, roll = -1, roundChance;
    private int ticks, round, nextRound, startAck, selectionAck;
    private long unlockTick;
    private boolean serverClosed;
    private ItemStack lastInput = ItemStack.EMPTY;
    private boolean clientPending, clientSelecting;
    private long clientStart;
    private int clientRound, soundedRound;

    public LudkaMenu(int id, Inventory inventory) {
        super(LudoCraft.LUDKA_MENU.get(), id);
        owner = inventory.player;
        addSlot(new Slot(storage, INPUT, 24, 38) {
            @Override public boolean mayPlace(ItemStack stack) {
                return !isBusy() && !stack.isEmpty() && !stack.is(LudoCraft.POCKET_LUDKA.get());
            }
            @Override public boolean mayPickup(Player player) { return !isBusy(); }
        });
        addSlot(new Slot(storage, OUTPUT, 260, 38) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return !isBusy(); }
            @Override public boolean isActive() { return !isBusy(); }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 69 + column * 18, 162 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 69 + column * 18, 218));
        // DataSlot uses 16-bit transport. Explicit halves preserve large registries and tiny odds.
        addIntData(() -> targetId, value -> targetId = value);
        addData(() -> stakeValue, value -> stakeValue = value);
        addData(() -> targetValue, value -> targetValue = value);
        addIntData(() -> chance, value -> chance = value);
        addIntData(() -> roll, value -> roll = value);
        addIntData(() -> roundChance, value -> roundChance = value);
        addData(() -> ticks, value -> ticks = value);
        // The round token arrives AFTER the result and frozen chance.
        addData(() -> round, value -> {
            round = value;
            if (owner.level().isClientSide && value != 0 && value != clientRound) {
                clientRound = value;
                clientStart = System.nanoTime();
                clientPending = false;
            }
        });
        addData(() -> startAck, value -> { startAck = value; clientPending = false; });
        addData(() -> selectionAck, value -> { selectionAck = value; clientSelecting = false; });
    }
    private void addData(IntSupplier getter, IntConsumer setter) {
        addDataSlot(new DataSlot() {
            @Override public int get() { return getter.getAsInt(); }
            @Override public void set(int value) { setter.accept(value); }
        });
    }
    private void addIntData(IntSupplier getter, IntConsumer setter) {
        addData(() -> getter.getAsInt() & 0xffff,
                value -> setter.accept((getter.getAsInt() & 0xffff0000) | (value & 0xffff)));
        addData(() -> (getter.getAsInt() >>> 16) & 0xffff,
                value -> setter.accept((getter.getAsInt() & 0xffff) | ((value & 0xffff) << 16)));
    }
    public ItemStack target() {
        if (targetId < 0) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.byId(targetId);
        return item == null ? ItemStack.EMPTY : item.getDefaultInstance().copyWithCount(1);
    }
    public int chance() { return chance; }
    public int displayedChance() { return hasResult() ? roundChance : chance; }
    public int stakeValue() { return stakeValue; }
    public int targetValue() { return targetValue; }
    public int roll() { return roll; }
    public int round() { return round; }
    public boolean hasResult() { return round != 0; }
    public boolean won() { return hasResult() && LudkaRules.wins(roundChance, roll); }
    public boolean pending() { return clientPending; }
    public boolean selecting() { return clientSelecting; }
    public boolean isBusy() {
        if (!owner.level().isClientSide) return owner.level().getGameTime() < unlockTick;
        return clientPending || ticks > 0 || (clientRound != 0 && clientProgress() < 1.0);
    }
    public double clientProgress() {
        return Math.clamp((System.nanoTime() - clientStart) / (double) LudkaRules.SPIN_NANOS, 0.0, 1.0);
    }
    public boolean unsupportedInput() {
        return !storage.getItem(INPUT).isEmpty() && !LudkaCompatibility.accepts(storage.getItem(INPUT), owner.level());
    }
    public boolean canStart() {
        return !serverClosed && !isBusy() && !clientSelecting && chance > 0
                && (!owner.level().isClientSide || (stakeValue == LudkaValues.stake(storage.getItem(INPUT))
                    && chance == LudkaRules.chance(stakeValue, targetValue)))
                && LudkaCompatibility.accepts(storage.getItem(INPUT), owner.level())
                && LudkaCompatibility.accepts(target(), owner.level()) && storage.getItem(OUTPUT).isEmpty();
    }
    public void beginClientRequest() { if (owner.level().isClientSide) clientPending = true; }
    public void beginSelectionRequest() { if (owner.level().isClientSide) clientSelecting = true; }
    public boolean takeCompletionSound() {
        if (hasResult() && !isBusy() && soundedRound != round) {
            soundedRound = round;
            return true;
        }
        return false;
    }
    private boolean owns(Player player) {
        return player == owner && player.containerMenu == this && !serverClosed && !player.isSpectator()
                && player.level() instanceof ServerLevel;
    }
    public boolean selectTarget(Player player, ResourceLocation id) {
        if (!owns(player)) return false;
        selectionAck = (selectionAck + 1) & 32767;
        boolean valid = !isBusy() && BuiltInRegistries.ITEM.containsKey(id);
        if (valid) valid = LudkaCompatibility.accepts(BuiltInRegistries.ITEM.get(id).getDefaultInstance(), player.level());
        if (valid) {
            targetId = BuiltInRegistries.ITEM.getId(BuiltInRegistries.ITEM.get(id));
            roll = -1;
            round = 0;
        }
        broadcastChanges(); // Acknowledge even invalid/blacklisted/stale requests.
        return valid;
    }
    private void refreshQuote() {
        if (owner.level().isClientSide || isBusy()) return;
        ItemStack input = storage.getItem(INPUT);
        if (!ItemStack.matches(input, lastInput)) {
            round = 0;
            roll = -1;
            lastInput = input.copy();
        }
        stakeValue = LudkaCompatibility.accepts(input, owner.level()) ? LudkaValues.stake(input) : 0;
        ItemStack prize = target();
        targetValue = LudkaCompatibility.accepts(prize, owner.level()) ? LudkaValues.base(prize) : 0;
        chance = LudkaRules.chance(stakeValue, targetValue);
    }
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!owns(player) || button != START_BUTTON) return false;
        startAck = (startAck + 1) & 32767;
        refreshQuote(); // Never trust a stale client quote or stack count.
        if (!canStart() || player.getCooldowns().isOnCooldown(LudoCraft.POCKET_LUDKA.get())) {
            broadcastChanges();
            return false;
        }
        var level = (ServerLevel) player.level();
        int result = level.random.nextInt(LudkaRules.PRECISION);
        ItemStack prize = LudkaRules.wins(chance, result) ? target() : ItemStack.EMPTY;
        unlockTick = level.getGameTime() + LudkaRules.SPIN_TICKS;
        player.getCooldowns().addCooldown(LudoCraft.POCKET_LUDKA.get(), LudkaRules.SPIN_TICKS);
        roundChance = chance; // Freeze BEFORE consuming the stake.
        storage.removeItem(INPUT, 1);
        lastInput = storage.getItem(INPUT).copy();
        storage.setItem(OUTPUT, prize);
        roll = result;
        ticks = LudkaRules.SPIN_TICKS;
        nextRound = nextRound == 32767 ? 1 : nextRound + 1;
        round = nextRound;
        broadcastChanges();
        return true;
    }
    @Override
    public void broadcastChanges() {
        if (!owner.level().isClientSide) {
            ticks = (int) Math.max(0, unlockTick - owner.level().getGameTime());
            refreshQuote();
        }
        super.broadcastChanges();
    }
    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (player != owner || serverClosed || isBusy()) return;
        super.clicked(slot, button, type, player);
    }
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player != owner || serverClosed || isBusy() || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, INPUT, INPUT + 1, false)) {
            if (index < 29) {
                if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 2, 29, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
    @Override public boolean stillValid(Player player) { return player == owner && player.isAlive() && !serverClosed; }
    @Override
    public void removed(Player player) {
        if (player.level().isClientSide) {
            super.removed(player);
            return;
        }
        if (serverClosed) return;
        serverClosed = true;
        super.removed(player);
        clearContainer(player, storage);
    }
}
