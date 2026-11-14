package ru.ludocraft.client;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.world.item.component.FireworkExplosion;

/** Uses vanilla firework rendering and sounds, but never creates a damaging rocket entity. */
final class JackpotFireworkParticle extends NoRenderParticle {
    private static final List<FireworkExplosion> EXPLOSIONS = List.of(new FireworkExplosion(
            FireworkExplosion.Shape.SMALL_BALL,
            IntList.of(0xFFD644, 0xFF426D, 0x49E5FF), IntList.of(0xFFFFFF), true, true));

    JackpotFireworkParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        lifetime = 13;
    }

    @Override
    public void tick() {
        if (age % 6 == 0) {
            double angle = (age / 6) * Math.PI * 2 / 3;
            level.createFireworks(x + Math.cos(angle) * 0.8, y + 0.6 + age * 0.06,
                    z + Math.sin(angle) * 0.8, 0, 0.03, 0, EXPLOSIONS);
        }
        if (++age >= lifetime) {
            remove();
        }
    }
}
