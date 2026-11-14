package ru.ludocraft.menu;

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
import ru.ludocraft.gameplay.LudkaRewards;
import ru.ludocraft.gameplay.LudkaRules;

public final class LudkaMenu extends AbstractContainerMenu {
    public static final int INPUT = 0, OUTPUT = 1;
    private static final int RISK = 0, ROLL = 1, TICKS = 2, ROUND = 3, ACK = 4;
    private final Player owner;
    private final SimpleContainer storage = new SimpleContainer(2);
    private final DataSlot risk = DataSlot.standalone();
    private final DataSlot roll = DataSlot.standalone();
    private final DataSlot ticks = DataSlot.standalone();
    private final DataSlot round = DataSlot.standalone();
    private final DataSlot ack = DataSlot.standalone();
    private long unlockTick;
    private int nextRound;
    private boolean removed;
    // Client presentation state lives on the menu so resizing the screen cannot reveal/restart a spin.
    private boolean clientPending;
    private long clientStart;
    private int clientRound;
    private int soundedRound;

    public LudkaMenu(int id, Inventory inventory) {
        super(LudoCraft.LUDKA_MENU.get(), id);
        owner = inventory.player;
        roll.set(-1);
        addSlot(new Slot(storage, INPUT, 24, 38) {
            @Override public boolean mayPlace(ItemStack stack) { return !isBusy() && LudkaRewards.accepts(stack); }
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
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 69 + column * 18, 218));
        }
        // ROUND and ACK must be last: the roll and remaining time arrive before the start/acknowledgement.
        addDataSlot(risk);
        addDataSlot(roll);
        addDataSlot(ticks);
        addDataSlot(round);
        addDataSlot(ack);
    }

    public int risk() { return risk.get(); }
    public int roll() { return roll.get(); }
    public int round() { return round.get(); }
    public boolean hasResult() { return round.get() != 0; }
    public boolean won() { return hasResult() && LudkaRules.wins(risk.get(), roll.get()); }
    public boolean pending() { return clientPending; }
    public boolean isBusy() {
        if (!owner.level().isClientSide) return owner.level().getGameTime() < unlockTick;
        return clientPending || ticks.get() > 0 || (clientRound != 0 && clientProgress() < 1.0);
    }
    public double clientProgress() {
        return Math.clamp((System.nanoTime() - clientStart) / (double) LudkaRules.SPIN_NANOS, 0.0, 1.0);
    }
    public boolean canStart() {
        return !removed && !isBusy() && LudkaRewards.accepts(storage.getItem(INPUT))
                && storage.getItem(OUTPUT).isEmpty();
    }
    public void beginClientRequest() {
        if (owner.level().isClientSide) clientPending = true;
    }
    public boolean takeCompletionSound() {
        if (hasResult() && !isBusy() && soundedRound != round.get()) {
            soundedRound = round.get();
            return true;
        }
        return false;
    }

    @Override
    public void setData(int index, int value) {
        super.setData(index, value);
        if (!owner.level().isClientSide) return;
        if (index == ROUND && value != 0 && value != clientRound) {
            clientRound = value;
            clientStart = System.nanoTime();
            clientPending = false;
        }
        if (index == ACK) clientPending = false; // Rejected requests must not leave the GUI locked.
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (player != owner || player.containerMenu != this || removed || player.isSpectator()
                || !(player.level() instanceof ServerLevel level)) return false;
        if (button >= 0 && button < 4) {
            if (isBusy()) return false;
            risk.set(button);
            // A previous result must not be reinterpreted with a newly selected probability.
            roll.set(-1);
            round.set(0);
            broadcastChanges();
            return true;
        }
        if (button != 4) return false;
        ack.set((ack.get() + 1) & 32767);
        if (!canStart() || player.getCooldowns().isOnCooldown(LudoCraft.POCKET_LUDKA.get())) {
            broadcastChanges();
            return false;
        }
        int result = level.random.nextInt(100); // Exact probabilities: 0..99, green iff result < chance.
        ItemStack wager = storage.getItem(INPUT);
        // Calculate before consuming; a malformed data pack cannot burn a wager without a result.
        ItemStack prize = LudkaRules.wins(risk.get(), result)
                ? LudkaRewards.create(level, wager, risk.get()) : ItemStack.EMPTY;
        unlockTick = level.getGameTime() + LudkaRules.SPIN_TICKS;
        player.getCooldowns().addCooldown(LudoCraft.POCKET_LUDKA.get(), LudkaRules.SPIN_TICKS);
        storage.removeItem(INPUT, 1); // Exactly ONE item, including stacked diamonds/emeralds.
        storage.setItem(OUTPUT, prize); // Immediate server outcome; client presentation masks this slot.
        roll.set(result);
        ticks.set(LudkaRules.SPIN_TICKS);
        // Monotonic request token, including consecutive identical rolls (16-bit safe).
        nextRound = nextRound == 32767 ? 1 : nextRound + 1;
        round.set(nextRound);
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {
        if (!owner.level().isClientSide) {
            ticks.set((int) Math.max(0, unlockTick - owner.level().getGameTime()));
        }
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        // Covers number keys, Q, double-click collection, drag, offhand swap and creative clone.
        if (player != owner || removed || isBusy()) return;
        super.clicked(slot, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player != owner || removed || isBusy() || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else if (!LudkaRewards.accepts(stack) || !moveItemStackTo(stack, INPUT, INPUT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override public boolean stillValid(Player player) { return player == owner && player.isAlive() && !removed; }

    @Override
    public void removed(Player player) {
        if (removed) return;
        removed = true;
        super.removed(player);
        // Closing, disconnecting or dying never rerolls or destroys an already awarded prize.
        // The item cooldown survives closing, so reopening cannot accelerate gambling.
        if (!player.level().isClientSide) clearContainer(player, storage);
    }
}
