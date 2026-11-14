package ru.ludocraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import ru.ludocraft.LudoCraft;
import ru.ludocraft.menu.LudkaMenu;

/** Client sends an item ID only, never a prize stack, price, probability or result. */
public record SelectLudkaTarget(int containerId, ResourceLocation item) implements CustomPacketPayload {
    public static final Type<SelectLudkaTarget> TYPE = new Type<>(LudoCraft.id("select_ludka_target"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectLudkaTarget> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectLudkaTarget::containerId,
            ResourceLocation.STREAM_CODEC, SelectLudkaTarget::item, SelectLudkaTarget::new);
    @Override public Type<SelectLudkaTarget> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1.3").playToServer(TYPE, CODEC, (payload, context) -> {
            // NeoForge dispatches this handler on the server main thread.
            if (context.player().containerMenu instanceof LudkaMenu menu && menu.containerId == payload.containerId()) {
                menu.selectTarget(context.player(), payload.item());
            }
        });
    }
}
