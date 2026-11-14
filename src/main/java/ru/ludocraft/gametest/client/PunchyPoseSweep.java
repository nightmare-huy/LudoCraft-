package ru.ludocraft.gametest.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.resources.model.ModelResourceLocation;
import ru.ludocraft.LudoCraft;

/** Dev-only comparison: use the actual vanilla diamond-pickaxe view transforms/profile as the candidate. */
public final class PunchyPoseSweep {
    private static final String[] CASES = {"vanilla-reference", "released", "match-diamond", "match-diamond-left", "match-diamond-down"};
    private static int phase, ticks;
    private static boolean loading, ready, capture;
    private static com.google.gson.JsonObject diamondRight, diamondLeft, releasedRight, releasedLeft;
    private PunchyPoseSweep() {}
    public static boolean enabled() { return Boolean.getBoolean("ludocraft.punchySweep"); }
    public static void tick(Minecraft mc) {
        if (phase >= CASES.length) return;
        mc.player.setYRot(0); mc.player.yRotO = 0;
        float pitch = CASES[phase].endsWith("down") ? 25 : 0;
        mc.player.setXRot(pitch); mc.player.xRotO = pitch;
        mc.player.setMainArm(CASES[phase].endsWith("left") ? net.minecraft.world.entity.HumanoidArm.LEFT : net.minecraft.world.entity.HumanoidArm.RIGHT);
        mc.player.yHeadRot = 0; mc.player.yHeadRotO = 0;
        if (!loading) {
            loading = true;
            try {
                if (diamondRight == null) {
                    mc.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
                    diamondRight = profile(mc, false);
                    diamondLeft = profile(mc, true);
                    mc.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LudoCraft.GAMBLERS_PICKAXE.get()));
                    releasedRight = profile(mc, false);
                    releasedLeft = profile(mc, true);
                    var vanillaModel = mc.getModelManager().getModel(ModelResourceLocation.inventory(
                            ResourceLocation.withDefaultNamespace("diamond_pickaxe")));
                    var transforms = new com.google.gson.JsonObject();
                    transforms.add("right", displayTransform(vanillaModel.getTransforms().getTransform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)));
                    transforms.add("left", displayTransform(vanillaModel.getTransforms().getTransform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)));
                    Files.createDirectories(Path.of("screenshots"));
                    Files.writeString(Path.of("screenshots", "punchy-grip-profiles.json"),
                            "{\"diamondRight\":" + diamondRight + ",\"diamondLeft\":" + diamondLeft +
                                    ",\"releasedRight\":" + releasedRight + ",\"releasedLeft\":" + releasedLeft +
                                    ",\"diamondItemDisplay\":" + transforms + "}\n");
                }
                var right = (phase >= 2 ? diamondRight : releasedRight).deepCopy();
                var left = (phase >= 2 ? diamondLeft : releasedLeft).deepCopy();
                var sides = new com.google.gson.JsonObject(); sides.add("right", right); sides.add("left", left);
                var entry = new com.google.gson.JsonObject(); entry.addProperty("kind", "PICKAXE"); entry.add("transforms", sides);
                var specifics = new com.google.gson.JsonObject(); specifics.add("ludocraft:gamblers_pickaxe", entry);
                var json = new com.google.gson.JsonObject(); json.add("itemSpecific", specifics);
                Path root = Path.of("resourcepacks", "ludka-punchy-candidate-" + CASES[phase]);
                Path path = root.resolve("assets/minecraft/punchy/compat/ludocraft_candidate.json");
                Files.createDirectories(path.getParent());
                Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":34,\"description\":\"LudoCraft diamond-pickaxe pose comparison\"}}");
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
            boolean candidate = phase >= 2;
            mc.player.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(phase == 0 ? Items.DIAMOND_PICKAXE : LudoCraft.GAMBLERS_PICKAXE.get()));
            mc.reloadResourcePacks().thenRun(() -> mc.execute(() -> {
                if (candidate) copyDiamondViewTransforms(mc);
                ready = true;
            }));
        }
        if (ready && ++ticks == 80) capture = true;
    }
    private static com.google.gson.JsonObject displayTransform(net.minecraft.client.renderer.block.model.ItemTransform transform) {
        var result = new com.google.gson.JsonObject();
        result.add("rotation", vector(transform.rotation));
        result.add("translation", vector(transform.translation));
        result.add("scale", vector(transform.scale));
        return result;
    }
    private static com.google.gson.JsonArray vector(org.joml.Vector3f value) {
        var result = new com.google.gson.JsonArray();
        result.add(value.x); result.add(value.y); result.add(value.z);
        return result;
    }
    private static void copyDiamondViewTransforms(Minecraft mc) {
        var custom = mc.getModelManager().getModel(ModelResourceLocation.inventory(LudoCraft.id("gamblers_pickaxe")));
        var vanilla = mc.getModelManager().getModel(ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("diamond_pickaxe")));
        for (var context : new ItemDisplayContext[]{ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND}) {
            var from = vanilla.getTransforms().getTransform(context);
            var to = custom.getTransforms().getTransform(context);
            to.rotation.set(from.rotation); to.translation.set(from.translation); to.scale.set(from.scale);
        }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static com.google.gson.JsonObject profile(Minecraft mc, boolean left) throws ReflectiveOperationException {
        Class kind = Class.forName("punchy.client.renderer.layer.VanillaFirstPersonItemLayer$ToolKind");
        Object pickaxe = Enum.valueOf(kind, "PICKAXE");
        Object profile = Class.forName("punchy.client.render.ToolTuning").getMethod("resolveProfile",
                ItemStack.class, kind, boolean.class).invoke(null, mc.player.getMainHandItem(), pickaxe, left);
        var json = new com.google.gson.JsonObject();
        for (String field : new String[]{"scale", "offX", "offY", "offZ", "rotX", "rotY", "rotZ", "pivotX", "pivotY", "pivotZ"})
            json.addProperty(field, profile.getClass().getField(field).getFloat(profile));
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
        var diagnostic = new com.google.gson.JsonObject();
        diagnostic.add("resolvedPunchyRight", profile(mc, false));
        diagnostic.add("resolvedPunchyLeft", profile(mc, true));
        var customModel = mc.getModelManager().getModel(ModelResourceLocation.inventory(LudoCraft.id("gamblers_pickaxe")));
        var vanillaModel = mc.getModelManager().getModel(ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("diamond_pickaxe")));
        var customDisplay = new com.google.gson.JsonObject();
        var vanillaDisplay = new com.google.gson.JsonObject();
        for (var context : new ItemDisplayContext[]{ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND}) {
            customDisplay.add(context.name(), displayTransform(customModel.getTransforms().getTransform(context)));
            vanillaDisplay.add(context.name(), displayTransform(vanillaModel.getTransforms().getTransform(context)));
        }
        diagnostic.add("customDisplay", customDisplay);
        diagnostic.add("vanillaDisplay", vanillaDisplay);
        var specificMethod = Class.forName("punchy.client.render.ToolTuning").getMethod("hasSpecificProfile", ItemStack.class);
        diagnostic.addProperty("heldItem", mc.player.getMainHandItem().getItem().toString());
        diagnostic.addProperty("customHasSpecific", (Boolean) specificMethod.invoke(null, mc.player.getMainHandItem()));
        diagnostic.addProperty("diamondHasSpecific", (Boolean) specificMethod.invoke(null, new ItemStack(Items.DIAMOND_PICKAXE)));
        Files.createDirectories(Path.of("screenshots"));
        Files.writeString(Path.of("screenshots", "punchy-transform-diagnostic.json"), diagnostic.toString());
        for (boolean left : new boolean[]{false, true}) {
            var actual = profile(mc, left);
            var pose = expected.getAsJsonObject(left ? "left" : "right");
            for (String key : pose.keySet()) if (Math.abs(pose.get(key).getAsFloat() - actual.get(key).getAsFloat()) > .0001F)
                throw new AssertionError("Punchy did not apply shipped " + key + ": " + actual);
        }
        var method = Class.forName("punchy.client.render.ToolTuning").getMethod("hasSpecificProfile", ItemStack.class);
        boolean customSpecific = (Boolean) method.invoke(null, mc.player.getMainHandItem());
        boolean diamondSpecific = (Boolean) method.invoke(null, new ItemStack(Items.DIAMOND_PICKAXE));
        if (!customSpecific || diamondSpecific)
            throw new AssertionError("Wrong item-specific profile scope: held=" + mc.player.getMainHandItem().getItem() +
                    ", customSpecific=" + customSpecific + ", diamondSpecific=" + diamondSpecific);
        var custom = mc.getModelManager().getModel(ModelResourceLocation.inventory(LudoCraft.id("gamblers_pickaxe")));
        var vanilla = mc.getModelManager().getModel(ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("diamond_pickaxe")));
        for (var context : new ItemDisplayContext[]{ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND}) {
            var actual = custom.getTransforms().getTransform(context);
            var reference = vanilla.getTransforms().getTransform(context);
            if (!actual.rotation.equals(reference.rotation, .0001F) || !actual.translation.equals(reference.translation, .0001F) ||
                    !actual.scale.equals(reference.scale, .0001F))
                throw new AssertionError("LudoCraft first-person item transform differs from vanilla diamond pickaxe for " + context +
                        ": rotation=" + actual.rotation + ", translation=" + actual.translation + ", scale=" + actual.scale);
        }
        System.out.println("PUNCHY_COMPAT_PASS: shipped Punchy profiles and both first-person item transforms match vanilla diamond pickaxe");
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
            System.out.println("PUNCHY_SWEEP_PASS: exact diamond-pickaxe pose and model-transform reference captured; not a release");
            mc.stop();
        }
    }
}
