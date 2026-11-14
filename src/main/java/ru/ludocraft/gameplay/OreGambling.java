package ru.ludocraft.gameplay;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import ru.ludocraft.LudoCraft;

public final class OreGambling {
    private static final int[] VIP_MULTIPLIERS = {3, 4, 5, 6, 7, 8, 10};
    private static final int[] HIGHROLLER_MULTIPLIERS = {5, 8, 10, 12, 15, 20};

    private OreGambling() {}

    public static boolean isOre(BlockState state) {
        // Includes ancient debris and modded ores added to c:ores by data packs.
        return state.is(Tags.Blocks.ORES)
                || GamblingRules.hasOreWord(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled() || !(event.getBreaker() instanceof Player player) || !isOre(event.getState())) {
            return;
        }
        var tool = event.getTool();
        final int lossPercent;
        int[] multipliers;
        final String messagePrefix;
        final boolean highroller;
        final boolean customPickaxe;
        if (tool.is(LudoCraft.GAMBLERS_PICKAXE.get())) {
            lossPercent = 85;
            multipliers = null;
            messagePrefix = "";
            highroller = false;
            customPickaxe = false;
        } else if (tool.is(LudoCraft.VIP_GAMBLERS_PICKAXE.get())) {
            lossPercent = 75;
            multipliers = VIP_MULTIPLIERS;
            messagePrefix = "vip_";
            highroller = false;
            customPickaxe = true;
        } else if (tool.is(LudoCraft.HIGHROLLER_PICKAXE.get())) {
            lossPercent = 65;
            multipliers = HIGHROLLER_MULTIPLIERS;
            messagePrefix = "highroller_";
            highroller = true;
            customPickaxe = true;
        } else {
            return;
        }

        ServerLevel level = event.getLevel();
        if (multipliers == null) {
            // Keep the original Gambler's Pickaxe odds and RNG sequence unchanged.
            int legacyMultiplier = GamblingRules.rollMultiplier(level.random::nextInt);
            if (legacyMultiplier == 0) {
                event.getDrops().clear();
                event.setDroppedExperience(0);
                event.setCanceled(true);
                player.displayClientMessage(Component.translatable("message.ludocraft.loss")
                        .withStyle(ChatFormatting.RED), false);
                return;
            }
            multipliers = new int[]{legacyMultiplier};
        } else if (level.random.nextInt(100) < lossPercent) {
            event.getDrops().clear();
            event.setDroppedExperience(0);
            event.setCanceled(true); // Also prevents spawnAfterBreak and XP, not just items.
            player.displayClientMessage(Component.translatable("message.ludocraft.loss")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        int multiplier = customPickaxe
                ? multipliers[level.random.nextInt(multipliers.length)] : multipliers[0];
        multiplyDrops(event.getDrops(), level, multiplier);
        // Jackpot XP is deliberately unchanged; only item drops are multiplied.
        player.displayClientMessage(Component.translatable("message.ludocraft." + messagePrefix + "jackpot", multiplier)
                .withStyle(highroller ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD), false);
        double x = event.getPos().getX() + 0.5;
        double y = event.getPos().getY() + 0.5;
        double z = event.getPos().getZ() + 0.5;
        level.playSound(null, x, y, z, LudoCraft.JACKPOT_SOUND.get(), SoundSource.PLAYERS,
                highroller ? 1.2F : 0.9F, highroller ? 0.72F : 1.0F);
        level.sendParticles(LudoCraft.CONFETTI.get(), x, y, z, highroller ? 180 : 70,
                highroller ? 0.65 : 0.35, highroller ? 0.55 : 0.25,
                highroller ? 0.65 : 0.35, highroller ? 0.38 : 0.25);
        // Each client-side emitter makes three vanilla-rendered explosions; two emitters create six.
        level.sendParticles(LudoCraft.JACKPOT_FIREWORK.get(), x, y, z, highroller ? 2 : 1, 0, 0, 0, 0);
        if (highroller && multiplier == 15 && player instanceof ServerPlayer serverPlayer) {
            var advancement = level.getServer().getAdvancements()
                    .get(ResourceLocation.fromNamespaceAndPath(LudoCraft.MOD_ID, "gameplay/mega_jackpot"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "mega_jackpot");
            }
        }
    }

    static void multiplyDrops(List<ItemEntity> drops, ServerLevel level, int multiplier) {
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity original : drops) {
            ItemStack stack = original.getItem();
            if (stack.isEmpty()) continue;
            // Preserve the original entity and metadata; append only the bonus stacks.
            long remaining = (long) stack.getCount() * (multiplier - 1);
            while (remaining > 0) {
                int count = (int) Math.min(remaining, stack.getMaxStackSize());
                ItemEntity bonus = new ItemEntity(level, original.getX(), original.getY(), original.getZ(),
                        stack.copyWithCount(count));
                bonus.setDefaultPickUpDelay();
                extra.add(bonus);
                remaining -= count;
            }
        }
        drops.addAll(extra); // CommonHooks spawns these exactly once, after the event.
    }
}
