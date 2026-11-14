package ru.ludocraft.gameplay;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import ru.ludocraft.LudoCraft;

public final class OreGambling {
    private OreGambling() {}

    public static boolean isOre(BlockState state) {
        // Includes ancient debris and modded ores added to c:ores by data packs.
        return state.is(Tags.Blocks.ORES)
                || GamblingRules.hasOreWord(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled()
                || !(event.getBreaker() instanceof Player player)
                || !event.getTool().is(LudoCraft.GAMBLERS_PICKAXE.get())
                || !isOre(event.getState())) {
            return;
        }

        ServerLevel level = event.getLevel();
        int multiplier = GamblingRules.rollMultiplier(level.random::nextInt);
        if (multiplier == 0) {
            event.getDrops().clear();
            event.setDroppedExperience(0);
            event.setCanceled(true); // Also prevents spawnAfterBreak and XP, not just items.
            player.displayClientMessage(Component.translatable("message.ludocraft.loss")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        multiplyDrops(event.getDrops(), level, multiplier);
        // Jackpot XP is deliberately unchanged; the specification multiplies items only.
        player.displayClientMessage(Component.translatable("message.ludocraft.jackpot", multiplier)
                .withStyle(ChatFormatting.GOLD), false);
        double x = event.getPos().getX() + 0.5;
        double y = event.getPos().getY() + 0.5;
        double z = event.getPos().getZ() + 0.5;
        level.playSound(null, x, y, z, LudoCraft.JACKPOT_SOUND.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        level.sendParticles(LudoCraft.CONFETTI.get(), x, y, z, 70, 0.35, 0.25, 0.35, 0.25);
        // One networked emitter, not 3 damaging rocket entities. All nearby clients see it.
        level.sendParticles(LudoCraft.JACKPOT_FIREWORK.get(), x, y, z, 1, 0, 0, 0, 0);
    }

    static void multiplyDrops(List<ItemEntity> drops, ServerLevel level, int multiplier) {
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity original : drops) {
            ItemStack stack = original.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            // Preserve the original entity and its metadata; only append the bonus stacks.
            // copyWithCount retains all components (including enchantments/custom data).
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
