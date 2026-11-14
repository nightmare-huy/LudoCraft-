package ru.ludocraft.gametest.client;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import ru.ludocraft.LudoCraft;

/** Dev-only deterministic world + real rendered hand views, not a model-only preview. */
@EventBusSubscriber(modid = LudoCraft.MOD_ID, value = Dist.CLIENT)
public final class HandPoseClientTest {
    private static final String[] SHOTS = {"before-first-right", "after-first-right", "after-first-left",
            "after-third-front", "after-third-side", "after-third-left", "after-first-swing"};
    private static boolean baked, started, configured, capture;
    private static int phase, ticks;
    private static org.joml.Vector3f originalRotation, originalTranslation, originalScale;
    private HandPoseClientTest() {}
    public static void startAfterBake() { baked = true; }
    private static boolean enabled() { return Boolean.getBoolean("ludocraft.handPoseTest"); }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!enabled() || !baked) return;
        try {
            var mc = Minecraft.getInstance();
            if (!started && mc.screen instanceof AccessibilityOnboardingScreen && mc.getOverlay() == null) {
                mc.options.onboardingAccessibilityFinished();
                mc.setScreen(new TitleScreen());
            }
            if (!started && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                System.out.println("HAND_POSE_WORLD: creating deterministic world");
                started = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(2);
                mc.options.simulationDistance().set(2);
                mc.options.bobView().set(false);
                mc.options.fov().set(70);
                var rules = new GameRules();
                mc.createWorldOpenFlows().createFreshLevel("ludocraft-hand-check",
                        new LevelSettings("LudoCraft pose check", GameType.CREATIVE, false,
                                Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(777L, false, false),
                        access -> access.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
                return;
            }
            if (mc.player == null || mc.level == null || mc.screen != null || mc.getOverlay() != null) return;
            if (!configured) {
                configured = true;
                var uuid = mc.player.getUUID();
                var server = mc.getSingleplayerServer();
                if (server == null) throw new AssertionError("No integrated server");
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    if (player == null) throw new AssertionError("No test player");
                    player.teleportTo(.5, -60, .5);
                    player.getInventory().selected = 0;
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LudoCraft.GAMBLERS_PICKAXE.get()));
                    player.setMainArm(HumanoidArm.RIGHT);
                    player.serverLevel().setDayTime(1000);
                    server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                });
            }
            if (!mc.player.getMainHandItem().is(LudoCraft.GAMBLERS_PICKAXE.get())) return;
            float yaw = phase == 4 ? 90 : phase == 5 ? -35 : 0;
            mc.player.setYRot(yaw); mc.player.yRotO = yaw;
            mc.player.setXRot(0); mc.player.xRotO = 0;
            mc.player.yBodyRot = 0; mc.player.yBodyRotO = 0;
            mc.player.yHeadRot = yaw; mc.player.yHeadRotO = yaw;
            if (ticks == 0) configureView(mc);
            ticks++;
            if (phase == 6 && ticks == 60) mc.player.swing(InteractionHand.MAIN_HAND);
            if (ticks == (phase == 6 ? 62 : 80)) capture = true;
        } catch (Throwable failure) { fail(failure); }
    }
    private static void configureView(Minecraft mc) {
        var model = mc.getModelManager().getModel(ModelResourceLocation.inventory(LudoCraft.id("gamblers_pickaxe")));
        var transform = model.getTransforms().getTransform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        // A/B in the SAME world and camera: old 1.5.1 pose versus exported corrected pose.
        if (phase == 0) {
            originalRotation = new org.joml.Vector3f(transform.rotation);
            originalTranslation = new org.joml.Vector3f(transform.translation);
            originalScale = new org.joml.Vector3f(transform.scale);
            transform.rotation.set(0, -90, 20);
            transform.translation.set(1.13F/16, 3.2F/16, 1.13F/16);
            transform.scale.set(.85F);
        } else {
            // Restore the ACTUAL baked JSON, never a separate hard-coded candidate pose.
            transform.rotation.set(originalRotation);
            transform.translation.set(originalTranslation);
            transform.scale.set(originalScale);
        }
        mc.player.setMainArm(phase == 2 || phase == 5 ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(mc.player);
        mc.gameRenderer.setRenderHand(true);
        if (phase >= 3 && phase <= 5) {
            // Actual F5 front camera. Keep torso yaw fixed and turn the head/camera to
            // inspect the side; vanilla deliberately hides LocalPlayer from other entities' cameras.
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
    }
    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (!enabled() || !capture) return;
        capture = false;
        try {
            var mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null) throw new AssertionError("Screenshot would capture a loading screen");
            Path output = Path.of("screenshots", "hand-" + SHOTS[phase] + ".png");
            Files.createDirectories(output.getParent());
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                if (image.getWidth() < 900 || image.getHeight() < 500) throw new AssertionError("Bad framebuffer size");
                image.writeToFile(output);
            }
            System.out.println("HAND_POSE_CAPTURE: " + output);
            phase++; ticks = 0;
            if (phase == SHOTS.length) {
                System.out.println("HAND_POSE_WORLD_PASS: seven actual world captures including both hands, third-person side and swing");
                mc.stop();
            }
        } catch (Throwable failure) { fail(failure); }
    }
    private static void fail(Throwable failure) {
        failure.printStackTrace();
        System.err.println("HAND_POSE_WORLD_FAIL: " + failure);
        System.exit(1);
    }
}
