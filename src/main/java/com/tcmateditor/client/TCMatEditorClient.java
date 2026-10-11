package com.tcmateditor.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tcmateditor.TCMatEditor;
import com.tcmateditor.client.gui.EditorScreen;
import com.tcmateditor.client.texture.PartTextureLibrary;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Client entry point: Shift+Z opens the editor */
@Mod(value = TCMatEditor.MOD_ID, dist = Dist.CLIENT)
public final class TCMatEditorClient {
  public static final KeyMapping OPEN_EDITOR = new KeyMapping("key.tcmateditor.open", KeyConflictContext.IN_GAME, KeyModifier.SHIFT,
    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.tcmateditor");

  public TCMatEditorClient(IEventBus modBus, ModContainer container) {
    modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(OPEN_EDITOR));
    modBus.addListener(RegisterClientReloadListenersEvent.class, event ->
      event.registerReloadListener((ResourceManagerReloadListener) manager -> PartTextureLibrary.clear()));
    NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> onTick());
    NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> ClientApply.reset());
  }

  private static void onTick() {
    Minecraft minecraft = Minecraft.getInstance();
    while (OPEN_EDITOR.consumeClick()) {
      if (minecraft.screen == null && minecraft.player != null) {
        minecraft.setScreen(new EditorScreen());
      }
    }
    ClientApply.tick();
    if (com.tcmateditor.client.dev.SelfTest.ENABLED) {
      com.tcmateditor.client.dev.SelfTest.tick();
    }
  }
}
