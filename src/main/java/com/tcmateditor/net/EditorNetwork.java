package com.tcmateditor.net;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Network registration. Optional, so clients with the editor can still join servers without it. */
public final class EditorNetwork {
  private EditorNetwork() {}

  public static void register(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar("1").optional();
    registrar.playToServer(ApplyDataPayload.TYPE, ApplyDataPayload.CODEC, ServerApply::handleData);
    registrar.playToServer(GiveTestPayload.TYPE, GiveTestPayload.CODEC, ServerApply::handleGive);
    // the lambda keeps client classes from loading on dedicated servers
    registrar.playToClient(ApplyResultPayload.TYPE, ApplyResultPayload.CODEC, (payload, context) -> com.tcmateditor.client.ClientApply.onResult(payload));
  }
}
