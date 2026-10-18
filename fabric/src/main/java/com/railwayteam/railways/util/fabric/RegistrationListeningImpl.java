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

package com.railwayteam.railways.util.fabric;

import com.railwayteam.railways.util.RegistrationListening.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge: Fabric's RegistryEntryAddedCallback has no NeoForge counterpart. Entries registered already are reported
 * at once; the rest are checked again by {@link #flush()} after registration ends (common setup).
 */
public class RegistrationListeningImpl {
	private static final List<Listener<?>> pending = new ArrayList<>();

	public static synchronized <T> void addListener(Listener<T> listener) {
		if (!tryFire(listener))
			pending.add(listener);
	}

	public static synchronized void flush() {
		pending.removeIf(RegistrationListeningImpl::tryFire);
	}

	private static <T> boolean tryFire(Listener<T> listener) {
		if (!listener.registry().containsKey(listener.id()))
			return false;
		listener.onRegister(listener.registry().getValue(listener.id()));
		return true;
	}
}
