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

package com.railwayteam.railways.registry.fabric;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import java.util.ArrayList;
import java.util.List;

public class CRKeysImpl {
    // NeoForge: key mappings are registered in RegisterKeyMappingsEvent (RailwaysClientImpl)
    private static final List<KeyMapping> MAPPINGS = new ArrayList<>();

    public static void registerKeyBinding(KeyMapping keyMapping) {
        MAPPINGS.add(keyMapping);
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        MAPPINGS.forEach(event::register);
    }

    public static int getBoundCode(KeyMapping keyMapping) {
        return keyMapping.getKey().getValue();
    }
}
