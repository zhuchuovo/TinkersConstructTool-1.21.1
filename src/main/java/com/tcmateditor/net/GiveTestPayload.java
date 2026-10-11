package com.tcmateditor.net;

import com.tcmateditor.TCMatEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: give the player test tools made from the material */
public record GiveTestPayload(String material) implements CustomPacketPayload {
  public static final Type<GiveTestPayload> TYPE = new Type<>(TCMatEditor.id("give_test"));
  public static final StreamCodec<ByteBuf,GiveTestPayload> CODEC = StreamCodec.composite(
    ByteBufCodecs.STRING_UTF8, GiveTestPayload::material,
    GiveTestPayload::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
