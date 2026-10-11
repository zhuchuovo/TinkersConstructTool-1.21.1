package com.tcmateditor.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tcmateditor.TCMatEditor;
import com.tcmateditor.client.gui.EditorScreen;
import com.tcmateditor.client.texture.PartTextureLibrary;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Client entry point: Shift+Z opens the editor */
@Mod.EventBusSubscriber(modid = TCMatEditor.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TCMatEditorClient {
  public static final KeyMapping OPEN_EDITOR = new KeyMapping("key.tcmateditor.open", KeyConflictContext.IN_GAME, KeyModifier.SHIFT,
    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.tcmateditor");

  @SubscribeEvent
  public static void registerKeys(RegisterKeyMappingsEvent event) {
    event.register(OPEN_EDITOR);
  }

  @SubscribeEvent
  public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
    event.registerReloadListener((ResourceManagerReloadListener) manager -> PartTextureLibrary.clear());
  }

  @Mod.EventBusSubscriber(modid = TCMatEditor.MOD_ID, value = Dist.CLIENT)
  public static final class GameEvents {
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
      if (event.phase == TickEvent.Phase.END) onTick();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
      ClientApply.reset();
    }
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
