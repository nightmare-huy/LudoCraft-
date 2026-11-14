package ru.ludocraft.gametest.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.ludocraft.LudoCraft;

/** Dev-only reference/anchor comparison, loading temporary packs through Punchy's real loader.
 * Does not modify the shipped compatibility resource. All test code is excluded from the JAR. */
public final class PunchyPoseSweep {
    private static final String[] CASES = {"vanilla-reference", "released", "offset-x", "offset-y", "offset-z"};
    private static int phase, ticks;
    private static boolean loading, ready, capture;
    private static com.google.gson.JsonObject baselineRight, baselineLeft;
    private PunchyPoseSweep() {}
    public static boolean enabled() { return Boolean.getBoolean("ludocraft.punchySweep"); }
    public static void tick(Minecraft mc) {
        if (phase >= CASES.length) return;
        mc.player.setYRot(0); mc.player.yRotO = 0;
        mc.player.setXRot(0); mc.player.xRotO = 0;
        mc.player.yHeadRot = 0; mc.player.yHeadRotO = 0;
        if (!loading) {
            loading = true;
            try {
                if (baselineRight == null) {
                    baselineRight = profile(mc, false);
                    baselineLeft = profile(mc, true);
                }
                var right = baselineRight.deepCopy();
                var left = baselineLeft.deepCopy();
                if (CASES[phase].startsWith("offset-")) {
                    String field = "off" + CASES[phase].substring(7).toUpperCase(java.util.Locale.ROOT);
                    right.addProperty(field, .25F);
                    left.addProperty(field, field.equals("offX") ? -.25F : .25F);
                }
                var sides = new com.google.gson.JsonObject(); sides.add("right", right); sides.add("left", left);
                var entry = new com.google.gson.JsonObject(); entry.addProperty("kind", "PICKAXE"); entry.add("transforms", sides);
                var specifics = new com.google.gson.JsonObject(); specifics.add("ludocraft:gamblers_pickaxe", entry);
                var json = new com.google.gson.JsonObject(); json.add("itemSpecific", specifics);
                Path root = Path.of("resourcepacks", "ludka-punchy-candidate-" + CASES[phase]);
                Path path = root.resolve("assets/minecraft/punchy/compat/ludocraft_candidate.json");
                Files.createDirectories(path.getParent());
                Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":34,\"description\":\"LudoCraft dev-only grip comparison\"}}");
                Files.writeString(path, json.toString());
            } catch (ReflectiveOperationException | java.io.IOException failure) { throw new RuntimeException(failure); }
            var repository = mc.getResourcePackRepository();
            repository.reload();
            var ids = new ArrayList<>(repository.getSelectedIds());
            ids.removeIf(id -> id.startsWith("file/ludka-punchy-candidate-"));
            String selected = "file/ludka-punchy-candidate-" + CASES[phase];
            if (repository.getPack(selected) == null) throw new AssertionError("Missing candidate pack " + selected);
            ids.add(selected);
            repository.setSelected(ids);
            mc.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(phase == 0 ? Items.DIAMOND_PICKAXE : LudoCraft.GAMBLERS_PICKAXE.get()));
            mc.reloadResourcePacks().thenRun(() -> mc.execute(() -> ready = true));
        }
        if (ready && ++ticks == 80) capture = true;
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static com.google.gson.JsonObject profile(Minecraft mc, boolean left) throws ReflectiveOperationException {
        Class kind = Class.forName("punchy.client.renderer.layer.VanillaFirstPersonItemLayer$ToolKind");
        Object pickaxe = Enum.valueOf(kind, "PICKAXE");
        Object profile = Class.forName("punchy.client.render.ToolTuning").getMethod("resolveProfile",
                ItemStack.class, kind, boolean.class).invoke(null, mc.player.getMainHandItem(), pickaxe, left);
        var json = new com.google.gson.JsonObject();
        for (String field : new String[]{"scale", "offX", "offY", "offZ", "rotX", "rotY", "rotZ", "pivotX", "pivotY", "pivotZ"}) {
            json.addProperty(field, profile.getClass().getField(field).getFloat(profile));
        }
        return json;
    }
    public static void verifyLoadedCompatibility(Minecraft mc) throws Exception {
        var resource = mc.getResourceManager().getResource(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                "minecraft", "punchy/compat/ludocraft_gamblers_pickaxe.json")).orElseThrow();
        com.google.gson.JsonObject expected;
        try (var reader = resource.openAsReader()) {
            expected = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject()
                    .getAsJsonObject("itemSpecific").getAsJsonObject("ludocraft:gamblers_pickaxe").getAsJsonObject("transforms");
        }
        for (boolean left : new boolean[]{false, true}) {
            var actual = profile(mc, left);
            var pose = expected.getAsJsonObject(left ? "left" : "right");
            for (String key : pose.keySet()) {
                if (Math.abs(pose.get(key).getAsFloat() - actual.get(key).getAsFloat()) > .0001F)
                    throw new AssertionError("Punchy did not apply shipped " + key + ": " + actual);
            }
        }
        var method = Class.forName("punchy.client.render.ToolTuning").getMethod("hasSpecificProfile", ItemStack.class);
        if (!(Boolean) method.invoke(null, mc.player.getMainHandItem()) || (Boolean) method.invoke(null, new ItemStack(Items.DIAMOND_PICKAXE)))
            throw new AssertionError("Wrong item-specific profile scope");
        System.out.println("PUNCHY_COMPAT_PASS: shipped JSON applied to both hands, vanilla diamond pickaxe unaffected");
    }
    public static void frame(Minecraft mc) throws Exception {
        if (!capture || mc.getOverlay() != null) return;
        capture = false;
        if (!mc.player.getMainHandItem().is(phase == 0 ? Items.DIAMOND_PICKAXE : LudoCraft.GAMBLERS_PICKAXE.get()))
            throw new AssertionError("Wrong held item in reference/candidate capture");
        Path path = Path.of("screenshots", "punchy-grip-" + CASES[phase] + ".png");
        Files.createDirectories(path.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(path); }
        phase++; ticks = 0; loading = false; ready = false;
        if (phase == CASES.length) {
            System.out.println("PUNCHY_SWEEP_PASS: vanilla reference and grip anchor probes captured; no visual approval implied");
            mc.stop();
        }
    }
}
