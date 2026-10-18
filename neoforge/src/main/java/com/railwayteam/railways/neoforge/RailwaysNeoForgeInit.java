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
	static final List<Consumer<IEventBus>> MOD_BUS_TASKS = new ArrayList<>();
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

	/** Runs the task with Railways' mod event bus once the mod is constructed. */
	public static void onModBus(Consumer<IEventBus> task) {
		MOD_BUS_TASKS.add(task);
	}
}
