package dev.rinchan.rinlibprobe;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.UUID;

public final class ProbePlayers {
    public static ServerPlayer create(MinecraftServer server, String name, long id) {
        var profile = new GameProfile(new UUID(0L, id), name);
        var player =
                new ServerPlayer(
                        server, server.overworld(), profile, ClientInformation.createDefault());
        player.connection =
                new ServerGamePacketListenerImpl(
                        server,
                        new Connection(PacketFlow.SERVERBOUND),
                        player,
                        CommonListenerCookie.createInitial(profile, false));
        player.setPos(0.5, 81, 1.5);
        return player;
    }
}
