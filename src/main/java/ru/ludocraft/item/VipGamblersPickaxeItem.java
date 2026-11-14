package ru.ludocraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

public final class VipGamblersPickaxeItem extends PickaxeItem {
    public VipGamblersPickaxeItem(Item.Properties properties) {
        super(Tiers.NETHERITE, properties.fireResistant()
                .attributes(PickaxeItem.createAttributes(Tiers.NETHERITE, 1.0F, -2.8F)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.ludocraft.ore_only").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ludocraft.vip_loss").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.ludocraft.vip_jackpot").withStyle(ChatFormatting.GOLD));
    }
}
