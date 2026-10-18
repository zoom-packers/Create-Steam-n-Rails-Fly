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

package com.railwayteam.railways.fabric.events;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.fuel.LiquidFuelManager;
import com.railwayteam.railways.events.CommonEvents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** NeoForge: the Fabric API lifecycle callbacks upstream used, as NeoForge game-bus listeners. */
public class CommonEventsFabric {
	public static void init() {
		NeoForge.EVENT_BUS.addListener(LevelTickEvent.Pre.class, event -> {
			if (!event.getLevel().isClientSide())
				CommonEvents.onWorldTickStart(event.getLevel());
		});
		NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
			if (event.getEntity() instanceof ServerPlayer player)
				CommonEvents.onPlayerJoin(player);
		});
		// Fabric's END_DATA_PACK_RELOAD: tags are rebound on the server after every data pack (re)load
		NeoForge.EVENT_BUS.addListener(TagsUpdatedEvent.ServerDataLoad.class, event -> CommonEvents.onTagsUpdated());
		NeoForge.EVENT_BUS.addListener(AddServerReloadListenersEvent.class, event ->
			event.addListener(Railways.asResource(LiquidFuelManager.ReloadListener.ID), LiquidFuelManager.ReloadListener.INSTANCE));
	}
}
