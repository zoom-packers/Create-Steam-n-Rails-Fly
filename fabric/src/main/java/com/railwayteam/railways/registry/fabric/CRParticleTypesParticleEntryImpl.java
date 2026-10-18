package com.railwayteam.railways.registry.fabric;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.internal.compat.create.foundation.particle.ICustomParticleData;
import com.railwayteam.railways.internal.compat.create.foundation.particle.ICustomParticleDataWithSprite;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CRParticleTypesParticleEntryImpl {
    public static void register(String id, Supplier<ParticleType<?>> supplier) {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Railways.asResource(id), supplier.get());
    }

    public static void register() {
        com.railwayteam.railways.registry.CRParticleTypes.init();
    }

    @SuppressWarnings("unchecked")
    @Environment(EnvType.CLIENT)
    public static <T extends ParticleOptions> void registerFactory(ParticleType<T> object, ParticleEngine engine, ICustomParticleData<T> customParticleData) {
        if (customParticleData instanceof ICustomParticleDataWithSprite<T> withSprite) {
            // NeoForge: providers are registered in RegisterParticleProvidersEvent (RailwaysClientImpl)
            PROVIDERS.add(event -> event.registerSpriteSet(object, sprite -> withSprite.getMetaFactory().apply(sprite)));
        }
    }

    @Environment(EnvType.CLIENT)
    private static final List<Consumer<RegisterParticleProvidersEvent>> PROVIDERS = new ArrayList<>();

    @Environment(EnvType.CLIENT)
    public static void registerProviders(RegisterParticleProvidersEvent event) {
        PROVIDERS.forEach(provider -> provider.accept(event));
    }
}
