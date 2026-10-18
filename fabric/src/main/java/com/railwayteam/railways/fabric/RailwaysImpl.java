/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2025 The Railways Team
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

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.config.fabric.CRConfigsImpl;
import com.railwayteam.railways.content.custom_bogeys.BogeyGaugeRuntimeChecks;
import com.railwayteam.railways.content.custom_tracks.monorail.MonorailRuntimeChecks;
import com.railwayteam.railways.content.fuel.tank.FuelRuntimeChecks;
import com.railwayteam.railways.content.palettes.painting.fabric.PaintPitcherRuntimeChecks;
import com.railwayteam.railways.content.roller_extensions.TrackReplacePaverRuntimeChecks;
import com.railwayteam.railways.content.shadow_realm.ShadowRealmRuntimeChecks;
import com.railwayteam.railways.fabric.events.CommonEventsFabric;
import com.railwayteam.railways.multiloader.CommandRegistrar;
import com.railwayteam.railways.multiloader.fabric.PlayerSelectionImpl;
import com.railwayteam.railways.registry.fabric.CRBlockEntitiesImpl;
import com.railwayteam.railways.registry.fabric.CRBlocksImpl;
import com.railwayteam.railways.registry.fabric.CRMountedStorageTypesImpl;
import com.railwayteam.railways.registry.fabric.CRParticleTypesParticleEntryImpl;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// NeoForge: run by neoforge.RailwaysNeoForge in the first RegisterEvent, while NeoForge has every registry unfrozen
public class RailwaysImpl {
	public void onInitialize() {
		RailwaysCreateRegisterPlugin.verifyEarlyRegistrationComplete();
		PlayerSelectionImpl.init();
		Railways.init();
		BogeyGaugeRuntimeChecks.register();
		MonorailRuntimeChecks.register();
		PaintPitcherRuntimeChecks.register();
		FuelRuntimeChecks.register();
		TrackReplacePaverRuntimeChecks.register();
		ShadowRealmRuntimeChecks.register();
		CRConfigsImpl.register();
		CRParticleTypesParticleEntryImpl.register();
		CommonEventsFabric.init();
	}

	public static void finalizeRegistrate() {
		Railways.registrate().register();
		Railways.postRegistrationInit();
	}

	public static void registerCommands(CommandRegistrar registrar) {
		NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> registrar.register(event.getDispatcher(),
			event.getCommandSelection() != Commands.CommandSelection.INTEGRATED, event.getBuildContext()));
	}

	public static void platformBasedRegistration() {
		CRMountedStorageTypesImpl.init();
		CRBlocksImpl.init();
		CRBlockEntitiesImpl.init();
	}
}
