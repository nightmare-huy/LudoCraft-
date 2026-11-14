package ru.ludocraft.gameplay;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class LudkaRewards {
    // Exact vanilla upgrades; all other accepted equipment keeps its own mod item/components.
    private static final List<Item> IRON = List.of(Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE,
            Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_HELMET, Items.IRON_CHESTPLATE,
            Items.IRON_LEGGINGS, Items.IRON_BOOTS);
    private static final List<Item> DIAMOND = List.of(Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE,
            Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
    private static final List<Item> NETHERITE = List.of(Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE,
            Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
    private LudkaRewards() {}

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && !LudkaCompatibility.blocked(stack)
                && (IRON.contains(stack.getItem()) || DIAMOND.contains(stack.getItem())
                || stack.is(Items.DIAMOND) || stack.is(Items.EMERALD) || LudkaCompatibility.accepts(stack));
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
                    ItemStack prize = gearIndex < 0 && LudkaCompatibility.equipment(input)
                            ? repairedCopy(input) : new ItemStack(NETHERITE.get(gearIndex >= 0 ? gearIndex : 1));
                    enchant(level, prize, true);
                    prize.set(DataComponents.CUSTOM_NAME, Component.translatable("item.ludocraft.ludka_jackpot")
                            .withStyle(ChatFormatting.GOLD));
                    yield prize;
                }
            };
        }
        if (gearIndex < 0 && LudkaCompatibility.equipment(input)) {
            // Do not silently turn a powerful mod weapon into a weaker vanilla item.
            ItemStack prize = repairedCopy(input);
            enchant(level, prize, risk >= 2);
            return prize;
        }
        if (gearIndex < 0) {
            if (!input.is(Items.DIAMOND) && !input.is(Items.EMERALD) && risk <= 1) {
                // Gems/ingots and explicit pack-added resources preserve namespace and components.
                return input.copyWithCount(Math.min(risk == 0 ? 2 : 4, input.getMaxStackSize()));
            }
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

    private static ItemStack repairedCopy(ItemStack input) {
        ItemStack result = input.copyWithCount(1);
        if (result.isDamageableItem()) result.setDamageValue(0);
        return result;
    }

    private static boolean compatible(ItemStack stack, Holder<Enchantment> candidate) {
        // Use NeoForge's item hook, not only vanilla supported_items: mods can override it.
        if (candidate.is(EnchantmentTags.CURSE) || !stack.supportsEnchantment(candidate)) return false;
        for (var existing : EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet()) {
            if (!existing.equals(candidate) && !Enchantment.areCompatible(existing, candidate)) return false;
        }
        return true;
    }

    private static void enchant(ServerLevel level, ItemStack stack, boolean maximum) {
        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var applicable = new ArrayList<Holder<Enchantment>>();
        registry.holders().forEach(holder -> {
            int existing = EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(holder);
            if (compatible(stack, holder) && existing < holder.value().getMaxLevel()) applicable.add(holder);
        });
        if (!maximum && !applicable.isEmpty()) {
            var chosen = applicable.get(level.random.nextInt(applicable.size()));
            int current = EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(chosen);
            int upgraded = current + 1 + level.random.nextInt(chosen.value().getMaxLevel() - current);
            stack.enchant(chosen, upgraded);
        } else if (maximum) {
            for (var holder : applicable) {
                // Recheck after each addition: never combine mutually exclusive mod/vanilla enchants.
                if (compatible(stack, holder)) stack.enchant(holder, holder.value().getMaxLevel());
            }
        }
    }
}
