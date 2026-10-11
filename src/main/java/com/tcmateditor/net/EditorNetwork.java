package com.tcmateditor.net;

import com.tcmateditor.TCMatEditor;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Network registration. Optional, so clients with the editor can still join servers without it. */
public final class EditorNetwork {
  private static final String PROTOCOL = "1";
  private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
    .named(TCMatEditor.id("editor"))
    .networkProtocolVersion(() -> PROTOCOL)
    .clientAcceptedVersions(EditorNetwork::acceptsVersion)
    .serverAcceptedVersions(EditorNetwork::acceptsVersion)
    .simpleChannel();
  private static boolean registered;

  private EditorNetwork() {}

  private static boolean acceptsVersion(String version) {
    return PROTOCOL.equals(version) || NetworkRegistry.ABSENT.equals(version) || NetworkRegistry.ACCEPTVANILLA.equals(version);
  }

  public static void register() {
    if (registered) {
      return;
    }
    registered = true;
    CHANNEL.registerMessage(0, ApplyDataPayload.class, ApplyDataPayload::encode, ApplyDataPayload::decode,
      (payload, context) -> handleServerMessage(payload, context, ServerApply::handleData), Optional.of(NetworkDirection.PLAY_TO_SERVER));
    CHANNEL.registerMessage(1, GiveTestPayload.class, GiveTestPayload::encode, GiveTestPayload::decode,
      (payload, context) -> handleServerMessage(payload, context, ServerApply::handleGive), Optional.of(NetworkDirection.PLAY_TO_SERVER));
    CHANNEL.registerMessage(2, ApplyResultPayload.class, ApplyResultPayload::encode, ApplyResultPayload::decode,
      EditorNetwork::handleResult, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
  }

  private static <T> void handleServerMessage(T payload, Supplier<NetworkEvent.Context> supplier, BiConsumer<T,ServerPlayer> handler) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) {
      context.enqueueWork(() -> handler.accept(payload, player));
    }
    context.setPacketHandled(true);
  }

  private static void handleResult(ApplyResultPayload payload, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handle(payload)));
    context.setPacketHandled(true);
  }

  public static boolean hasChannel(Connection connection) {
    return CHANNEL.isRemotePresent(connection);
  }

  public static void sendToServer(Object payload) {
    CHANNEL.sendToServer(payload);
  }

  public static void sendToPlayer(ServerPlayer player, ApplyResultPayload payload) {
    if (!player.hasDisconnected() && hasChannel(player.connection.connection)) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
  }
}
