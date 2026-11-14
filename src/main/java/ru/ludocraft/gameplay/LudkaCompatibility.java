package ru.ludocraft.gameplay;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.ludocraft.LudoCraft;

/** Data-pack tags are automatically synced by Minecraft; no client-only allowlist. */
public final class LudkaCompatibility {
    public static final TagKey<Item> INPUTS = TagKey.create(Registries.ITEM, LudoCraft.id("ludka_inputs"));
    public static final TagKey<Item> BLACKLIST = TagKey.create(Registries.ITEM, LudoCraft.id("ludka_blacklist"));
    private static final TagKey<Item> TOOLS = common("tools");
    private static final TagKey<Item> ARMORS = common("armors");

    private LudkaCompatibility() {}
    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
    public static boolean blocked(ItemStack stack) {
        // The device itself can never be a stake, even if a pack replaces the blacklist.
        return stack.is(LudoCraft.POCKET_LUDKA.get()) || stack.is(BLACKLIST);
    }
    public static boolean equipment(ItemStack stack) {
        return stack.isDamageableItem() || stack.has(DataComponents.TOOL) || stack.is(TOOLS) || stack.is(ARMORS);
    }
    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && !blocked(stack) && (equipment(stack) || stack.is(INPUTS));
    }
}
