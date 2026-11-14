package ru.ludocraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

public final class GamblersPickaxeItem extends PickaxeItem {
    public GamblersPickaxeItem(Item.Properties properties) {
        super(Tiers.DIAMOND, properties.rarity(Rarity.RARE)
                .attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, 1.0F, -2.8F)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                               List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.ludocraft.ore_only").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ludocraft.loss").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.ludocraft.jackpot").withStyle(ChatFormatting.GOLD));
    }
}
