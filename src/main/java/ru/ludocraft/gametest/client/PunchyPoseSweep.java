package ru.ludocraft.gametest.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/** Development-only resource-pack sweep; tests Punchy's actual JSON loader, not an emulated renderer. */
public final class PunchyPoseSweep {
    private static final String[] ANGLES = {"x-90", "x-45", "x0", "x45", "x90", "y-90", "y-45", "y0", "y45", "y90"};
    private static int phase, ticks;
    private static boolean loading, ready, capture;
    private static com.google.gson.JsonObject baselineRight, baselineLeft;
    private PunchyPoseSweep() {}
    public static boolean enabled() { return Boolean.getBoolean("ludocraft.punchySweep"); }
    public static void tick(Minecraft mc) {
        if (phase >= ANGLES.length) return;
        mc.player.setYRot(0); mc.player.yRotO = 0;
        mc.player.setXRot(0); mc.player.xRotO = 0;
        mc.player.yHeadRot = 0; mc.player.yHeadRotO = 0;
        if (!loading) {
            loading = true;
            try {
                if (baselineRight == null) {
                    baselineRight = profile(mc, false);
                    baselineLeft = profile(mc, true);
                    Files.writeString(Path.of("punchy-profiles.txt"), "right=" + baselineRight + "\nleft=" + baselineLeft);
                }
                String field = "rot" + ANGLES[phase].substring(0, 1).toUpperCase(java.util.Locale.ROOT);
                int angle = Integer.parseInt(ANGLES[phase].substring(1));
                var right = baselineRight.deepCopy(); right.addProperty(field, angle);
                var left = baselineLeft.deepCopy(); left.addProperty(field, field.equals("rotX") ? angle : -angle);
                var sides = new com.google.gson.JsonObject(); sides.add("right", right); sides.add("left", left);
                var entry = new com.google.gson.JsonObject(); entry.addProperty("kind", "PICKAXE"); entry.add("transforms", sides);
                var specifics = new com.google.gson.JsonObject(); specifics.add("ludocraft:gamblers_pickaxe", entry);
                var json = new com.google.gson.JsonObject(); json.add("itemSpecific", specifics);
                Path path = Path.of("resourcepacks", "ludka-punchy-candidate-" + ANGLES[phase],
                        "assets/minecraft/punchy/compat/ludocraft_candidate.json");
                Files.writeString(path, json.toString());
            } catch (ReflectiveOperationException | java.io.IOException failure) { throw new RuntimeException(failure); }
            var repository = mc.getResourcePackRepository();
            repository.reload();
            var ids = new ArrayList<>(repository.getSelectedIds());
            ids.removeIf(id -> id.startsWith("file/ludka-punchy-candidate-"));
            String selected = "file/ludka-punchy-candidate-" + ANGLES[phase];
            if (repository.getPack(selected) == null) throw new AssertionError("Missing candidate pack " + selected);
            ids.add(selected);
            repository.setSelected(ids);
            mc.reloadResourcePacks().thenRun(() -> mc.execute(() -> ready = true));
        }
        if (ready && ++ticks == 60) capture = true;
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static com.google.gson.JsonObject profile(Minecraft mc, boolean left) throws ReflectiveOperationException {
        Class kind = Class.forName("punchy.client.renderer.layer.VanillaFirstPersonItemLayer$ToolKind");
        Object pickaxe = Enum.valueOf(kind, "PICKAXE");
        Object profile = Class.forName("punchy.client.render.ToolTuning").getMethod("resolveProfile",
                net.minecraft.world.item.ItemStack.class, kind, boolean.class).invoke(null, mc.player.getMainHandItem(), pickaxe, left);
        var json = new com.google.gson.JsonObject();
        for (String field : new String[]{"scale", "offX", "offY", "offZ", "rotX", "rotY", "rotZ", "pivotX", "pivotY", "pivotZ"}) {
            json.addProperty(field, profile.getClass().getField(field).getFloat(profile));
        }
        return json;
    }
    public static void frame(Minecraft mc) throws Exception {
        if (!capture || mc.getOverlay() != null) return;
        capture = false;
        Path path = Path.of("screenshots", "punchy-candidate-" + ANGLES[phase] + ".png");
        Files.createDirectories(path.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(path); }
        phase++; ticks = 0; loading = false; ready = false;
        if (phase == ANGLES.length) {
            System.out.println("PUNCHY_SWEEP_PASS: actual resource-pack pose candidates captured");
            mc.stop();
        }
    }
}
