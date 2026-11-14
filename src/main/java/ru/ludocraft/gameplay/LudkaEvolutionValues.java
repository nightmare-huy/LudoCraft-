package ru.ludocraft.gameplay;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Opt-in conservative first pass for FTB Evolution 1.47.0 (pack 125/version 100598).
 * Manual policy informed by pinned pack scripts; NOT a recipe solver or stage tracker.
 * Zero = use the generic profile. No Minecraft or optional-mod class dependencies.
 */
public final class LudkaEvolutionValues {
    private static final Map<String, Integer> ITEMS = new HashMap<>();
    private static final Map<String, Integer> MATERIALS = Map.ofEntries(
            Map.entry("iron", 8), Map.entry("gold", 16), Map.entry("copper", 4),
            Map.entry("diamond", 32), Map.entry("emerald", 16), Map.entry("netherite", 512),
            Map.entry("tin", 8), Map.entry("lead", 8), Map.entry("osmium", 12),
            Map.entry("silver", 12), Map.entry("nickel", 12), Map.entry("zinc", 4),
            Map.entry("aluminum", 4), Map.entry("steel", 16), Map.entry("brass", 12),
            Map.entry("bronze", 12), Map.entry("invar", 16), Map.entry("electrum", 16),
            Map.entry("constantan", 16), Map.entry("uranium", 16), Map.entry("platinum", 32),
            Map.entry("quartz", 8), Map.entry("certus_quartz", 8), Map.entry("silicon", 4),
            Map.entry("fluix", 16), Map.entry("coal", 4), Map.entry("charcoal", 4),
            Map.entry("redstone", 4), Map.entry("lapis_lazuli", 8), Map.entry("fluorite", 4));
    private static final Set<String> CUSTOM_RESOURCES = Set.of("ruby_gem", "xy_aluminum_dust");
    static {
        MATERIALS.forEach((material, value) -> {
            // FTB Materials' actual raw/ore naming differs from AllTheOres.
            for (String ns : new String[]{"ftbmaterials", "alltheores", "mekanism", "modern_industrialization"}) {
                for (String form : new String[]{"ingot", "gem", "dust", "plate", "clump", "crystal", "shard", "dirty_dust"}) {
                    put(ns, value, material + "_" + form, form + "_" + material);
                }
                put(ns, Math.max(1, value / 9), material + "_nugget", "nugget_" + material, material + "_tiny_dust");
                put(ns, Math.max(1, value / 2), material + "_rod", "rod_" + material, material + "_raw_ore", "raw_" + material);
                put(ns, value * 4, material + "_gear");
                put(ns, value * 9, material + "_block", "block_" + material);
                put(ns, value * 9 / 2, material + "_raw_block", "raw_" + material + "_block", "block_raw_" + material);
                for (String ore : new String[]{"stone_ore", "deepslate_ore", "nether_ore", "end_ore"}) put(ns, value, material + "_" + ore);
                put(ns, value, material + "_ore", "deepslate_" + material + "_ore");
            }
            put("minecraft", value, material + "_ingot");
            put("minecraft", value * 9, material + "_block");
            put("minecraft", Math.max(1, value / 9), material + "_nugget");
            put("minecraft", Math.max(1, value / 2), "raw_" + material);
            put("minecraft", value * 9 / 2, "raw_" + material + "_block");
        });
        put("minecraft", 32, "diamond", "blaze_rod", "totem_of_undying");
        put("minecraft", 16, "emerald", "bow", "shield", "flint_and_steel", "shears");
        put("minecraft", 8, "quartz", "lapis_lazuli", "fishing_rod", "ender_pearl");
        put("minecraft", 32, "crossbow"); put("minecraft", 512, "nether_star", "trident");
        put("minecraft", 1024, "beacon"); put("minecraft", 4096, "dragon_egg", "elytra");
        put("minecraft", 128, "netherite_scrap", "ancient_debris");
        put("ae2", 4, "silicon", "printed_silicon");
        put("ae2", 8, "certus_quartz_crystal", "certus_quartz_dust");
        put("ae2", 12, "charged_certus_quartz_crystal");
        put("ae2", 16, "fluix_crystal", "fluix_dust"); put("ae2", 64, "fluix_block");
        put("ae2", 1024, "drive"); put("ae2", 2048, "controller");
        // Triple-component progression, then the existing hard price ceiling.
        int[] sizes = {1, 4, 16, 64, 256}, components = {512, 1536, 4608, 13824, 32767};
        for (int i = 0; i < sizes.length; i++) {
            put("ae2", components[i], "cell_component_" + sizes[i] + "k");
            put("ae2", cap(components[i] + 256), sizes[i] + "k_crafting_storage");
            for (String resource : new String[]{"item", "fluid", "chemical"}) {
                String ns = resource.equals("chemical") ? "appmek" : "ae2";
                put(ns, cap(components[i] + 128), resource + "_storage_cell_" + sizes[i] + "k");
                put(ns, cap(components[i] + 512), "portable_" + resource + "_cell_" + sizes[i] + "k");
            }
        }
        // Mega cells must never be cheaper than the preceding AE2 256k tier.
        // All reach the existing ceiling: this profile does not pretend to model their full recipe cost.
        for (int size : sizes) {
            put("megacells", 32767, "cell_component_" + size + "m", size + "m_crafting_storage");
            for (String resource : new String[]{"item", "fluid", "chemical", "experience", "mana", "soul", "source"}) {
                put("megacells", 32767, resource + "_storage_cell_" + size + "m", "portable_" + resource + "_cell_" + size + "m");
            }
        }
        // Same-value aliases for 1:1 stonecutter conversions in general_additions.js.
        put("immersiveengineering", 144, "storage_steel");
        put("create", 108, "brass_block"); put("create", 36, "zinc_block"); put("create", 18, "raw_zinc_block");
        put("createpropulsion", 288, "platinum_block");
        put("supplementaries", 36, "sugar_cube", "feather_block");
        put("mekanism", 36, "block_charcoal");
        put("ftbevolution", 32, "ruby_gem"); put("ftbevolution", 4, "xy_aluminum_dust");
        put("actuallyadditions", 1, "tiny_coal", "tiny_charcoal");
        put("mekanism", 1, "bio_fuel", "sawdust"); put("mekanism", 9, "block_bio_fuel");
    }
    private LudkaEvolutionValues() {}
    private static int cap(int v) { return Math.min(LudkaRules.MAX_VALUE, v); }
    private static void put(String ns, int value, String... paths) {
        for (String path : paths) ITEMS.put(ns + ":" + path, value);
    }
    public static int lookup(String ns, String path) {
        Integer exact = ITEMS.get(ns + ":" + path);
        if (exact != null) return exact;
        // Avoid pricing an unreviewed custom epic item as 2048 merely because its name is purple.
        if (ns.equals("ftbevolution")) return 4;
        if (ns.equals("minecraft")) {
            int units = equipmentUnits(path);
            if (units > 0) {
                if (path.startsWith("netherite_")) return 512 + 32 * units;
                if (path.startsWith("diamond_")) return 32 * units;
                if (path.startsWith("iron_")) return 8 * units;
                if (path.startsWith("golden_")) return 16 * units;
            }
        }
        return 0;
    }
    private static int equipmentUnits(String path) {
        if (path.endsWith("_chestplate")) return 8;
        if (path.endsWith("_leggings")) return 7;
        if (path.endsWith("_helmet")) return 5;
        if (path.endsWith("_boots")) return 4;
        if (path.endsWith("_pickaxe") || path.endsWith("_axe")) return 3;
        if (path.endsWith("_sword") || path.endsWith("_hoe")) return 2;
        if (path.endsWith("_shovel")) return 1;
        return 0;
    }
    public static int commonTag(String path) {
        int slash = path.indexOf('/');
        if (slash < 0) return 0;
        String form = path.substring(0, slash), material = path.substring(slash + 1);
        boolean raw = form.equals("storage_blocks") && material.startsWith("raw_");
        if (raw) material = material.substring(4);
        Integer v = MATERIALS.get(material);
        if (v == null) return 0;
        return switch (form) {
            case "ingots", "gems", "dusts", "plates", "clumps", "crystals", "shards", "ores" -> v;
            case "nuggets", "tiny_dusts" -> Math.max(1, v / 9);
            case "rods", "raw_materials" -> Math.max(1, v / 2);
            case "gears" -> v * 4;
            case "storage_blocks" -> v * 9 / (raw ? 2 : 1);
            default -> 0;
        };
    }
    /** Dynamic prefix checks are deliberately scoped to known pack namespaces. */
    public static boolean blocksTarget(String ns, String path) {
        if (ns.equals("ftbevolution")) return !CUSTOM_RESOURCES.contains(path);
        if (ns.equals("compressedblocks")) return true; // unknown decompression yield: never award at fallback price
        if (ns.equals("draconicevolution") && (path.contains("chaos") || path.startsWith("creative_"))) return true;
        if (ns.equals("mekanism") && Set.of("pellet_antimatter", "antiprotonic_nucleosynthesizer").contains(path)) return true;
        // Do not buy a resource generator or creative machine for an unreviewed default quote.
        return path.startsWith("creative_") || path.endsWith("_creative");
    }
    /** Unreviewed compressed blocks can be stakes, but not an expensive currency. */
    public static int fallback(int value, boolean stake) {
        return stake ? Math.min(value, 32) : Math.max(value, 512);
    }
}
