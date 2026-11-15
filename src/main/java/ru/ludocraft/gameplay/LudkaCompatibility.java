package ru.ludocraft.gameplay;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ru.ludocraft.LudoCraft;

/** Any ordinary registry item, regardless of mod or category. Tags are synced by Minecraft. */
public final class LudkaCompatibility {
    public static final TagKey<Item> BLACKLIST = TagKey.create(Registries.ITEM, LudoCraft.id("ludka_blacklist"));
    private LudkaCompatibility() {}

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(LudoCraft.POCKET_LUDKA.get()) && !stack.is(BLACKLIST);
    }
    public static boolean accepts(ItemStack stack, Level level) {
        return accepts(stack) && stack.isItemEnabled(level.enabledFeatures());
    }
    public static boolean acceptsTarget(ItemStack stack, Level level) {
        return accepts(stack, level) && LudkaBalanceProfile.targetAllowed(stack);
    }
}
