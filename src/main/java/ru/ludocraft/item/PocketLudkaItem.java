package ru.ludocraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import ru.ludocraft.menu.LudkaMenu;

public final class PocketLudkaItem extends Item {
    public PocketLudkaItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this) || player.isSpectator()) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new LudkaMenu(id, inventory),
                    Component.translatable("container.ludocraft.ludka")));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ludocraft.pocket_ludka").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.ludocraft.ludka_stake").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ludocraft.ludka_compat").withStyle(ChatFormatting.AQUA));
    }
}
