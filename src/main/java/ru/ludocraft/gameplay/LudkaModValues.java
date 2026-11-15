package ru.ludocraft.gameplay;

import java.util.HashMap;
import java.util.Map;

/** Optional integrations by registry ID only: never loads classes from another mod.
 * Values are balance estimates, not recipe costs. Zero means use the normal fallback.
 * Keep this class Minecraft-free so profiles can be tested without any optional mods.
 */
public final class LudkaModValues {
    private static final Map<String, Integer> MATERIALS = new HashMap<>();
    private static final Map<String, Integer> ITEMS = new HashMap<>();
    static {
        materials(4, "coal", "charcoal", "redstone", "sulfur", "salt");
        materials(8, "copper", "aluminum", "aluminium", "zinc", "fluorite", "obsidian");
        materials(16, "iron", "tin", "lead", "quartz", "certus_quartz", "silicon", "lapis", "lapis_lazuli", "ender_pearl");
        materials(24, "silver", "nickel", "osmium", "bronze", "brass");
        materials(32, "gold", "steel", "invar", "constantan", "electrum", "uranium", "lithium", "fluix");
        materials(64, "platinum", "lumium", "refined_glowstone", "conductive_alloy", "energetic_alloy", "pulsating_alloy", "soularium");
        materials(96, "emerald", "signalum", "dark_steel");
        materials(128, "diamond", "iridium", "enderium", "refined_obsidian", "vibrant_alloy");
        materials(192, "end_steel"); materials(24, "redstone_alloy", "cinnabar");
        materials(96, "ruby", "sapphire", "peridot", "cobalt", "titanium");
        materials(2048, "netherite");
        materials(4096, "allthemodium"); materials(8192, "vibranium"); materials(16384, "unobtainium");
        materials(24576, "vibranium_allthemodium_alloy");
        materials(28672, "unobtainium_allthemodium_alloy"); materials(32767, "unobtainium_vibranium_alloy");
        materials(64, "sky_steel", "sky_bronze", "sky_osmium", "entro"); materials(512, "quantum_alloy");
        // Recognize both conventions, but ONLY in the named material-oriented mods.
        for (String ns : new String[]{"alltheores", "allthemodium", "mekanism", "enderio"}) {
            MATERIALS.forEach((material, value) -> materialForms(ns, material, value));
        }
        for (String material : new String[]{"ruby", "sapphire", "peridot", "cinnabar"}) put("alltheores", MATERIALS.get(material), material);
        for (String material : new String[]{"copper", "iron", "bronze", "invar", "platinum"}) put("alltheores", MATERIALS.get(material) * 4, material + "_ore_hammer");
        for (String material : new String[]{"allthemodium", "vibranium", "unobtainium"}) {
            equipment("allthemodium", material, MATERIALS.get(material));
            put("allthemodium", scaled(MATERIALS.get(material), 3, 1), material + "_bow", material + "_crossbow", material + "_mace");
            put("allthemodium", MATERIALS.get(material), material + "_brush");
            put("allthemodium", scaled(MATERIALS.get(material), 2, 1), material + "_upgrade_smithing_template", material + "_carrot");
            put("allthemodium", scaled(MATERIALS.get(material), 8, 1), material + "_apple");
        }
        put("allthemodium", 32767, "alloy_axe", "alloy_paxel", "alloy_pick", "alloy_shovel", "alloy_sword", "alloy_trident");
        put("allthemodium", 8192, "piglich_heart"); put("allthemodium", 32767, "piglich_heart_block");
        put("allthemodium", 2048, "teleport_pad");
        mekanism(); enderIO(); ae2(); addons();
    }
    private LudkaModValues() {}
    private static void materials(int value, String... names) {
        for (String name : names) MATERIALS.put(name, value);
    }
    private static int scaled(int value, int numerator, int denominator) {
        return Math.max(1, (int) Math.min(LudkaRules.MAX_VALUE, (long) value * numerator / denominator));
    }
    private static void put(String ns, int value, String... paths) {
        for (String path : paths) ITEMS.put(ns + ":" + path, value);
    }
    public static int lookup(String namespace, String path) {
        return ITEMS.getOrDefault(namespace + ":" + path, 0);
    }
    static Map<String, Integer> profiles() {
        return Map.copyOf(ITEMS);
    }
    /** A precise c:form/material tag, not a substring match on an arbitrary item's name. */
    public static int commonTag(String path) {
        int slash = path.indexOf('/');
        if (slash < 0) return 0;
        String material = path.substring(slash + 1);
        boolean rawBlock = path.startsWith("storage_blocks/raw_");
        if (rawBlock) material = material.substring(4);
        Integer value = MATERIALS.get(material);
        if (value == null) return 0;
        return switch (path.substring(0, slash)) {
            case "ingots", "gems", "dusts", "plates", "clumps", "crystals", "shards", "ores" -> value;
            case "nuggets", "tiny_dusts" -> scaled(value, 1, 9);
            case "rods", "raw_materials" -> scaled(value, 1, 2);
            case "gears" -> scaled(value, 4, 1);
            case "storage_blocks" -> scaled(value, 9, rawBlock ? 2 : 1);
            default -> 0;
        };
    }
    private static void materialForms(String ns, String m, int v) {
        for (String form : new String[]{"ingot", "gem", "dust", "plate", "clump", "crystal", "shard"}) {
            put(ns, v, m + "_" + form, form + "_" + m);
        }
        put(ns, v, "dirty_" + m + "_dust", "dirty_dust_" + m, "powdered_" + m);
        put(ns, scaled(v, 1, 9), m + "_nugget", "nugget_" + m);
        put(ns, scaled(v, 1, 2), m + "_rod", "rod_" + m, "raw_" + m, "crushed_" + m);
        put(ns, scaled(v, 4, 1), m + "_gear", "gear_" + m);
        put(ns, scaled(v, 9, 1), m + "_block", "block_" + m);
        put(ns, scaled(v, 9, 2), "raw_" + m + "_block", "block_raw_" + m);
        for (String prefix : new String[]{"", "deepslate_", "nether_", "end_", "other_"}) put(ns, v, prefix + m + "_ore");
        put(ns, v, m + "_slate_ore");
    }
    private static void equipment(String ns, String material, int value) {
        String[] pieces = {"shovel", "sword", "hoe", "pickaxe", "axe", "helmet", "chestplate", "leggings", "boots", "paxel", "shield"};
        int[] units = {1, 2, 2, 3, 3, 5, 8, 7, 4, 6, 6};
        for (int i = 0; i < pieces.length; i++) put(ns, scaled(value, units[i], 1), material + "_" + pieces[i]);
    }
    private static void mekanism() {
        String ns = "mekanism";
        put(ns, 48, "alloy_infused", "energy_tablet"); put(ns, 192, "alloy_reinforced"); put(ns, 768, "alloy_atomic");
        put(ns, 4096, "pellet_polonium", "pellet_plutonium"); put(ns, 32767, "pellet_antimatter");
        put(ns, 16, "hdpe_pellet", "hdpe_stick"); put(ns, 48, "hdpe_rod"); put(ns, 64, "hdpe_sheet");
        put(ns, 128, "steel_casing", "electrolytic_core", "module_base");
        String[] tiers = {"basic", "advanced", "elite", "ultimate"};
        for (int i = 0; i < tiers.length; i++) {
            String t = tiers[i] + "_"; int factor = 1 << (i * 2);
            put(ns, 32 * factor, t + "control_circuit");
            for (String kind : new String[]{"smelting", "enriching", "crushing", "compressing", "combining", "purifying", "injecting", "infusing", "sawing"}) {
                put(ns, scaled(512, 1 << i, 1), t + kind + "_factory");
            }
            put(ns, 256 * (1 << i), t + "energy_cube", t + "tier_installer");
            put(ns, 64 * (1 << i), t + "bin", t + "fluid_tank", t + "chemical_tank");
            put(ns, 2048 * (1 << i), t + "induction_cell", t + "induction_provider");
            for (String pipe : new String[]{"universal_cable", "mechanical_pipe", "pressurized_tube", "logistical_transporter", "thermodynamic_conductor"}) put(ns, 16 * (1 << i), t + pipe);
        }
        put(ns, 256, "enrichment_chamber", "crusher", "energized_smelter", "metallurgic_infuser", "osmium_compressor", "precision_sawmill", "combiner", "electric_pump", "chargepad", "logistical_sorter");
        put(ns, 512, "purification_chamber", "electrolytic_separator", "rotary_condensentrator", "chemical_oxidizer", "chemical_infuser", "formulaic_assemblicator", "nutritional_liquifier", "resistive_heater", "fuelwood_heater", "fluidic_plenisher");
        put(ns, 1024, "chemical_injection_chamber", "chemical_dissolution_chamber", "chemical_washer", "chemical_crystallizer", "pressurized_reaction_chamber", "solar_neutron_activator", "isotopic_centrifuge", "pigment_extractor", "pigment_mixer", "painting_machine", "modification_station");
        put(ns, 8192, "digital_miner", "quantum_entangloporter", "dimensional_stabilizer");
        put(ns, 32767, "antiprotonic_nucleosynthesizer", "meka_tool", "mekasuit_helmet", "mekasuit_bodyarmor", "mekasuit_pants", "mekasuit_boots");
        put(ns, 4096, "atomic_disassembler", "robit", "portable_teleporter", "teleporter");
        put(ns, 1024, "teleportation_core", "jetpack", "free_runners"); put(ns, 2048, "jetpack_armored", "free_runners_armored");
        put(ns, 8192, "qio_drive_base"); put(ns, 16384, "qio_drive_hyper_dense"); put(ns, 24576, "qio_drive_time_dilating"); put(ns, 32767, "qio_drive_supermassive");
        put(ns, 2048, "qio_drive_array", "qio_dashboard", "portable_qio_dashboard", "qio_importer", "qio_exporter", "qio_redstone_adapter");
        put(ns, 64, "upgrade_speed", "upgrade_energy", "upgrade_filter", "upgrade_muffling", "upgrade_chemical");
        put(ns, 128, "induction_casing", "induction_port", "boiler_casing", "boiler_valve", "thermal_evaporation_controller");
        put(ns, 4096, "sps_casing", "sps_port", "supercharged_coil");
        ns = "mekanismgenerators";
        put(ns, 256, "heat_generator", "bio_generator"); put(ns, 512, "solar_generator", "gas_burning_generator", "wind_generator");
        put(ns, 2048, "advanced_solar_generator");
        put(ns, 512, "fission_reactor_casing", "fission_reactor_port", "fission_fuel_assembly", "control_rod_assembly", "turbine_rotor", "electromagnetic_coil");
        put(ns, 4096, "fusion_reactor_controller", "fusion_reactor_frame", "fusion_reactor_port", "laser_focus_matrix");
        for (String m : new String[]{"bronze", "osmium", "steel", "refined_glowstone", "refined_obsidian"}) equipment("mekanismtools", m, MATERIALS.get(m));
        equipment("mekanismtools", "lapis_lazuli", 16);
    }
    private static void enderIO() {
        String ns = "enderio";
        put(ns, 64, "basic_capacitor"); put(ns, 256, "double_layer_capacitor"); put(ns, 1024, "octadic_capacitor");
        put(ns, 256, "basic_capacitor_bank", "void_chassis", "stirling_generator", "wired_charger");
        put(ns, 1024, "advanced_capacitor_bank", "ensouled_chassis"); put(ns, 4096, "vibrant_capacitor_bank");
        put(ns, 512, "alloy_smelter", "sag_mill", "slice_and_splice", "painting_machine", "crafter", "vat", "enchanter", "vacuum_chest", "xp_vacuum");
        put(ns, 2048, "soul_binder", "farming_station", "soul_engine", "wireless_charger", "travel_anchor", "xp_obelisk", "energetic_photovoltaic_module");
        put(ns, 4096, "powered_spawner", "attractor_obelisk", "aversion_obelisk", "inhibitor_obelisk", "weather_obelisk", "pulsating_photovoltaic_module");
        put(ns, 8192, "vibrant_photovoltaic_module");
        put(ns, 16, "grains_of_infinity"); put(ns, 8, "conduit_binder");
        put(ns, 64, "iron_gear", "photovoltaic_plate"); put(ns, 256, "dark_bimetal_gear", "energized_gear", "ender_resonator");
        put(ns, 128, "pulsating_crystal", "prescient_crystal"); put(ns, 512, "ender_crystal", "enticing_crystal", "vibrant_crystal");
        put(ns, 64, "fluid_tank"); put(ns, 256, "pressurized_fluid_tank");
        equipment(ns, "dark_steel", 96); equipment(ns, "end_steel", 192);
        // Conduit variants share a component-bearing ID in 1.21.1: do not invent IDs or price their data.
    }
    private static void storage(String ns, String unit, int[] values, String... resources) {
        int[] sizes = {1, 4, 16, 64, 256};
        for (int i = 0; i < sizes.length; i++) {
            String size = sizes[i] + unit;
            put(ns, values[i], "cell_component_" + size);
            put(ns, scaled(values[i] + 256, 1, 1), size + "_crafting_storage");
            for (String resource : resources) {
                put(ns, scaled(values[i] + 128, 1, 1), resource + "_storage_cell_" + size);
                put(ns, scaled(values[i] + 512, 1, 1), "portable_" + resource + "_cell_" + size);
            }
        }
    }
    private static void ae2() {
        String ns = "ae2";
        put(ns, 16, "certus_quartz_crystal", "certus_quartz_dust", "silicon", "ender_dust");
        put(ns, 24, "charged_certus_quartz_crystal"); put(ns, 32, "fluix_crystal", "fluix_dust");
        put(ns, 128, "fluix_block", "fluix_pearl");
        put(ns, 64, "printed_logic_processor", "printed_calculation_processor"); put(ns, 160, "printed_engineering_processor");
        put(ns, 96, "logic_processor", "calculation_processor"); put(ns, 192, "engineering_processor");
        put(ns, 256, "logic_processor_press", "calculation_processor_press", "engineering_processor_press", "silicon_press");
        put(ns, 16, "printed_silicon"); put(ns, 64, "formation_core", "annihilation_core", "blank_pattern");
        put(ns, 256, "charger", "inscriber", "energy_acceptor", "cell_workbench", "energy_cell", "crafting_unit");
        put(ns, 512, "drive", "chest", "molecular_assembler", "pattern_provider", "cable_pattern_provider", "interface", "cable_interface", "io_port", "crafting_accelerator", "crafting_monitor", "condenser");
        put(ns, 1024, "controller"); put(ns, 2048, "dense_energy_cell");
        put(ns, 8192, "singularity", "quantum_entangled_singularity");
        put(ns, 2048, "quantum_ring", "quantum_link", "spatial_io_port", "spatial_anchor");
        put(ns, 128, "item_cell_housing", "fluid_cell_housing", "basic_card", "terminal");
        put(ns, 256, "advanced_card", "crafting_terminal", "import_bus", "export_bus", "storage_bus", "formation_plane", "annihilation_plane");
        put(ns, 512, "pattern_encoding_terminal", "pattern_access_terminal");
        put(ns, 1024, "wireless_terminal", "wireless_crafting_terminal", "wireless_access_point", "wireless_receiver");
        for (String card : new String[]{"capacity", "crafting", "redstone", "fuzzy", "inverter", "speed", "energy", "void", "equal_distribution"}) put(ns, 256, card + "_card");
        for (String resource : new String[]{"me", "item", "fluid", "fe", "light", "redstone"}) put(ns, 256, resource + "_p2p_tunnel");
        for (String color : new String[]{"fluix", "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"}) {
            put(ns, 8, color + "_glass_cable"); put(ns, 16, color + "_covered_cable"); put(ns, 32, color + "_smart_cable");
            put(ns, 64, color + "_covered_dense_cable"); put(ns, 128, color + "_smart_dense_cable");
        }
        int[] values = {256, 512, 1024, 2048, 4096};
        storage(ns, "k", values, "item", "fluid"); storage("appmek", "k", values, "chemical");
        put("appmek", 128, "chemical_cell_housing"); put("appmek", 256, "chemical_p2p_tunnel");
        int[] spatialSizes = {2, 16, 128};
        for (int i = 0; i < 3; i++) {
            put(ns, 1024 << i, "spatial_cell_component_" + spatialSizes[i]);
            put(ns, (1024 << i) + 128, "spatial_storage_cell_" + spatialSizes[i]);
        }
        equipment(ns, "certus_quartz", 16); equipment(ns, "fluix", 32);
    }
    private static void addons() {
        String ns = "megacells";
        storage(ns, "m", new int[]{8192, 12288, 18432, 24576, 30000}, "item", "fluid", "chemical", "experience", "mana", "soul", "source");
        for (String m : new String[]{"sky_steel", "sky_bronze", "sky_osmium"}) materialForms(ns, m, 64);
        put(ns, 384, "accumulation_processor"); put(ns, 256, "printed_accumulation_processor", "accumulation_processor_press");
        put(ns, 2048, "mega_interface", "cable_mega_interface", "mega_pattern_provider", "cable_mega_pattern_provider", "mega_crafting_unit", "mega_crafting_monitor", "mega_crafting_accelerator");
        put(ns, 8192, "mega_energy_cell", "bulk_cell_component", "radioactive_cell_component");
        put(ns, 8448, "bulk_item_cell", "radioactive_chemical_cell");
        for (String r : new String[]{"item", "fluid", "chemical", "experience", "mana", "soul", "source"}) put(ns, 128, "mega_" + r + "_cell_housing");
        put(ns, 512, "compression_card", "greater_energy_card", "cell_dock", "portable_cell_workbench", "decompression_module");
        ns = "extendedae";
        materialForms(ns, "entro", 64); put(ns, 32, "entro_shard", "entro_seed");
        put(ns, 256, "concurrent_processor"); put(ns, 128, "concurrent_processor_print", "concurrent_processor_press");
        put(ns, 512, "machine_frame", "ex_charger", "circuit_cutter", "crystal_assembler", "crystal_fixer", "caner", "ingredient_buffer", "void_cell");
        // These are legitimate survival cobblestone/water generators, NOT creative cells.
        put(ns, 2048, "infinity_cobblestone_cell", "infinity_water_cell");
        put(ns, 2048, "ex_drive", "ex_inscriber", "ex_interface", "ex_interface_part", "ex_pattern_provider", "ex_pattern_provider_part", "ex_molecular_assembler", "ex_io_port", "oversize_interface", "oversize_interface_part", "wireless_connect", "wireless_hub", "wireless_ex_pat", "wireless_tool");
        put(ns, 1024, "assembler_matrix_frame", "assembler_matrix_wall", "assembler_matrix_glass");
        put(ns, 4096, "assembler_matrix_crafter", "assembler_matrix_pattern", "assembler_matrix_speed");
        for (String type : new String[]{"drive", "interface", "pattern_provider", "io_bus", "pattern_terminal", "wireless_connector"}) put(ns, 512, type + "_upgrade");
        for (String type : new String[]{"ex_export_bus_part", "ex_import_bus_part", "ex_pattern_access_part", "precise_export_bus", "precise_storage_bus", "tag_export_bus", "tag_storage_bus", "mod_export_bus", "mod_storage_bus", "threshold_export_bus", "threshold_level_emitter", "smart_annihilation_plane", "active_formation_plane"}) put(ns, 512, type);
        ns = "advanced_ae";
        put(ns, 512, "quantum_alloy", "quantum_alloy_plate", "quantum_infused_dust", "printed_quantum_processor", "quantum_processor_press");
        put(ns, 4608, "quantum_alloy_block"); put(ns, 1024, "quantum_processor", "quantum_upgrade_base");
        put(ns, 8192, "quantum_core", "quantum_crafter", "quantum_multi_threader", "data_entangler", "quantum_storage_component");
        put(ns, 4096, "quantum_unit", "quantum_accelerator", "reaction_chamber", "adv_pattern_provider", "adv_pattern_provider_part", "quantum_crafter_terminal", "wireless_quantum_crafter_terminal");
        put(ns, 1024, "quantum_structure", "adv_pattern_provider_upgrade", "adv_pattern_provider_capacity_upgrade");
        put(ns, 2048, "small_adv_pattern_provider", "small_adv_pattern_provider_part", "shattered_singularity");
        put(ns, 16384, "quantum_storage_128"); put(ns, 24576, "quantum_storage_256");
        put(ns, 32767, "quantum_helmet", "quantum_chestplate", "quantum_leggings", "quantum_boots");
        for (String card : new String[]{"attack_speed", "auto_feeding", "auto_stock", "camo", "evasion", "flight", "flight_drift", "hp_buffer", "jump_height", "lava_immunity", "luck", "magnet", "night_vision", "pick_craft", "portable_workbench", "reach", "recharging", "regeneration", "sprint_speed", "step_assist", "strength", "swim_speed", "walk_speed", "water_breathing"}) put(ns, 2048, card + "_card");
    }
}
