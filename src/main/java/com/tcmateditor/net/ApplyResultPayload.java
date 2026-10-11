package com.tcmateditor.net;

import com.tcmateditor.TCMatEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: result of applying data.
 * @param messageKey  Translation key of the message
 * @param detail      Argument for the message
 * @param millis      Time taken by the reload
 */
public record ApplyResultPayload(int session, boolean success, String messageKey, String detail, int millis) implements CustomPacketPayload {
  public static final Type<ApplyResultPayload> TYPE = new Type<>(TCMatEditor.id("apply_result"));
  public static final StreamCodec<ByteBuf,ApplyResultPayload> CODEC = StreamCodec.composite(
    ByteBufCodecs.VAR_INT, ApplyResultPayload::session,
    ByteBufCodecs.BOOL, ApplyResultPayload::success,
    ByteBufCodecs.STRING_UTF8, ApplyResultPayload::messageKey,
    ByteBufCodecs.STRING_UTF8, ApplyResultPayload::detail,
    ByteBufCodecs.VAR_INT, ApplyResultPayload::millis,
    ApplyResultPayload::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
