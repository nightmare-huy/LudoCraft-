package ru.ludocraft.gameplay;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import ru.ludocraft.LudoCraft;

/** Runtime behavior for the data-driven ludocraft:gambling enchantment. */
public final class EnchantedGambling {
    private static final ResourceKey<Enchantment> GAMBLING = ResourceKey.create(
            Registries.ENCHANTMENT, LudoCraft.id("gambling"));

    private EnchantedGambling() {}

    /** Hard stop for every anvil user, including creative mode's otherwise-unrestricted path. */
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!isExcludedPickaxe(event.getLeft()) && !isExcludedPickaxe(event.getRight())) return;
        var lookup = event.getPlayer().level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> enchantment = lookup.get(GAMBLING).orElse(null);
        if (enchantment == null) return;
        if (hasGambling(event.getLeft(), enchantment) || hasGambling(event.getRight(), enchantment)) {
            event.setCanceled(true);
        }
    }

    private static boolean isExcludedPickaxe(ItemStack stack) {
        return stack.is(LudoCraft.GAMBLERS_PICKAXE.get())
                || stack.is(LudoCraft.VIP_GAMBLERS_PICKAXE.get())
                || stack.is(LudoCraft.HIGHROLLER_PICKAXE.get());
    }

    private static boolean hasGambling(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(enchantment) > 0
                || stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(enchantment) > 0;
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled() || !(event.getBreaker() instanceof Player player)
                || !OreGambling.isOre(event.getState())) return;

        ItemStack tool = event.getTool();
        if (tool.is(LudoCraft.GAMBLERS_PICKAXE.get())
                || tool.is(LudoCraft.VIP_GAMBLERS_PICKAXE.get())
                || tool.is(LudoCraft.HIGHROLLER_PICKAXE.get())) return;

        Holder<Enchantment> enchantment = event.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).get(GAMBLING).orElse(null);
        if (enchantment == null) return;
        int level = tool.getEnchantmentLevel(enchantment);
        if (level < 1 || level > 3) return;

        int multiplier = EnchantedGamblingRules.rollMultiplier(level, event.getLevel().random::nextInt);
        if (multiplier == 0) {
            event.getDrops().clear();
            event.setDroppedExperience(0);
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.ludocraft.loss")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        OreGambling.multiplyDrops(event.getDrops(), event.getLevel(), multiplier);
        player.displayClientMessage(Component.translatable("message.ludocraft.gambling_jackpot", multiplier)
                .withStyle(level == 3 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD), false);
        double x = event.getPos().getX() + 0.5;
        double y = event.getPos().getY() + 0.5;
        double z = event.getPos().getZ() + 0.5;
        event.getLevel().playSound(null, x, y, z, LudoCraft.JACKPOT_SOUND.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
        event.getLevel().sendParticles(LudoCraft.CONFETTI.get(), x, y, z, level == 3 ? 180 : 70,
                level == 3 ? 0.65 : 0.35, level == 3 ? 0.55 : 0.25,
                level == 3 ? 0.65 : 0.35, level == 3 ? 0.38 : 0.25);
        // JackpotFireworkParticle creates one vanilla explosion per particle instance.
        event.getLevel().sendParticles(LudoCraft.JACKPOT_FIREWORK.get(), x, y, z,
                level == 3 ? 6 : 1, 0, 0, 0, 0);
    }
}
