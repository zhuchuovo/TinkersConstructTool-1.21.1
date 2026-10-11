package com.tcmateditor.net;

import com.tcmateditor.client.ClientApply;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Loaded only after EditorNetwork has checked the physical distribution. */
@OnlyIn(Dist.CLIENT)
final class ClientPacketHandler {
  private ClientPacketHandler() {}

  static void handle(ApplyResultPayload payload) {
    ClientApply.onResult(payload);
  }
}
