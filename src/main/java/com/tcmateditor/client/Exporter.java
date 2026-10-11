package com.tcmateditor.client;

import com.tcmateditor.EditorPaths;
import com.tcmateditor.client.gen.AssetGenerator;
import com.tcmateditor.gen.DataGenerator;
import com.tcmateditor.model.Workspace;
import com.tcmateditor.pack.LivePack;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports the workspace as a standalone data pack and resource pack.
 * The exported packs only use Tinkers' Construct formats and do not need this mod.
 */
public final class Exporter {
  private Exporter() {}

  public record Result(Path folder, Path dataPack, Path resourcePack) {}

  public static Result export(Workspace workspace) throws IOException {
    workspace.save();
    Path folder = EditorPaths.exports();
    Files.createDirectories(folder);
    String name = "tinkers_materials_" + workspace.namespace;

    Map<String,String> data = DataGenerator.generate(workspace);
    Path dataZip = folder.resolve(name + "-datapack.zip");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(dataZip))) {
      put(zip, "pack.mcmeta", LivePack.mcmeta("Tinkers' Construct materials (" + workspace.namespace + ")", 48).getBytes(StandardCharsets.UTF_8));
      for (Map.Entry<String,String> entry : data.entrySet()) {
        put(zip, entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8));
      }
    }

    Map<String,byte[]> assets = AssetGenerator.generate(workspace);
    Path resourceZip = folder.resolve(name + "-resourcepack.zip");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(resourceZip))) {
      put(zip, "pack.mcmeta", LivePack.mcmeta("Tinkers' Construct material textures (" + workspace.namespace + ")", 34).getBytes(StandardCharsets.UTF_8));
      for (Map.Entry<String,byte[]> entry : assets.entrySet()) {
        put(zip, entry.getKey(), entry.getValue());
      }
    }

    Path readme = folder.resolve("README.txt");
    try (OutputStream out = Files.newOutputStream(readme)) {
      out.write(("""
        Tinkers' Material Editor export
        ================================
        %1$s-datapack.zip      -> put into <world>/datapacks  (or the server's world/datapacks)
        %1$s-resourcepack.zip  -> put into resourcepacks and enable it

        Both packs only use Tinkers' Construct formats. They work without Tinkers' Material Editor.

        Tinkers' 材料编辑器导出
        datapack.zip     -> 放入 存档/datapacks
        resourcepack.zip -> 放入 resourcepacks 并启用
        两个包只使用匠魂的标准格式，卸载本编辑器后依然可用。
        """).formatted(name).getBytes(StandardCharsets.UTF_8));
    }
    return new Result(folder, dataZip, resourceZip);
  }

  private static void put(ZipOutputStream zip, String path, byte[] content) throws IOException {
    zip.putNextEntry(new ZipEntry(path));
    zip.write(content);
    zip.closeEntry();
  }
}
