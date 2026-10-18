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

package com.railwayteam.railways.neoforge.mixin;

import com.railwayteam.railways.neoforge.RailwaysNeoForgeInit;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge: Create Fly's NeoForge port registers its content at the head of {@code BuiltInRegistries.freeze()} during
 * vanilla bootstrap, before NeoForge freezes the registries and constructs mods. Railways registers right after it,
 * just before the first registry is frozen, so its items, block entities and entities exist (and can use Create's)
 * before any mod constructor runs, as on Fabric.
 * <p>
 * This cannot target {@code Create.register()} itself: Create's mixin plugin loads the {@code Create} class before
 * other mods' mixins are prepared.
 */
@Mixin(BuiltInRegistries.class)
public class BuiltInRegistriesMixin {
	@Inject(
		method = "freeze()V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;freeze()Lnet/minecraft/core/Registry;", ordinal = 0)
	)
	private static void railways$register(CallbackInfo ci) {
		RailwaysNeoForgeInit.initialize();
	}
}
