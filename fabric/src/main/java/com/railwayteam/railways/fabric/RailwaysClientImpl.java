/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2024 The Railways Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.railwayteam.railways.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.railwayteam.railways.Railways;
import com.railwayteam.railways.RailwaysClient;
import com.railwayteam.railways.base.reload.ClientResourceReloadListener;
import com.railwayteam.railways.content.buffer.headstock.fabric.CopycatHeadstockModelRegistration;
import com.railwayteam.railways.content.conductor.fabric.ConductorCapItemRenderer;
import com.railwayteam.railways.content.custom_tracks.monorail.MonorailClientRuntimeChecks;
import com.railwayteam.railways.events.ClientEvents;
import com.railwayteam.railways.multiloader.fabric.PacketSetImpl;
import com.railwayteam.railways.registry.CRParticleTypes;
import com.railwayteam.railways.registry.fabric.CRBlockEntitiesClientImpl;
import com.railwayteam.railways.registry.fabric.CRKeysImpl;
import com.railwayteam.railways.registry.fabric.CRParticleTypesParticleEntryImpl;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * NeoForge (ZP5 port): the Fabric client entrypoint, run by {@code neoforge.RailwaysNeoForgeClient}. Fabric API's
 * client registries and callbacks are replaced by NeoForge events; the static helpers below collect their entries
 * until the matching mod-bus event fires.
 */
public class RailwaysClientImpl {
	private static final List<Runnable> LAYER_DEFINITIONS = new ArrayList<>();
	private static final List<EntityRendererRegistration<?>> ENTITY_RENDERERS = new ArrayList<>();
	private static final List<String[]> BUILTIN_PACKS = new ArrayList<>();
	private static EntityRenderersEvent.RegisterLayerDefinitions layerEvent;

	public void onInitializeClient(IEventBus modBus) {
		modBus.addListener(RailwaysClientImpl::registerLayerDefinitions);
		modBus.addListener(RailwaysClientImpl::registerEntityRenderers);
		modBus.addListener(RailwaysClientImpl::addPackFinders);
		modBus.addListener(PacketSetImpl::registerClientHandlers);
		modBus.addListener(CRKeysImpl::registerKeyMappings);
		modBus.addListener(CRParticleTypesParticleEntryImpl::registerProviders);
		modBus.addListener(AddClientReloadListenersEvent.class, event ->
			event.addListener(Railways.asResource("client_resource_reload"), ClientResourceReloadListener.INSTANCE));

		CopycatHeadstockModelRegistration.register();
		CRBlockEntitiesClientImpl.register();
		RailwaysClient.init();
		MonorailClientRuntimeChecks.run();
		// NeoForge: JEI finds RailwaysJeiClient through its @JeiPlugin annotation
		ConductorCapItemRenderer.register(modBus);
		CRParticleTypes.registerFactories();
		registerClientEvents();
	}

	private static void registerClientEvents() {
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, event -> ClientEvents.onClientTickStart(net.minecraft.client.Minecraft.getInstance()));
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> ClientEvents.onClientTickEnd(net.minecraft.client.Minecraft.getInstance()));
		NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, event -> {
			if (event.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel level)
				ClientEvents.onClientWorldLoad(level);
		});
		NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> ClientEvents.onClientDisconnect());
		// Fabric registered a SERVER_DATA reload listener on the client: tags were (re)loaded
		NeoForge.EVENT_BUS.addListener(TagsUpdatedEvent.class, event -> ClientEvents.onTagsUpdated());
		// Was MixinItemInHandRenderer: hide the stationary player's hands while viewing through a conductor
		NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, event -> {
			if (com.railwayteam.railways.content.conductor.ClientHandler.isPlayerMountedOnCamera())
				event.setCanceled(true);
		});
	}

	@SuppressWarnings({"unchecked", "rawtypes"}) // jank!
	public static void registerClientCommands(Consumer<CommandDispatcher<SharedSuggestionProvider>> consumer) {
		NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> {
			CommandDispatcher<SharedSuggestionProvider> casted = (CommandDispatcher) event.getDispatcher();
			consumer.accept(casted);
		});
	}

	public static void registerModelLayer(ModelLayerLocation layer, Supplier<LayerDefinition> definition) {
		LAYER_DEFINITIONS.add(() -> layerEvent.registerLayerDefinition(layer, definition));
	}

	public static <T extends Entity> void registerEntityRenderer(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
		ENTITY_RENDERERS.add(new EntityRendererRegistration<>(type, provider));
	}

	public static void registerBuiltinPack(String id, String name) {
		BUILTIN_PACKS.add(new String[] {id, name});
	}

	private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		layerEvent = event;
		LAYER_DEFINITIONS.forEach(Runnable::run);
		layerEvent = null;
	}

	private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		ENTITY_RENDERERS.forEach(registration -> registration.register(event));
	}

	private static void addPackFinders(AddPackFindersEvent event) {
		if (event.getPackType() != PackType.CLIENT_RESOURCES)
			return;
		for (String[] pack : BUILTIN_PACKS)
			event.addPackFinders(Railways.asResource("resourcepacks/" + pack[0]), PackType.CLIENT_RESOURCES,
				Component.literal(pack[1]), PackSource.BUILT_IN, false, Pack.Position.TOP);
	}

	private record EntityRendererRegistration<T extends Entity>(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
		void register(EntityRenderersEvent.RegisterRenderers event) {
			event.registerEntityRenderer(type, provider);
		}
	}
}
