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
        double angle = level.random.nextDouble() * Math.PI * 2;
        double radius = level.random.nextDouble() * 0.8;
        this.x += Math.cos(angle) * radius;
        this.y += 0.45 + level.random.nextDouble() * 0.35;
        this.z += Math.sin(angle) * radius;
        lifetime = 1;
    }

    @Override
    public void tick() {
        level.createFireworks(x, y, z, 0, 0.03, 0, EXPLOSIONS);
        if (++age >= lifetime) remove();
    }
}
