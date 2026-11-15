package ru.ludocraft.gameplay;

public final class LudkaModValuesTest {
    private LudkaModValuesTest() {}
    public static void run() {
        exactProfiles(); commonTags(); monotonicTiers(); unknownIdsStayUnknown();
        for (var entry : LudkaModValues.profiles().entrySet()) {
            require(entry.getValue() > 0 && entry.getValue() <= LudkaRules.MAX_VALUE, "Out-of-bounds profile " + entry);
            require(entry.getKey().matches("[a-z0-9_]+:[a-z0-9_/]+"), "Invalid profile ID " + entry);
            int chance = LudkaRules.chance(1, entry.getValue());
            require(chance > 0 && chance <= LudkaRules.PRECISION, "Unreachable target " + entry);
        }
        System.out.println("PASS: optional-mod profiles, common material forms, bounded prices, storage/machine progression");
    }
    private static int value(String id) {
        String[] parts = id.split(":", 2);
        return LudkaModValues.lookup(parts[0], parts[1]);
    }
    private static void exactProfiles() {
        String[] ids = {
            "alltheores:tin_ingot", "alltheores:tin_nugget", "alltheores:tin_block", "alltheores:tin_gear",
            "alltheores:raw_tin", "alltheores:dirty_tin_dust", "alltheores:deepslate_tin_ore", "alltheores:ruby",
            "allthemodium:allthemodium_ingot", "allthemodium:vibranium_ingot", "allthemodium:unobtainium_ingot",
            "allthemodium:allthemodium_pickaxe", "allthemodium:unobtainium_block", "allthemodium:alloy_paxel",
            "mekanism:ingot_osmium", "mekanism:block_osmium", "mekanism:pellet_antimatter", "mekanism:digital_miner",
            "mekanism:mekasuit_bodyarmor", "mekanismgenerators:advanced_solar_generator", "mekanismtools:steel_pickaxe",
            "enderio:vibrant_alloy_ingot", "enderio:octadic_capacitor", "enderio:powered_spawner",
            "ae2:engineering_processor", "ae2:controller", "ae2:cell_component_1k", "ae2:item_storage_cell_1k",
            "ae2:fluid_storage_cell_256k", "ae2:16k_crafting_storage", "ae2:spatial_storage_cell_128",
            "appmek:chemical_storage_cell_64k", "appmek:portable_chemical_cell_64k",
            "megacells:item_storage_cell_1m", "megacells:chemical_storage_cell_256m", "megacells:sky_steel_block",
            "extendedae:concurrent_processor", "extendedae:ex_pattern_provider", "extendedae:infinity_cobblestone_cell",
            "advanced_ae:quantum_processor", "advanced_ae:quantum_storage_256", "advanced_ae:quantum_chestplate"
        };
        int[] expected = {16, 1, 144, 64, 8, 16, 16, 96, 4096, 8192, 16384, 12288, 32767, 32767,
            24, 216, 32767, 8192, 32767, 2048, 96, 128, 1024, 4096, 192, 1024, 256, 384, 4224, 1280, 4224,
            2176, 2560, 8320, 30128, 576, 256, 2048, 2048, 1024, 24576, 32767};
        require(ids.length == expected.length, "Test fixture mismatch");
        for (int i = 0; i < ids.length; i++) equals(expected[i], value(ids[i]), ids[i]);
        // Dirt -> allthemodium = 0.0122%; diamond -> 1k cell = 16.6666%.
        equals(122, LudkaRules.chance(1, value("allthemodium:allthemodium_ingot")), "Rare metal odds");
        equals(166666, LudkaRules.chance(128, value("ae2:item_storage_cell_1k")), "Storage odds");
    }
    private static void commonTags() {
        String[] tags = {"ingots/tin", "dusts/tin", "nuggets/tin", "plates/tin", "rods/tin", "gears/tin",
            "ores/tin", "raw_materials/tin", "storage_blocks/tin", "storage_blocks/raw_tin", "gems/ruby",
            "storage_blocks/unobtainium", "ingots/netherite", "nuggets/gold", "raw_materials/copper"};
        int[] prices = {16, 16, 1, 16, 8, 64, 16, 8, 144, 72, 96, 32767, 2048, 3, 4};
        for (int i = 0; i < tags.length; i++) equals(prices[i], LudkaModValues.commonTag(tags[i]), tags[i]);
        for (String tag : new String[]{"ingots", "ores", "ingots/missing", "dusts/tin/extra", "seeds/tin", "storage_blocks/raw_unknown", "arbitrary_unobtainium"}) {
            equals(0, LudkaModValues.commonTag(tag), tag);
        }
    }
    private static void monotonicTiers() {
        increasing("allthemodium:", "allthemodium_ingot", "vibranium_ingot", "unobtainium_ingot");
        increasing("mekanism:", "alloy_infused", "alloy_reinforced", "alloy_atomic");
        increasing("enderio:", "basic_capacitor", "double_layer_capacitor", "octadic_capacitor");
        for (String kind : new String[]{"control_circuit", "energy_cube", "induction_cell", "smelting_factory", "chemical_tank", "universal_cable"}) {
            increasing("mekanism:", "basic_" + kind, "advanced_" + kind, "elite_" + kind, "ultimate_" + kind);
        }
        for (String ns : new String[]{"ae2", "appmek", "megacells"}) {
            String unit = ns.equals("megacells") ? "m" : "k";
            String resource = ns.equals("appmek") ? "chemical" : "item";
            String stem = resource + "_storage_cell_";
            increasing(ns + ":", stem + "1" + unit, stem + "4" + unit, stem + "16" + unit, stem + "64" + unit, stem + "256" + unit);
            for (int size : new int[]{1, 4, 16, 64, 256}) {
                if (!ns.equals("appmek")) require(value(ns + ":cell_component_" + size + unit) < value(ns + ":" + stem + size + unit), "Housing adds no value");
            }
        }
        require(value("megacells:item_storage_cell_1m") > value("ae2:item_storage_cell_256k"), "Mega progression inverted");
    }
    private static void unknownIdsStayUnknown() {
        for (String id : new String[]{"randommod:controller", "randommod:unobtainium_ingot", "minecraft:controller",
                "ae2:cell_component_9999999999999999999k", "ae2:item_storage_cell_2k", "ae2:creative_storage_cell",
                "mekanism:creative_energy_cube", "ender_io:alloy_smelter", "ae2:stone", "advanced_ae:quantum_fake"}) equals(0, value(id), id);
    }
    private static void increasing(String ns, String... paths) {
        int previous = 0;
        for (String path : paths) {
            int current = value(ns + path);
            require(current > previous, "Non-increasing tier " + ns + path);
            previous = current;
        }
    }
    private static void equals(int expected, int actual, String message) {
        require(expected == actual, message + ": expected " + expected + ", got " + actual);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
