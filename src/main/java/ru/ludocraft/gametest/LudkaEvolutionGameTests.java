package ru.ludocraft.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.ludocraft.gameplay.LudkaBalanceProfile;
import ru.ludocraft.gameplay.LudkaCompatibility;
import ru.ludocraft.gameplay.LudkaValues;
import ru.ludocraft.menu.LudkaMenu;

/** Runs separately with the actual distributed preset + test-only override pack. */
@GameTestHolder("ludocraft_evolution")
@PrefixGameTestTemplate(false)
public final class LudkaEvolutionGameTests {
    @GameTest(template = "ludka_test")
    public static void presetLoadsAndOverridesWin(GameTestHelper h) {
        h.assertTrue(LudkaBalanceProfile.evolutionEnabled(), "Distributed Evolution datapack did not activate");
        check(h, Items.IRON_INGOT, 8); check(h, Items.IRON_BLOCK, 72);
        check(h, Items.DIAMOND, 32); check(h, Items.NETHERITE_INGOT, 512);
        check(h, Items.IRON_PICKAXE, 24); check(h, Items.NETHERITE_PICKAXE, 608);
        check(h, Items.BOW, 16); check(h, Items.STICK, 8192); // highest explicit tag beats preset and value/1
        ItemStack damaged = new ItemStack(Items.BOW);
        damaged.setDamageValue(damaged.getMaxDamage() - 1);
        h.assertTrue(LudkaValues.stake(damaged) == 1, "Damage discount lost");
        h.succeed();
    }
    @GameTest(template = "ludka_test")
    public static void targetGuardsDoNotBanStakes(GameTestHelper h) {
        FakePlayer p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "EvolutionTest"));
        LudkaMenu menu = new LudkaMenu(1, p.getInventory()); p.containerMenu = menu;
        menu.getSlot(0).set(new ItemStack(Items.DIRT, 3));
        for (Item item : new Item[]{Items.IRON_ORE, Items.RAW_IRON, Items.RAW_IRON_BLOCK, Items.ANCIENT_DEBRIS, Items.APPLE, Items.FEATHER}) {
            h.assertTrue(LudkaCompatibility.accepts(new ItemStack(item), h.getLevel()), "Target restriction leaked into stakes");
            h.assertTrue(!LudkaCompatibility.acceptsTarget(new ItemStack(item), h.getLevel()), "Forbidden target accepted");
            h.assertTrue(!menu.selectTarget(p, BuiltInRegistries.ITEM.getKey(item)), "Forged target ID bypassed profile");
        }
        h.assertTrue(LudkaCompatibility.acceptsTarget(new ItemStack(Items.LEATHER), h.getLevel()), "Profile allowlist ignored");
        h.assertTrue(!LudkaCompatibility.acceptsTarget(new ItemStack(Items.BARRIER), h.getLevel()), "Global blacklist bypassed");
        h.assertTrue(menu.selectTarget(p, BuiltInRegistries.ITEM.getKey(Items.NETHERITE_INGOT)), "Normal target rejected");
        h.assertTrue(menu.targetValue() == 512 && menu.stakeValue() == 1 && menu.chance() == 976, "Wrong preset server quote");
        h.assertTrue(menu.clickMenuButton(p, 4), "Preset wager failed");
        h.assertTrue(menu.getSlot(0).getItem().getCount() == 2, "Preset consumed more than one item");
        h.assertTrue(!menu.selectTarget(p, BuiltInRegistries.ITEM.getKey(Items.DIAMOND)), "Changed running wager");
        menu.removed(p); h.succeed();
    }
    private static void check(GameTestHelper h, Item item, int expected) {
        h.assertTrue(LudkaValues.base(new ItemStack(item)) == expected, "Wrong preset price for " + item);
    }
}
