package ru.ludocraft.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
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
        h.assertTrue(!m.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "Dirt accepted");
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
}
