package ru.ludocraft.gametest.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import ru.ludocraft.LudoCraft;

/** Dev-only asset smoke test; the entire gametest package is excluded from the JAR. */
@EventBusSubscriber(modid = LudoCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PickaxeModelSmokeTest {
    private PickaxeModelSmokeTest() {}

    @SubscribeEvent
    public static void baked(ModelEvent.BakingCompleted event) {
        if (!Boolean.getBoolean("ludocraft.modelSmokeTest")) return;
        try {
            var model = event.getModels().get(ModelResourceLocation.inventory(LudoCraft.id("gamblers_pickaxe")));
            require(model != null && model != event.getModelManager().getMissingModel(), "Missing pickaxe model");
            require(model.isGui3d(), "Model was baked as a flat/generated item");
            require(model.getParticleIcon().contents().name().equals(LudoCraft.id("item/gamblers_pickaxe_3d")), "Missing particle sprite");
            var quads = model.getQuads(null, null, RandomSource.create(0));
            require(quads.size() == 241, "Wrong exposed quad count: " + quads.size());
            var directions = EnumSet.noneOf(Direction.class);
            for (var quad : quads) {
                directions.add(quad.getDirection());
                require(quad.getSprite().contents().name().equals(LudoCraft.id("item/gamblers_pickaxe_3d")), "Missing/wrong face sprite");
                int[] data = quad.getVertices(); int stride = data.length / 4;
                require(stride >= 8, "Unexpected vertex format");
                for (int vertex = 0; vertex < 4; vertex++) {
                    for (int axis = 0; axis < 3; axis++) {
                        float value = Float.intBitsToFloat(data[vertex * stride + axis]);
                        require(Float.isFinite(value) && value >= 0 && value <= 1, "Invalid baked position");
                    }
                    for (int uv = 4; uv <= 5; uv++) require(Float.isFinite(Float.intBitsToFloat(data[vertex * stride + uv])), "Non-finite baked UV");
                }
            }
            require(directions.size() == 6, "Missing outward face directions");
            for (var context : ItemDisplayContext.values()) {
                if (context == ItemDisplayContext.NONE) continue;
                var pose = new PoseStack();
                model.applyTransform(context, pose, context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                        || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
                require(pose.last().pose().isFinite(), "Non-finite item transform: " + context);
                var scale = model.getTransforms().getTransform(context).scale;
                require(scale.x > 0 && scale.y > 0 && scale.z > 0, "Invisible or inverted transform: " + context);
            }
            System.out.println("PICKAXE_MODEL_SMOKE_PASS: real client baked 241 quads, correct sprites, finite vertices/UVs, six directions and all display transforms");
            if (Boolean.getBoolean("ludocraft.handPoseTest")) {
                HandPoseClientTest.startAfterBake();
            } else {
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.err.println("PICKAXE_MODEL_SMOKE_FAIL: " + failure);
            System.exit(1);
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
