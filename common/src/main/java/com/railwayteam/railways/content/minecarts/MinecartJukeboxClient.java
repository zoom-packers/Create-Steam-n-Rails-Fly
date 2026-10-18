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

package com.railwayteam.railways.content.minecarts;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.JukeboxSong;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge (ZP5 port): the jukebox cart's client-only sound handling, moved out of {@link MinecartJukebox} so the
 * entity class loads on dedicated servers (Fabric stripped these members there; NeoForge does not).
 */
@Environment(EnvType.CLIENT)
public final class MinecartJukeboxClient {
    private MinecartJukeboxClient() {
    }

    /** Stops the cart's current sound and starts the inserted disc's song, if any; returns the new sound handle. */
    static @Nullable Object updateSound(MinecartJukebox cart, @Nullable Object handle) {
        if (handle instanceof JukeboxCartSoundInstance sound && !sound.isStopped())
            sound.requestStop();
        if (cart.getDisc().isEmpty())
            return null;

        JukeboxSong song = JukeboxSong.fromStack(cart.getDisc()).map(Holder::value).orElse(null);
        if (song == null)
            return null;
        JukeboxCartSoundInstance sound = new JukeboxCartSoundInstance(cart, song);
        Minecraft.getInstance().getSoundManager().play(sound);
        return sound;
    }

    public static class JukeboxCartSoundInstance extends AbstractTickableSoundInstance {
        private final MinecartJukebox cart;

        public JukeboxCartSoundInstance(MinecartJukebox cart, JukeboxSong song) {
            super(song.soundEvent().value(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.cart = cart;
            this.x = cart.getX();
            this.y = cart.getY();
            this.z = cart.getZ();
        }

        @Override
        public void tick() {
            if (cart.isRemoved())
                requestStop();

            this.x = cart.getX();
            this.y = cart.getY();
            this.z = cart.getZ();
        }

        public void requestStop() {
            stop();
        }
    }
}
