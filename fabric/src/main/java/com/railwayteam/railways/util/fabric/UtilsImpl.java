package com.railwayteam.railways.util.fabric;

import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.infrastructure.packet.s2c.HonkReturnPacket;
import com.zurrtum.create.neoforge.Platform;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.nio.file.Path;

public class UtilsImpl {
    public static Path configDir() {
        return Platform.getConfigDir(); // NeoForge
    }

    public static boolean isDevEnv() {
        return Platform.isDevelopmentEnvironment(); // NeoForge
    }

    public static void sendHonkPacket(Level level, Train train, boolean isHonk) {
        MinecraftServer server = level.getServer();
        if (server != null) {
            server.getPlayerList().broadcastAll(new HonkReturnPacket(train, isHonk));
        }
    }

    public static void postChunkEventClient(LevelChunk chunk, boolean load) {
    }

    public static Path modsDir() {
        return FMLPaths.MODSDIR.get(); // NeoForge
    }
}
