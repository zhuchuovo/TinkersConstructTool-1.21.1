package com.tcmateditor.net;

import com.tcmateditor.TCMatEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: one chunk of the gzip compressed data file map.
 * Data is chunked as serverbound custom payloads are limited to 32 KiB.
 */
public record ApplyDataPayload(int session, int index, int total, byte[] data) implements CustomPacketPayload {
  public static final int CHUNK_SIZE = 30000;
  public static final Type<ApplyDataPayload> TYPE = new Type<>(TCMatEditor.id("apply_data"));
  public static final StreamCodec<ByteBuf,ApplyDataPayload> CODEC = StreamCodec.composite(
    ByteBufCodecs.VAR_INT, ApplyDataPayload::session,
    ByteBufCodecs.VAR_INT, ApplyDataPayload::index,
    ByteBufCodecs.VAR_INT, ApplyDataPayload::total,
    ByteBufCodecs.BYTE_ARRAY, ApplyDataPayload::data,
    ApplyDataPayload::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
