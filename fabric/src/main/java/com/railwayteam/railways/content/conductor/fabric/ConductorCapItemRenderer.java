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

package com.railwayteam.railways.content.conductor.fabric;

import net.neoforged.bus.api.IEventBus;

/**
 * NeoForge (ZP5 port): upstream drew conductor caps worn by players through Fabric API's ArmorRenderer, which has no
 * NeoForge counterpart. TODO(neoforge 26.3): a player render layer (EntityRenderersEvent.AddLayers) or an equipment
 * asset. Until then a worn cap is not drawn on players; conductors still draw theirs (ConductorCapLayer).
 */
public class ConductorCapItemRenderer {
	public static void register(IEventBus modBus) {
	}
}
