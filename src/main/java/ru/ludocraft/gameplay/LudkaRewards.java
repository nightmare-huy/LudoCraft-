package ru.ludocraft.gameplay;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public final class LudkaRewards {
    // Explicit allowlist: unrelated/modded items cannot accidentally be consumed.
    private static final List<Item> IRON = List.of(Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE,
            Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_HELMET, Items.IRON_CHESTPLATE,
            Items.IRON_LEGGINGS, Items.IRON_BOOTS);
    private static final List<Item> DIAMOND = List.of(Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE,
            Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
    private static final List<Item> NETHERITE = List.of(Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE,
            Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
    // Mutually compatible, non-curse pool. No Silk Touch/Fortune or Protection conflicts.
    private static final List<ResourceKey<Enchantment>> ENCHANTS = List.of(
            Enchantments.EFFICIENCY, Enchantments.FORTUNE, Enchantments.SHARPNESS,
            Enchantments.LOOTING, Enchantments.SWEEPING_EDGE, Enchantments.FIRE_ASPECT,
            Enchantments.PROTECTION, Enchantments.RESPIRATION, Enchantments.AQUA_AFFINITY,
            Enchantments.FEATHER_FALLING, Enchantments.DEPTH_STRIDER,
            Enchantments.UNBREAKING, Enchantments.MENDING);

    private LudkaRewards() {}

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && (IRON.contains(stack.getItem()) || DIAMOND.contains(stack.getItem())
                || stack.is(Items.DIAMOND) || stack.is(Items.EMERALD));
    }

    public static ItemStack create(ServerLevel level, ItemStack input, int risk) {
        if (!accepts(input) || !LudkaRules.validRisk(risk)) {
            throw new IllegalArgumentException("Unsupported Ludka wager");
        }
        RandomSource random = level.random;
        int ironIndex = IRON.indexOf(input.getItem());
        int diamondIndex = DIAMOND.indexOf(input.getItem());
        int gearIndex = Math.max(ironIndex, diamondIndex);
        if (risk == 3) {
            return switch (random.nextInt(4)) {
                case 0 -> new ItemStack(Items.ELYTRA);
                case 1 -> new ItemStack(Items.NETHER_STAR);
                case 2 -> new ItemStack(Items.MACE);
                default -> {
                    ItemStack prize = new ItemStack(NETHERITE.get(gearIndex >= 0 ? gearIndex : 1));
                    enchant(level, prize, true);
                    prize.set(DataComponents.CUSTOM_NAME, Component.translatable("item.ludocraft.ludka_jackpot")
                            .withStyle(ChatFormatting.GOLD));
                    yield prize;
                }
            };
        }
        if (gearIndex < 0) {
            return switch (risk) {
                case 0 -> new ItemStack(Items.DIAMOND, input.is(Items.DIAMOND) ? 2 : 1);
                case 1 -> new ItemStack(Items.DIAMOND, 4);
                default -> new ItemStack(random.nextBoolean() ? Items.NETHERITE_INGOT : Items.ENCHANTED_GOLDEN_APPLE);
            };
        }
        ItemStack prize = new ItemStack(risk == 0 && ironIndex >= 0
                ? DIAMOND.get(gearIndex) : NETHERITE.get(gearIndex));
        if (risk == 2) enchant(level, prize, true);
        else if (risk == 1 && diamondIndex >= 0) enchant(level, prize, false);
        return prize;
    }

    private static void enchant(ServerLevel level, ItemStack stack, boolean maximum) {
        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var applicable = new ArrayList<net.minecraft.core.Holder<Enchantment>>();
        for (var key : ENCHANTS) {
            var holder = registry.getHolderOrThrow(key);
            if (holder.value().canEnchant(stack)) applicable.add(holder);
        }
        if (!maximum && !applicable.isEmpty()) {
            var chosen = applicable.get(level.random.nextInt(applicable.size()));
            stack.enchant(chosen, 1 + level.random.nextInt(chosen.value().getMaxLevel()));
        } else {
            for (var holder : applicable) stack.enchant(holder, holder.value().getMaxLevel());
        }
    }
}
