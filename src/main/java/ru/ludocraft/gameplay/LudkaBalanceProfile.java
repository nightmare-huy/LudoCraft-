package ru.ludocraft.gameplay;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.ludocraft.LudoCraft;

/** Profile selection is a synced datapack tag, never guessed from installed mod names. */
public final class LudkaBalanceProfile {
    public static final TagKey<Item> EVOLUTION = tag("ludocraft", "balance/ftb_evolution_1_47_0");
    public static final TagKey<Item> TARGET_BLACKLIST = tag("ludocraft", "ludka_target_blacklist");
    public static final TagKey<Item> EVOLUTION_ALLOWLIST = tag("ludocraft", "evolution_target_allowlist");
    private static final TagKey<Item> HIDDEN = tag("c", "hidden_from_recipe_viewers");
    private static final TagKey<Item> ORES = tag("c", "ores"), RAW = tag("c", "raw_materials");
    private LudkaBalanceProfile() {}
    private static TagKey<Item> tag(String ns, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ns, path));
    }
    public static boolean evolutionEnabled() {
        return LudoCraft.POCKET_LUDKA.get().getDefaultInstance().is(EVOLUTION);
    }
    public static boolean targetAllowed(ItemStack stack) {
        if (stack.is(TARGET_BLACKLIST)) return false;
        if (!evolutionEnabled() || stack.is(EVOLUTION_ALLOWLIST)) return true;
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        // Ancient debris is not necessarily in c:ores on every pack version.
        boolean rawBlock = stack.getTags().anyMatch(tag -> tag.location().getNamespace().equals("c")
                && tag.location().getPath().startsWith("storage_blocks/raw_"));
        return !stack.is(HIDDEN) && !stack.is(ORES) && !stack.is(RAW) && !rawBlock
                && !id.toString().equals("minecraft:ancient_debris")
                && !LudkaEvolutionValues.blocksTarget(id.getNamespace(), id.getPath());
    }
}
