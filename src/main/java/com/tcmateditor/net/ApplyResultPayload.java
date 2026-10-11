package com.tcmateditor.net;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Server to client: result of applying data.
 * @param messageKey  Translation key of the message
 * @param detail      Argument for the message
 * @param millis      Time taken by the reload
 */
public record ApplyResultPayload(int session, boolean success, String messageKey, String detail, int millis) {
  public static ApplyResultPayload decode(FriendlyByteBuf buffer) {
    return new ApplyResultPayload(buffer.readVarInt(), buffer.readBoolean(), buffer.readUtf(), buffer.readUtf(), buffer.readVarInt());
  }

  public void encode(FriendlyByteBuf buffer) {
    buffer.writeVarInt(session);
    buffer.writeBoolean(success);
    buffer.writeUtf(messageKey);
    buffer.writeUtf(detail);
    buffer.writeVarInt(millis);
  }
}
