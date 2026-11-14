package ru.ludocraft;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.ludocraft.gameplay.OreGambling;
import ru.ludocraft.item.GamblersPickaxeItem;

@Mod(LudoCraft.MOD_ID)
public final class LudoCraft {
    public static final String MOD_ID = "ludocraft";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, MOD_ID);

    public static final DeferredItem<GamblersPickaxeItem> GAMBLERS_PICKAXE =
            ITEMS.registerItem("gamblers_pickaxe", GamblersPickaxeItem::new);
    public static final DeferredHolder<SoundEvent, SoundEvent> JACKPOT_SOUND =
            SOUNDS.register("jackpot_777", () -> SoundEvent.createVariableRangeEvent(id("jackpot_777")));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CONFETTI =
            PARTICLES.register("confetti", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> JACKPOT_FIREWORK =
            PARTICLES.register("jackpot_firework", () -> new SimpleParticleType(true));

    public LudoCraft(IEventBus modBus) {
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        PARTICLES.register(modBus);
        modBus.addListener(this::addCreativeItems);
        // Process the already calculated loot. Never recalculate loot or spawn a second vanilla drop.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, OreGambling::onBlockDrops);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GAMBLERS_PICKAXE);
        }
    }
}
