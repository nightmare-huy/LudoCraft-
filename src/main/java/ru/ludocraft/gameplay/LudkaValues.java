package ru.ludocraft.gameplay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import ru.ludocraft.LudoCraft;

/** Deterministic, side-independent estimates. Pack makers can override them using value tags. */
public final class LudkaValues {
    private static final int[] OVERRIDE_VALUES = {1, 8, 32, 128, 512, 2048, 8192, 32767};
    private static final List<TagKey<Item>> OVERRIDES = java.util.Arrays.stream(OVERRIDE_VALUES)
            .mapToObj(value -> TagKey.create(Registries.ITEM, LudoCraft.id("value/" + value))).toList();
    private static final Map<String, Integer> VANILLA = new HashMap<>();
    private static final TagKey<Item> GEMS = common("gems"), INGOTS = common("ingots");
    static {
        values(1, "dirt", "cobblestone", "stone", "sand", "gravel", "netherrack", "rotten_flesh", "stick", "dirt_path");
        values(4, "coal", "charcoal", "redstone", "wheat", "leather", "string");
        values(8, "copper_ingot", "raw_iron", "amethyst_shard", "glass");
        values(16, "iron_ingot", "raw_gold", "lapis_lazuli", "quartz", "ender_pearl");
        values(32, "gold_ingot", "blaze_rod", "ender_eye");
        values(96, "emerald"); values(128, "diamond");
        values(384, "netherite_scrap", "ancient_debris"); values(2048, "netherite_ingot");
        values(4096, "enchanted_golden_apple", "totem_of_undying", "heavy_core");
        values(8192, "nether_star"); values(9216, "beacon"); values(12288, "elytra");
        values(16384, "mace"); values(24576, "dragon_egg");
        values(144, "iron_block"); values(288, "gold_block"); values(72, "copper_block");
        values(1152, "diamond_block"); values(864, "emerald_block"); values(18432, "netherite_block");
    }
    private LudkaValues() {}
    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
    private static void values(int value, String... paths) {
        for (String path : paths) VANILLA.put(path, value);
    }
    public static int base(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        // Explicit pack overrides win; if multiple tags match, the highest value wins.
        for (int i = OVERRIDE_VALUES.length - 1; i >= 0; i--) {
            if (stack.is(OVERRIDES.get(i))) return OVERRIDE_VALUES[i];
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.getNamespace().equals("minecraft")) {
            Integer exact = VANILLA.get(id.getPath());
            if (exact != null) return exact;
            String path = id.getPath();
            int units = materialCount(path);
            if (units > 0) {
                if (path.startsWith("netherite_")) return Math.min(LudkaRules.MAX_VALUE, 2048 + 128 * units);
                if (path.startsWith("diamond_")) return 128 * units;
                if (path.startsWith("iron_")) return 16 * units;
                if (path.startsWith("golden_")) return 32 * units;
            }
        }
        // Exact optional-mod profiles precede common tags; a generic tag must not flatten a machine tier.
        int profile = LudkaModValues.lookup(id.getNamespace(), id.getPath());
        if (profile > 0) return profile;
        int material = stack.getTags()
                .filter(tag -> tag.location().getNamespace().equals("c"))
                .mapToInt(tag -> LudkaModValues.commonTag(tag.location().getPath()))
                .max().orElse(0);
        if (material > 0) return material;
        int value = stack.getItem() instanceof BlockItem ? 2 : 4;
        if (stack.is(GEMS)) value = Math.max(value, 128);
        if (stack.is(INGOTS)) value = Math.max(value, 32);
        if (stack.getItem() instanceof TieredItem tool) {
            int uses = tool.getTier().getUses();
            value = Math.max(value, uses >= 2000 ? 2304 : uses >= 1400 ? 384 : uses >= 200 ? 48 : 16);
        } else if (stack.has(DataComponents.MAX_DAMAGE)) {
            value = Math.max(value, (int) Math.min(4096L, (long) stack.getMaxDamage() * 2));
        } else if (stack.has(DataComponents.TOOL)) value = Math.max(value, 64);
        // Base rarity only: custom names, stack size and player-applied enchantments cannot inflate a wager.
        int rarityFloor = switch (stack.getItem().getDefaultInstance().getRarity()) {
            case COMMON -> 1;
            case UNCOMMON -> 32;
            case RARE -> 256;
            case EPIC -> 2048;
        };
        return Math.clamp(Math.max(value, rarityFloor), 1, LudkaRules.MAX_VALUE);
    }
    private static int materialCount(String path) {
        if (path.endsWith("chestplate")) return 8;
        if (path.endsWith("leggings")) return 7;
        if (path.endsWith("helmet")) return 5;
        if (path.endsWith("boots")) return 4;
        if (path.endsWith("pickaxe") || path.endsWith("axe")) return 3;
        if (path.endsWith("sword") || path.endsWith("hoe")) return 2;
        if (path.endsWith("shovel")) return 1;
        return 0;
    }
    public static int stake(ItemStack stack) {
        int value = base(stack);
        if (value > 0 && stack.isDamageableItem()) {
            int durability = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
            value = Math.max(1, (int) ((long) value * durability / Math.max(1, stack.getMaxDamage())));
        }
        return value; // ONE item, never the entire stack or contents of a container.
    }
}
