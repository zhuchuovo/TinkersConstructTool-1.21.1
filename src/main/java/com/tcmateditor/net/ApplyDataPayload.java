package com.tcmateditor.net;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Client to server: one chunk of the gzip compressed data file map.
 * Data is chunked as serverbound custom payloads are limited to 32 KiB.
 */
public record ApplyDataPayload(int session, int index, int total, byte[] data) {
  public static final int CHUNK_SIZE = 30000;
  public static ApplyDataPayload decode(FriendlyByteBuf buffer) {
    return new ApplyDataPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray(CHUNK_SIZE));
  }

  public void encode(FriendlyByteBuf buffer) {
    if (data.length > CHUNK_SIZE) {
      throw new IllegalArgumentException("Editor data chunk exceeds " + CHUNK_SIZE + " bytes");
    }
    buffer.writeVarInt(session);
    buffer.writeVarInt(index);
    buffer.writeVarInt(total);
    buffer.writeByteArray(data);
  }
}
