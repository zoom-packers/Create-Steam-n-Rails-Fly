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

package com.railwayteam.railways.neoforge;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.fabric.RailwaysCreateRegisterPlugin;
import com.railwayteam.railways.multiloader.fabric.PacketSetImpl;
import com.railwayteam.railways.util.fabric.RegistrationListeningImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * NeoForge entrypoint (ZP5 port), replacing the Fabric {@code main} entrypoint.
 * <p>
 * Railways' content is not registered here. NeoForge freezes the vanilla registries before it constructs mods, so,
 * like Create Fly's own NeoForge port, Railways registers during vanilla bootstrap: blocks and fluids through Create's
 * {@code CreateRegisterPlugin} (a {@code META-INF/services} entry), everything else from
 * {@link com.railwayteam.railways.neoforge.mixin.CreateRegisterMixin}, which runs the Fabric initializer
 * ({@link com.railwayteam.railways.fabric.RailwaysImpl#onInitialize()}) right after {@code Create.register()}.
 * This class wires up what Fabric API did through callbacks.
 */
@Mod(Railways.MOD_ID)
public class RailwaysNeoForge {
	public RailwaysNeoForge(IEventBus modBus, ModContainer container) {
		RailwaysCreateRegisterPlugin.verifyEarlyRegistrationComplete();
		if (!RailwaysNeoForgeInit.isInitialized())
			throw new IllegalStateException("Railways was not initialized during bootstrap; is Create Fly (NeoForge) installed?");
		modBus.addListener(PacketSetImpl::registerPayloads);
		modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(RegistrationListeningImpl::flush));
		RailwaysNeoForgeInit.MOD_BUS_TASKS.forEach(task -> task.accept(modBus));
		RailwaysNeoForgeInit.MOD_BUS_TASKS.clear();
	}
}
