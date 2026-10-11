package com.tcmateditor;

import com.tcmateditor.net.EditorNetwork;
import com.tcmateditor.pack.LivePack;
import com.tcmateditor.recipe.EditorRecipes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tinkers' Material Editor.
 * <p>
 * The mod is only an editor: every material it creates is written as plain Tinkers' Construct
 * data pack / resource pack content, so exported materials work without this mod installed.
 */
@Mod(TCMatEditor.MOD_ID)
public final class TCMatEditor {
  public static final String MOD_ID = "tcmateditor";
  public static final Logger LOG = LoggerFactory.getLogger("TCMatEditor");

  public TCMatEditor(IEventBus modBus, ModContainer container) {
    LivePack.ensureExists();
    modBus.addListener(LivePack::addPackFinders);
    modBus.addListener(EditorNetwork::register);
    EditorRecipes.register(modBus);
  }

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
  }

  public static String key(String path) {
    return MOD_ID + "." + path;
  }
}
