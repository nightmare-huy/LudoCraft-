package ru.ludocraft.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.ludocraft.LudoCraft;
import ru.ludocraft.gameplay.LudkaCompatibility;
import ru.ludocraft.gameplay.LudkaRules;
import ru.ludocraft.gameplay.LudkaValues;
import ru.ludocraft.menu.LudkaMenu;
import ru.ludocraft.network.SelectLudkaTarget;

@GameTestHolder(LudoCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LudkaGameTests {
    private static FakePlayer player(GameTestHelper h) {
        return new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "LudkaTest"));
    }
    private static LudkaMenu menu(FakePlayer player) {
        LudkaMenu menu = new LudkaMenu(1, player.getInventory());
        player.containerMenu = menu;
        return menu;
    }
    private static ResourceLocation id(Item item) { return BuiltInRegistries.ITEM.getKey(item); }
    private static long seedFor(int chance, boolean win) {
        for (long seed = 0; ; seed++) {
            if (LudkaRules.wins(chance, RandomSource.create(seed).nextInt(LudkaRules.PRECISION)) == win) return seed;
        }
    }
    @GameTest(template = "ludka_test")
    public static void dirtAndNetheriteAreNotEqual(GameTestHelper h) {
        FakePlayer p = player(h); LudkaMenu m = menu(p);
        m.getSlot(0).set(new ItemStack(Items.DIRT, 64));
        h.assertTrue(m.selectTarget(p, id(Items.NETHERITE_INGOT)), "Cannot select netherite");
        int dirtChance = m.chance();
        h.assertTrue(dirtChance == 244 && m.stakeValue() == 1 && m.targetValue() == 2048, "Wrong dirt quote / entire stack priced");
        m.getSlot(0).set(new ItemStack(Items.DIAMOND)); m.broadcastChanges();
        h.assertTrue(m.chance() == 31250 && m.chance() > dirtChance, "Diamond not more valuable than dirt");
        m.getSlot(0).set(new ItemStack(Items.NETHERITE_INGOT)); m.broadcastChanges();
        h.assertTrue(m.chance() == 500000, "Equal value quote wrong");
        m.selectTarget(p, id(Items.DIRT));
        h.assertTrue(m.chance() == 950000, "Cheap target not capped");
        m.removed(p); h.succeed();
    }
    @GameTest(template = "ludka_test")
    public static void wagersConsumeOneAndAwardChosenTarget(GameTestHelper h) {
        for (boolean win : new boolean[]{false, true}) {
            FakePlayer p = player(h); LudkaMenu m = menu(p);
            m.getSlot(0).set(new ItemStack(Items.DIAMOND, 3));
            h.assertTrue(m.selectTarget(p, id(Items.NETHERITE_INGOT)), "Target rejected");
            h.getLevel().random.setSeed(seedFor(m.chance(), win));
            h.assertTrue(m.clickMenuButton(p, 4), "Start rejected");
            h.assertTrue(m.won() == win && m.getSlot(0).getItem().getCount() == 2, "Outcome/consumption wrong");
            ItemStack prize = m.getSlot(1).getItem().copy();
            h.assertTrue(win ? prize.is(Items.NETHERITE_INGOT) && prize.getCount() == 1 : prize.isEmpty(), "Did not award exact target");
            int result = m.roll();
            h.assertTrue(!m.selectTarget(p, id(Items.DIRT)) && !m.clickMenuButton(p, 4), "Changed active wager");
            h.assertTrue(m.roll() == result && m.target().is(Items.NETHERITE_INGOT), "Request changed frozen outcome");
            for (ClickType type : ClickType.values()) m.clicked(1, 0, type, p);
            h.assertTrue(m.quickMoveStack(p, 1).isEmpty() && ItemStack.matches(prize, m.getSlot(1).getItem()), "Output transfer bypass");
            m.removed(p); m.removed(p);
            h.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 2, "Stake remainder duplicated/lost");
            h.assertTrue(p.getInventory().countItem(Items.NETHERITE_INGOT) == (win ? 1 : 0), "Prize duplicated/lost");
            LudkaMenu reopened = menu(p); reopened.getSlot(0).set(new ItemStack(Items.DIRT));
            reopened.selectTarget(p, id(Items.DIRT));
            h.assertTrue(!reopened.clickMenuButton(p, 4), "Reopen bypassed cooldown");
            reopened.removed(p);
        }
        h.succeed();
    }
    @GameTest(template = "ludka_test")
    public static void rejectsForgedTargetsAndStaleMenus(GameTestHelper h) {
        FakePlayer p = player(h); LudkaMenu m = menu(p);
        m.getSlot(0).set(new ItemStack(Items.DIRT));
        h.assertTrue(!m.clickMenuButton(p, 4), "Wager without target");
        h.assertTrue(!m.selectTarget(p, ResourceLocation.parse("missing_mod:nonexistent")), "Unknown target resolved to fallback");
        for (Item item : new Item[]{Items.AIR, Items.BARRIER, Items.COMMAND_BLOCK, LudoCraft.POCKET_LUDKA.get()}) {
            h.assertTrue(!m.selectTarget(p, id(item)), "Forbidden target accepted: " + item);
        }
        h.assertTrue(!m.selectTarget(player(h), id(Items.DIAMOND)), "Foreign owner accepted");
        m.selectTarget(p, id(Items.DIAMOND));
        m.getSlot(1).set(new ItemStack(Items.NETHER_STAR));
        h.assertTrue(!m.clickMenuButton(p, 4) && m.getSlot(0).getItem().getCount() == 1, "Occupied output consumed stake");
        h.assertTrue(!m.clickMenuButton(p, -1) && !m.clickMenuButton(p, 0) && !m.clickMenuButton(p, 5), "Legacy risk/invalid button accepted");
        m.removed(p);
        h.assertTrue(!m.selectTarget(p, id(Items.DIAMOND)) && !m.clickMenuButton(p, 4), "Closed menu accepted request");
        h.succeed();
    }
    @GameTest(template = "ludka_test", timeoutTicks = 100)
    public static void frozenOddsSurviveEmptyInputAndSecondSpin(GameTestHelper h) {
        FakePlayer p = player(h); LudkaMenu m = menu(p);
        m.getSlot(0).set(new ItemStack(Items.DIAMOND)); m.selectTarget(p, id(Items.NETHERITE_INGOT));
        int chance = m.chance();
        h.getLevel().random.setSeed(seedFor(chance, true));
        h.assertTrue(m.clickMenuButton(p, 4), "Initial spin rejected");
        int firstRound = m.round();
        h.runAfterDelay(61, () -> {
            m.broadcastChanges();
            h.assertTrue(!m.isBusy() && m.won() && m.displayedChance() == chance, "Consumed stake changed result/green zone");
            h.assertTrue(!m.quickMoveStack(p, 1).isEmpty(), "Cannot take unlocked prize");
            m.getSlot(0).set(new ItemStack(Items.DIAMOND));
            p.getCooldowns().removeCooldown(LudoCraft.POCKET_LUDKA.get());
            h.getLevel().random.setSeed(seedFor(chance, true));
            h.assertTrue(m.clickMenuButton(p, 4) && m.round() != firstRound, "Repeated outcome reused round token");
            m.removed(p); h.succeed();
        });
    }
    @GameTest(template = "ludka_test")
    public static void actualInventoryClicksAndDrag(GameTestHelper h) {
        FakePlayer p = player(h); LudkaMenu m = menu(p);
        p.getInventory().setItem(9, new ItemStack(Items.DIRT, 3));
        m.clicked(2, 0, ClickType.PICKUP, p); m.clicked(0, 0, ClickType.PICKUP, p);
        m.selectTarget(p, id(Items.DIAMOND));
        h.assertTrue(m.canStart() && m.getSlot(0).getItem().getCount() == 3, "Dirt cannot be inserted/wagered");
        m.clicked(0, 1, ClickType.PICKUP, p);
        h.assertTrue(m.getCarried().getCount() == 2, "Right click failed");
        m.clicked(0, 0, ClickType.PICKUP, p); m.clicked(0, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(m.getSlot(0).getItem().isEmpty() && p.getInventory().countItem(Items.DIRT) == 3, "Shift return failed");
        p.getInventory().setItem(0, new ItemStack(Items.WHEAT_SEEDS)); m.clicked(29, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(m.getSlot(0).getItem().is(Items.WHEAT_SEEDS), "Shift insert of ordinary item failed");
        m.clicked(0, 0, ClickType.PICKUP, p); m.clicked(2, 0, ClickType.PICKUP, p);
        p.getInventory().setItem(0, new ItemStack(Items.EMERALD, 2)); m.clicked(0, 0, ClickType.SWAP, p);
        m.clicked(0, 0, ClickType.PICKUP, p);
        m.clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT, p);
        m.clicked(0, AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT, p);
        m.clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ClickType.QUICK_CRAFT, p);
        h.assertTrue(m.getCarried().isEmpty() && m.getSlot(0).getItem().getCount() == 2, "Drag insert failed");
        m.removed(p); h.succeed();
    }
    @GameTest(template = "ludka_test")
    public static void registryCoverageAndValuation(GameTestHelper h) {
        for (var item : BuiltInRegistries.ITEM) {
            var stack = item.getDefaultInstance();
            if (LudkaCompatibility.accepts(stack, h.getLevel())) {
                int value = LudkaValues.base(stack);
                h.assertTrue(value > 0 && value <= LudkaRules.MAX_VALUE, "Unpriced registry item: " + id(item));
            }
        }
        for (var item : new Item[]{Items.DIRT, Items.POTATO, Items.WHEAT_SEEDS, Items.STICK, Items.COBBLESTONE,
                Items.BOW, LudoCraft.GAMBLERS_PICKAXE.get()}) {
            h.assertTrue(LudkaCompatibility.accepts(new ItemStack(item)), "Ordinary item rejected: " + id(item));
        }
        ItemStack tool = new ItemStack(LudoCraft.GAMBLERS_PICKAXE.get());
        h.assertTrue(LudkaValues.base(tool) == 512, "Explicit mod-item value tag ignored");
        tool.setDamageValue(tool.getMaxDamage() - 1);
        h.assertTrue(LudkaValues.stake(tool) < 512, "Broken tool priced as new");
        ItemStack dirt = new ItemStack(Items.DIRT, 64);
        dirt.set(DataComponents.CUSTOM_NAME, Component.literal("NETHeRITE LEGENDARY"));
        h.assertTrue(LudkaValues.stake(dirt) == 1, "Name or count inflated value");
        FakePlayer p = player(h); LudkaMenu m = menu(p);
        h.assertTrue(m.selectTarget(p, id(LudoCraft.GAMBLERS_PICKAXE.get())) && m.target().is(LudoCraft.GAMBLERS_PICKAXE.get()), "Mod target rejected");
        m.removed(p); h.succeed();
    }
    @GameTest(template = "ludka_test")
    public static void dataSlotsAndSelectionCodecRoundTrip(GameTestHelper h) {
        FakePlayer p = player(h); LudkaMenu source = menu(p), replica = menu(player(h));
        source.setSynchronizer(new ContainerSynchronizer() {
            @Override public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> items, ItemStack carried, int[] data) {
                replica.initializeContents(0, items, carried);
                for (int i = 0; i < data.length; i++) replica.setData(i, (short) data[i]);
            }
            @Override public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) { replica.setItem(slot, 0, stack.copy()); }
            @Override public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) { replica.setCarried(stack.copy()); }
            @Override public void sendDataChange(AbstractContainerMenu menu, int index, int value) { replica.setData(index, (short) value); }
        });
        source.getSlot(0).set(new ItemStack(Items.NETHERITE_INGOT)); source.selectTarget(p, id(Items.DIRT));
        h.assertTrue(replica.chance() == 950000 && replica.target().is(Items.DIRT), "16-bit quote/target truncation");
        h.getLevel().random.setSeed(seedFor(source.chance(), false));
        h.assertTrue(source.clickMenuButton(p, 4), "Sync test start failed");
        h.assertTrue(replica.roll() == source.roll() && replica.roll() >= 950000
                && replica.displayedChance() == source.displayedChance() && !replica.won(), "16-bit roll truncated");
        var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            var packet = new SelectLudkaTarget(17, id(LudoCraft.GAMBLERS_PICKAXE.get()));
            SelectLudkaTarget.CODEC.encode(buffer, packet);
            h.assertTrue(packet.equals(SelectLudkaTarget.CODEC.decode(buffer)), "Selection payload roundtrip failed");
        } finally { buffer.release(); }
        source.removed(p); h.succeed();
    }
}
