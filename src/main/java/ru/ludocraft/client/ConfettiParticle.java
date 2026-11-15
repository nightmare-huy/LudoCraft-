package ru.ludocraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

final class ConfettiParticle extends TextureSheetParticle {
    private static final int[] COLORS = {0xFF426D, 0xFFD644, 0x49E5FF, 0x87FF65, 0xBE79FF, 0xFFFFFF};
    private final float spin;

    private ConfettiParticle(ClientLevel level, double x, double y, double z,
                             double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        xd = vx;
        yd = Math.abs(vy) + 0.15;
        zd = vz;
        gravity = 0.06F;
        friction = 0.97F;
        lifetime = 35 + random.nextInt(26);
        quadSize = 0.05F + random.nextFloat() * 0.04F;
        roll = random.nextFloat() * (float) (Math.PI * 2);
        oRoll = roll;
        spin = (random.nextFloat() - 0.5F) * 0.4F;
        int rgb = COLORS[random.nextInt(COLORS.length)];
        setColor(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F);
    }

    @Override
    public void tick() {
        super.tick();
        oRoll = roll;
        roll += spin;
        if (lifetime - age < 12) {
            setAlpha(Math.max(0, (lifetime - age) / 12F));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new ConfettiParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
