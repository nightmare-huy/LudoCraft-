package ru.ludocraft.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import ru.ludocraft.gameplay.LudkaCompatibility;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.ludocraft.LudoCraft;
import ru.ludocraft.gameplay.LudkaRewards;
import ru.ludocraft.gameplay.LudkaRules;
import ru.ludocraft.menu.LudkaMenu;

@GameTestHolder(LudoCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LudkaGameTests {
    private static FakePlayer player(GameTestHelper helper) {
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "LudkaTest"));
    }
    private static LudkaMenu menu(FakePlayer player) {
        LudkaMenu menu = new LudkaMenu(1, player.getInventory());
        player.containerMenu = menu;
        return menu;
    }
    private static long seedFor(int risk, boolean win) {
        for (long seed = 0; ; seed++) {
            if (LudkaRules.wins(risk, RandomSource.create(seed).nextInt(100)) == win) return seed;
        }
    }

    @GameTest(template = "ludka_test")
    public static void wagerAndCloseAreAtomic(GameTestHelper h) {
        for (int risk = 0; risk < 4; risk++) {
            for (boolean win : new boolean[]{false, true}) {
                FakePlayer p = player(h);
                LudkaMenu m = menu(p);
                m.getSlot(0).set(new ItemStack(Items.DIAMOND, 3));
                h.assertTrue(m.clickMenuButton(p, risk), "Risk rejected");
                h.getLevel().random.setSeed(seedFor(risk, win));
                h.assertTrue(m.clickMenuButton(p, 4), "Wager rejected");
                h.assertTrue(m.won() == win, "Wrong result");
                h.assertTrue(m.getSlot(0).getItem().getCount() == 2, "Must consume exactly one");
                ItemStack prize = m.getSlot(1).getItem().copy();
                h.assertTrue(prize.isEmpty() != win, "Server prize must be available immediately, only on a win");
                int roll = m.roll();
                h.assertTrue(!m.clickMenuButton(p, 4), "Duplicate start accepted");
                h.assertTrue(!m.clickMenuButton(p, (risk + 1) % 4), "Risk changed mid-spin");
                h.assertTrue(m.roll() == roll && m.getSlot(0).getItem().getCount() == 2, "Duplicate request changed wager");
                h.assertTrue(!m.getSlot(1).mayPickup(p) && !m.getSlot(1).isActive(), "Output not locked");
                h.assertTrue(m.quickMoveStack(p, 1).isEmpty(), "Shift-click bypass");
                for (ClickType type : ClickType.values()) m.clicked(1, 0, type, p);
                h.assertTrue(ItemStack.matches(prize, m.getSlot(1).getItem()), "Click bypass changed output");
                m.removed(p);
                m.removed(p); // Idempotence: closing twice cannot duplicate the prize/remainder.
                h.assertTrue(m.getSlot(0).getItem().isEmpty() && m.getSlot(1).getItem().isEmpty(), "Storage not cleared");
                int expectedDiamonds = 2 + (prize.is(Items.DIAMOND) ? prize.getCount() : 0);
                h.assertTrue(p.getInventory().countItem(Items.DIAMOND) == expectedDiamonds, "Remainder/prize lost or duplicated");
                if (!prize.isEmpty() && !prize.is(Items.DIAMOND)) {
                    h.assertTrue(p.getInventory().countItem(prize.getItem()) == prize.getCount(), "Prize not returned");
                }
                h.assertTrue(p.getCooldowns().isOnCooldown(LudoCraft.POCKET_LUDKA.get()), "Close removed cooldown");
                LudkaMenu reopened = menu(p);
                reopened.getSlot(0).set(new ItemStack(Items.DIAMOND));
                h.assertTrue(!reopened.clickMenuButton(p, 4), "Reopening bypassed cooldown");
                reopened.removed(p);
            }
        }
        h.succeed();
    }

    @GameTest(template = "ludka_test")
    public static void rejectsInvalidInputsAndOccupiedOutput(GameTestHelper h) {
        FakePlayer p = player(h);
        LudkaMenu m = menu(p);
        h.assertTrue(!m.clickMenuButton(p, -1) && !m.clickMenuButton(p, 5), "Unknown ID accepted");
        h.assertTrue(!m.clickMenuButton(p, 4), "Empty wager accepted");
        h.assertTrue(m.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "Inspection slot must accept an unsupported item");
        m.getSlot(0).set(new ItemStack(Items.DIRT)); // Even forged slot contents must be revalidated.
        h.assertTrue(!m.clickMenuButton(p, 4), "Server accepted dirt");
        m.getSlot(0).set(new ItemStack(Items.DIAMOND, 2));
        m.getSlot(1).set(new ItemStack(Items.NETHER_STAR));
        h.assertTrue(!m.getSlot(1).mayPlace(new ItemStack(Items.DIAMOND)), "Can insert in output");
        h.assertTrue(!m.clickMenuButton(p, 4), "Overwrote occupied output");
        h.assertTrue(m.getSlot(0).getItem().getCount() == 2 && m.getSlot(1).getItem().is(Items.NETHER_STAR), "Rejected wager changed items");
        h.assertTrue(!m.clickMenuButton(player(h), 4), "Another player used the menu");
        m.removed(p);
        h.succeed();
    }

    @GameTest(template = "ludka_test", timeoutTicks = 100)
    public static void unlocksAndCanRollAgain(GameTestHelper h) {
        FakePlayer p = player(h);
        LudkaMenu m = menu(p);
        m.getSlot(0).set(new ItemStack(Items.IRON_PICKAXE));
        h.getLevel().random.setSeed(seedFor(0, true));
        h.assertTrue(m.clickMenuButton(p, 4), "First start rejected");
        int firstRound = m.round();
        h.assertTrue(m.getSlot(1).getItem().is(Items.DIAMOND_PICKAXE), "Wrong basic upgrade");
        h.runAfterDelay(61, () -> {
            m.broadcastChanges();
            h.assertTrue(!m.isBusy() && m.getSlot(1).mayPickup(p), "Output did not unlock");
            h.assertTrue(!m.quickMoveStack(p, 1).isEmpty(), "Cannot take unlocked output");
            h.assertTrue(m.getSlot(1).getItem().isEmpty(), "Shift transfer did not clear output");
            h.assertTrue(m.clickMenuButton(p, 1), "Risk change rejected");
            m.getSlot(0).set(new ItemStack(Items.DIAMOND_PICKAXE));
            p.getCooldowns().removeCooldown(LudoCraft.POCKET_LUDKA.get()); // FakePlayer.tick is intentionally empty.
            h.getLevel().random.setSeed(seedFor(1, true));
            h.assertTrue(m.clickMenuButton(p, 4) && m.round() != firstRound, "Round token reused after risk change");
            h.assertTrue(m.getSlot(1).getItem().is(Items.NETHERITE_PICKAXE)
                    && m.getSlot(1).getItem().isEnchanted(), "25% diamond upgrade lacks enchantment");
            m.removed(p);
            h.succeed();
        });
    }

    @GameTest(template = "ludka_test")
    public static void rewardTiers(GameTestHelper h) {
        var level = h.getLevel();
        h.assertTrue(LudkaRewards.create(level, new ItemStack(Items.IRON_SWORD), 0).is(Items.DIAMOND_SWORD), "Iron to diamond");
        h.assertTrue(LudkaRewards.create(level, new ItemStack(Items.IRON_SWORD), 1).is(Items.NETHERITE_SWORD), "Iron to netherite");
        ItemStack high = LudkaRewards.create(level, new ItemStack(Items.DIAMOND_PICKAXE), 2);
        h.assertTrue(high.is(Items.NETHERITE_PICKAXE) && high.isEnchanted(), "High-tier enchanted tool");
        boolean[] found = new boolean[4];
        // Seed each outcome explicitly rather than relying on a statistical test.
        for (int outcome = 0; outcome < 4; outcome++) {
            long seed = 0;
            while (RandomSource.create(seed).nextInt(4) != outcome) seed++;
            level.random.setSeed(seed);
            ItemStack prize = LudkaRewards.create(level, new ItemStack(Items.EMERALD), 3);
            found[outcome] = switch (outcome) {
                case 0 -> prize.is(Items.ELYTRA);
                case 1 -> prize.is(Items.NETHER_STAR);
                case 2 -> prize.is(Items.MACE);
                default -> prize.is(Items.NETHERITE_PICKAXE) && prize.isEnchanted() && prize.has(DataComponents.CUSTOM_NAME);
            };
        }
        for (boolean valid : found) h.assertTrue(valid, "Missing jackpot outcome");
        h.succeed();
    }
    @GameTest(template = "ludka_test")
    public static void actualInventoryClicksAndShiftTransfer(GameTestHelper h) {
        FakePlayer p = player(h);
        LudkaMenu m = menu(p);
        p.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 3));
        m.clicked(2, 0, ClickType.PICKUP, p); // Pick up from the first visible inventory slot.
        h.assertTrue(m.getCarried().getCount() == 3, "Cannot pick up an inventory item");
        m.clicked(0, 0, ClickType.PICKUP, p); // Place via the same path as a real client packet.
        h.assertTrue(m.getCarried().isEmpty() && m.getSlot(0).getItem().getCount() == 3, "Left-click cannot insert diamonds");
        h.assertTrue(m.canStart(), "Valid inserted wager cannot start");
        m.clicked(0, 1, ClickType.PICKUP, p);
        h.assertTrue(m.getCarried().getCount() == 2 && m.getSlot(0).getItem().getCount() == 1, "Right-click split failed");
        m.clicked(0, 0, ClickType.PICKUP, p);
        m.clicked(0, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(m.getSlot(0).getItem().isEmpty() && p.getInventory().countItem(Items.DIAMOND) == 3, "Shift return lost items");
        // The hotbar's first slot corresponds to menu slot 29.
        p.getInventory().setItem(0, new ItemStack(Items.IRON_PICKAXE));
        m.clicked(29, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(m.getSlot(0).getItem().is(Items.IRON_PICKAXE), "Shift insert failed");
        m.clicked(0, 0, ClickType.PICKUP, p);
        m.clicked(2, 0, ClickType.PICKUP, p);
        // Number-key swap into the input (same path as hotbar keyboard shortcuts).
        p.getInventory().setItem(0, new ItemStack(Items.EMERALD, 2));
        m.clicked(0, 0, ClickType.SWAP, p);
        h.assertTrue(m.getSlot(0).getItem().is(Items.EMERALD), "Hotbar swap failed");
        m.clicked(0, 0, ClickType.PICKUP, p);
        h.assertTrue(m.getCarried().getCount() == 2, "Cannot retrieve input");
        m.clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT, p);
        m.clicked(0, AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT, p);
        m.clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ClickType.QUICK_CRAFT, p);
        h.assertTrue(m.getCarried().isEmpty() && m.getSlot(0).getItem().getCount() == 2, "Drag insert failed");
        m.removed(p);
        h.succeed();
    }

    @GameTest(template = "ludka_test")
    public static void acceptsModToolAndPreservesComponents(GameTestHelper h) {
        FakePlayer p = player(h);
        LudkaMenu m = menu(p);
        ItemStack modTool = new ItemStack(LudoCraft.GAMBLERS_PICKAXE.get());
        modTool.setDamageValue(140);
        modTool.set(DataComponents.CUSTOM_NAME, Component.literal("Mod tool fixture"));
        CompoundTag data = new CompoundTag();
        data.putInt("energy", 12345);
        data.putString("owner", "test-owner");
        modTool.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        var enchants = h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var silk = enchants.getHolderOrThrow(Enchantments.SILK_TOUCH);
        var fortune = enchants.getHolderOrThrow(Enchantments.FORTUNE);
        modTool.enchant(silk, 1);
        h.assertTrue(LudkaRewards.accepts(modTool), "Non-vanilla registry item rejected");
        m.setCarried(modTool.copy());
        m.clicked(0, 0, ClickType.PICKUP, p);
        h.assertTrue(m.canStart(), "Mod tool cannot be inserted/wagered");
        ItemStack prize = LudkaRewards.create(h.getLevel(), modTool, 2);
        h.assertTrue(prize.is(modTool.getItem()) && prize.getCount() == 1, "Mod tool converted into vanilla/duplicated");
        h.assertTrue(prize.getDamageValue() == 0 && modTool.getDamageValue() == 140, "Repair mutated input or failed");
        h.assertTrue(prize.get(DataComponents.CUSTOM_NAME).equals(modTool.get(DataComponents.CUSTOM_NAME)), "Custom name lost");
        h.assertTrue(prize.get(DataComponents.CUSTOM_DATA).equals(modTool.get(DataComponents.CUSTOM_DATA)), "Mod data lost");
        h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(silk, prize) == 1, "Existing enchantment lost");
        h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(fortune, prize) == 0, "Incompatible enchantment added");
        m.removed(p);
        h.succeed();
    }

    @GameTest(template = "ludka_test")
    public static void commonTagsAndBlacklist(GameTestHelper h) {
        for (var item : new net.minecraft.world.item.Item[]{Items.IRON_INGOT, Items.AMETHYST_SHARD,
                Items.NETHERITE_PICKAXE, Items.BOW, Items.SHIELD}) {
            h.assertTrue(LudkaRewards.accepts(new ItemStack(item)), "Common resource/equipment rejected: " + item);
        }
        ItemStack ingot = new ItemStack(Items.IRON_INGOT);
        h.assertTrue(ingot.is(LudkaCompatibility.INPUTS), "Common tag was not loaded");
        ItemStack prize = LudkaRewards.create(h.getLevel(), ingot, 1);
        h.assertTrue(prize.is(Items.IRON_INGOT) && prize.getCount() == 4, "Resource identity/count changed");
        ItemStack pocket = new ItemStack(LudoCraft.POCKET_LUDKA.get());
        h.assertTrue(pocket.is(LudkaCompatibility.BLACKLIST) && !LudkaRewards.accepts(pocket), "Blacklist ignored");
        FakePlayer p = player(h);
        LudkaMenu m = menu(p);
        m.setCarried(new ItemStack(Items.DIRT, 7));
        m.clicked(0, 0, ClickType.PICKUP, p);
        h.assertTrue(m.getSlot(0).getItem().getCount() == 7 && m.unsupportedInput(), "Cannot inspect unsupported input");
        h.assertTrue(!m.clickMenuButton(p, 4) && m.getSlot(0).getItem().getCount() == 7, "Unsupported wager burned items");
        m.clicked(0, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(p.getInventory().countItem(Items.DIRT) == 7, "Cannot take unsupported input back");
        m.removed(p);
        h.succeed();
    }

}
