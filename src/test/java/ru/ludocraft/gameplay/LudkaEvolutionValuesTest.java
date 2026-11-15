package ru.ludocraft.gameplay;

public final class LudkaEvolutionValuesTest {
    private LudkaEvolutionValuesTest() {}
    public static void run() {
        price("minecraft:diamond", 32); price("minecraft:diamond_block", 288);
        price("minecraft:bow", 16); price("minecraft:netherite_ingot", 512);
        price("ftbmaterials:netherite_dust", 512); price("mekanism:dust_netherite", 512);
        price("ftbmaterials:osmium_ingot", 12); price("ftbmaterials:osmium_raw_ore", 6);
        price("ftbmaterials:osmium_raw_block", 54);
        price("ftbmaterials:steel_block", 144); price("immersiveengineering:storage_steel", 144);
        price("ftbmaterials:brass_block", 108); price("create:brass_block", 108);
        price("ftbmaterials:zinc_raw_block", 18); price("create:raw_zinc_block", 18);
        price("ftbevolution:ultimate_singularity", 4); // neither epic-name inflation nor a legal prize
        price("ae2:item_storage_cell_1k", 640); price("ae2:item_storage_cell_64k", 13952);
        price("appmek:chemical_storage_cell_64k", 13952);
        price("ae2:item_storage_cell_256k", 32767);
        price("megacells:item_storage_cell_1m", 32767);
        price("megacells:cell_component_256m", 32767);
        price("randommod:diamond_block", 0); price("minecraft:iron_nugget", 1);
        require(LudkaEvolutionValues.commonTag("storage_blocks/diamond") == 288, "Cross-mod blocks disagree");
        require(LudkaEvolutionValues.commonTag("ingots/osmium") == 12, "Cross-mod unified metal disagrees");
        require(LudkaEvolutionValues.commonTag("storage_blocks/raw_osmium") == 54, "Raw conversion disagrees");
        require(LudkaEvolutionValues.commonTag("ingots/unknown") == 0, "Unknown tag overmatch");
        require(LudkaEvolutionValues.commonTag("ingots") == 0, "Generic tag overmatch");
        for (String id : new String[]{"ftbevolution:prediction_amalgam", "ftbevolution:creative_ascension",
                "ftbevolution:chaos_induced_shards", "ftbevolution:new_unreviewed_component", "compressedblocks:c9_diamond",
                "draconicevolution:medium_chaos_frag", "mekanism:pellet_antimatter", "mekanism:antiprotonic_nucleosynthesizer"}) {
            String[] s = id.split(":"); require(LudkaEvolutionValues.blocksTarget(s[0], s[1]), "Progression shortcut " + id);
        }
        require(!LudkaEvolutionValues.blocksTarget("ftbevolution", "ruby_gem"), "Reviewed custom material blocked");
        require(!LudkaEvolutionValues.blocksTarget("ae2", "controller"), "Normal machine blocked");
        require(!LudkaEvolutionValues.blocksTarget("randommod", "chaos_flower"), "Unrelated namespace matched");
        // Resource stakes now buy fewer high-tech prizes, while normal material ratios remain coherent.
        require(LudkaRules.chance(32, 2048) == 7812, "Diamond -> controller quote");
        require(LudkaRules.chance(32, 640) == 25000, "Diamond -> 1k cell quote");
        require(LudkaRules.chance(32, 512) == 31250, "Diamond -> netherite ratio");
        for (int v = 1; v <= LudkaRules.MAX_VALUE; v++) {
            require(LudkaEvolutionValues.fallback(v, true) <= 32, "Fallback wager inflation");
            require(LudkaEvolutionValues.fallback(v, false) >= 512, "Unreviewed target undervalued");
        }
        System.out.println("PASS: Evolution 1.47.0 unified forms, conversion aliases, target protections and conservative fallback");
    }
    private static void price(String id, int expected) {
        String[] parts = id.split(":"); int actual = LudkaEvolutionValues.lookup(parts[0], parts[1]);
        require(actual == expected, id + ": expected " + expected + ", got " + actual);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
