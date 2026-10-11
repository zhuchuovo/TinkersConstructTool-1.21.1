package com.tcmateditor.net;

import net.minecraft.network.FriendlyByteBuf;

/** Client to server: give the player test tools made from the material */
public record GiveTestPayload(String material) {
  public static GiveTestPayload decode(FriendlyByteBuf buffer) {
    return new GiveTestPayload(buffer.readUtf(256));
  }

  public void encode(FriendlyByteBuf buffer) {
    buffer.writeUtf(material, 256);
  }
}
