package ru.ludocraft.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

/** Custom endgame tool tier: netherite mining rules with higher durability and speed. */
public final class HighrollerTier {
    public static final Tier INSTANCE = new SimpleTier(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4096, 12.0F, 5.0F, 22,
            () -> Ingredient.of(Items.NETHERITE_INGOT));

    private HighrollerTier() {}
}
