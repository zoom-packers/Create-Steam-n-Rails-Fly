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

import com.railwayteam.railways.fabric.RailwaysImpl;
import net.neoforged.bus.api.IEventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * NeoForge: runs the Fabric initializer during vanilla bootstrap (see {@link RailwaysNeoForge}). Code that needs the
 * mod event bus, which does not exist yet at that point, queues a task in {@link #onModBus}.
 */
public final class RailwaysNeoForgeInit {
	private static final List<Consumer<IEventBus>> MOD_BUS_TASKS = new ArrayList<>();
	private static IEventBus modBus;
	private static boolean initialized;

	private RailwaysNeoForgeInit() {
	}

	public static void initialize() {
		if (initialized)
			return;
		initialized = true;
		new RailwaysImpl().onInitialize();
	}

	static boolean isInitialized() {
		return initialized;
	}

	/**
	 * Runs the task with Railways' mod event bus: right away once {@link RailwaysNeoForge} has handed the bus over,
	 * otherwise when it does. The client entrypoint can be constructed before or after the common one.
	 */
	public static void onModBus(Consumer<IEventBus> task) {
		if (modBus != null)
			task.accept(modBus);
		else
			MOD_BUS_TASKS.add(task);
	}

	static void bindModBus(IEventBus bus) {
		modBus = bus;
		MOD_BUS_TASKS.forEach(task -> task.accept(bus));
		MOD_BUS_TASKS.clear();
	}
}
