package ru.ludocraft.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import ru.ludocraft.LudoCraft;

/** Loaded only on the physical client; no net.minecraft.client references on a dedicated server. */
@Mod(value = LudoCraft.MOD_ID, dist = Dist.CLIENT)
public final class LudoCraftClient {
    public LudoCraftClient(IEventBus modBus) {
        modBus.addListener(this::registerParticles);
    }

    private void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LudoCraft.CONFETTI.get(), ConfettiParticle.Provider::new);
        event.registerSpecial(LudoCraft.JACKPOT_FIREWORK.get(),
                (type, level, x, y, z, vx, vy, vz) -> new JackpotFireworkParticle(level, x, y, z));
    }
}
