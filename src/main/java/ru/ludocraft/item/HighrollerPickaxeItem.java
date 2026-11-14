package ru.ludocraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

public final class HighrollerPickaxeItem extends PickaxeItem {
    public HighrollerPickaxeItem(Item.Properties properties) {
        super(HighrollerTier.INSTANCE, properties.fireResistant().rarity(Rarity.EPIC)
                .attributes(PickaxeItem.createAttributes(HighrollerTier.INSTANCE, 5.0F, -2.8F)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.ludocraft.ore_only").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ludocraft.highroller_loss").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.ludocraft.highroller_jackpot").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.ludocraft.mega_jackpot").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
