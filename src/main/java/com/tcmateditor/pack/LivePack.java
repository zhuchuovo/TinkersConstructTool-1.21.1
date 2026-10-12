package com.tcmateditor.pack;

import com.tcmateditor.EditorPaths;
import com.tcmateditor.TCMatEditor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The live pack is a normal folder pack containing both {@code data/} and {@code assets/}.
 * While this mod is installed it is always enabled on top, which is what makes hot reloading work.
 * Exported copies of the same files work without this mod.
 */
public final class LivePack {
  public static final String PACK_ID = TCMatEditor.MOD_ID + "/live";
  /** Minecraft 1.20.1 uses format 15 for resource and data packs. */
  public static final String MCMETA = """
    {
      "pack": {
        "description": "%s",
        "pack_format": %d
      }
    }
    """;

  private LivePack() {}

  /** Creates the folder and pack.mcmeta, must exist before pack finders run */
  public static void ensureExists() {
    try {
      Path root = EditorPaths.livePack();
      Files.createDirectories(root);
      Path mcmeta = root.resolve("pack.mcmeta");
      if (!Files.exists(mcmeta)) {
        Files.writeString(mcmeta, mcmeta("Tinkers' Construct Tool live pack", 15), StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      TCMatEditor.LOG.error("Failed to create live pack folder", e);
    }
  }

  public static String mcmeta(String description, int format) {
    return MCMETA.formatted(description.replace("\"", "'"), format);
  }

  public static void addPackFinders(AddPackFindersEvent event) {
    ensureExists();
    Path root = EditorPaths.livePack();
    Pack pack = Pack.readMetaAndCreate(PACK_ID, Component.literal("Tinkers' Construct Tool"), true,
      id -> new PathPackResources(id, root, false), event.getPackType(), Pack.Position.TOP, PackSource.BUILT_IN);
    if (pack != null) {
      event.addRepositorySource(consumer -> consumer.accept(pack));
    } else {
      TCMatEditor.LOG.error("Unable to create live pack from {}", root);
    }
  }
}
