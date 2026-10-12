package com.tcmateditor.client;

import com.tcmateditor.EditorPaths;
import com.tcmateditor.client.gen.AssetGenerator;
import com.tcmateditor.gen.DataGenerator;
import com.tcmateditor.gen.DataGenerator.Problem;
import com.tcmateditor.model.Workspace;
import com.tcmateditor.pack.LivePack;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports the workspace in two forms that only use Tinkers' Construct formats and do not need this mod:
 * a code free mod jar holding data and assets, and a separate data pack plus resource pack.
 */
public final class Exporter {
  private Exporter() {}

  public record Result(Path folder, Path modJar, Path dataPack, Path resourcePack, List<Problem> problems) {}

  /** Mod jar description, data and assets in one file, loaded for every world on client and server */
  private static final String MODS_TOML = """
    modLoader="lowcodefml"
    loaderVersion="[47,)"
    license="All Rights Reserved"

    [[mods]]
    modId="%1$s"
    version="1.0.0"
    displayName="Tinkers' Materials (%2$s)"
    description='''
    %3$s
    Made with Tinkers' Construct Tool, does not need it.
    '''

    [[dependencies.%1$s]]
    modId="tconstruct"
    mandatory=true
    versionRange="[3,)"
    ordering="AFTER"
    side="BOTH"

    [[dependencies.%1$s]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.20.1,1.20.2)"
    ordering="NONE"
    side="BOTH"
    """;

  public static Result export(Workspace workspace) throws IOException {
    workspace.save();
    Path folder = EditorPaths.exports();
    Files.createDirectories(folder);
    String name = "tinkers_materials_" + workspace.namespace;

    List<Problem> problems = new ArrayList<>();
    Map<String,String> data = DataGenerator.generate(workspace, problems);
    Map<String,byte[]> assets = AssetGenerator.generate(workspace);
    String summary = workspace.materials.size() + " materials, " + workspace.entries.size() + " entries, " + workspace.upgrades.size() + " upgrades.";

    // one file for the mods folder
    String modId = modId(workspace.namespace);
    Path modJar = folder.resolve(name + ".jar");
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(new Attributes.Name("Automatic-Module-Name"), modId);
    try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(modJar), manifest)) {
      put(jar, "META-INF/mods.toml", MODS_TOML.formatted(modId, workspace.namespace, summary).getBytes(StandardCharsets.UTF_8));
      put(jar, "pack.mcmeta", LivePack.mcmeta("Tinkers' Construct materials (" + workspace.namespace + ")", 15).getBytes(StandardCharsets.UTF_8));
      putAll(jar, data, assets);
    }

    // or a data pack and a resource pack
    Path dataZip = folder.resolve(name + "-datapack.zip");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(dataZip))) {
      put(zip, "pack.mcmeta", LivePack.mcmeta("Tinkers' Construct materials (" + workspace.namespace + ")", 15).getBytes(StandardCharsets.UTF_8));
      putAll(zip, data, Map.of());
    }
    Path resourceZip = folder.resolve(name + "-resourcepack.zip");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(resourceZip))) {
      put(zip, "pack.mcmeta", LivePack.mcmeta("Tinkers' Construct material textures (" + workspace.namespace + ")", 15).getBytes(StandardCharsets.UTF_8));
      putAll(zip, Map.of(), assets);
    }

    Path readme = folder.resolve("README.txt");
    try (OutputStream out = Files.newOutputStream(readme)) {
      out.write(("""
        Tinkers' Construct Tool export
        ===============================
        Use ONE of the two options. Both only use Tinkers' Construct formats and work without Tinkers' Construct Tool.

        A) %1$s.jar
           Put it into the mods folder (client and server). Contains everything, works in every world.

        B) %1$s-datapack.zip      -> <world>/datapacks  (server: world/datapacks)
           %1$s-resourcepack.zip  -> resourcepacks, then enable it in Options > Resource Packs
           The resource pack does nothing inside datapacks: without it textures and names are missing.

        Tinkers' 材料编辑器导出
        两种方式任选一种，都只使用匠魂的标准格式，卸载本编辑器后依然可用。

        A) %1$s.jar
           放进 mods 文件夹（客户端和服务端都放），一个文件包含全部内容，所有存档通用。

        B) %1$s-datapack.zip      -> 存档/datapacks（服务器：world/datapacks）
           %1$s-resourcepack.zip  -> resourcepacks，并在「选项 > 资源包」中启用
           资源包放进 datapacks 不会生效，缺少它会导致纹理和名称丢失。
        """).formatted(name).getBytes(StandardCharsets.UTF_8));
    }
    return new Result(folder, modJar, dataZip, resourceZip, problems);
  }

  /** Mod ID for the exported jar, mod IDs only allow lowercase letters, digits and underscores */
  static String modId(String namespace) {
    String id = "tinkers_materials_" + namespace.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    return id.length() > 64 ? id.substring(0, 64) : id;
  }

  private static void putAll(ZipOutputStream zip, Map<String,String> data, Map<String,byte[]> assets) throws IOException {
    for (Map.Entry<String,String> entry : data.entrySet()) {
      put(zip, entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8));
    }
    for (Map.Entry<String,byte[]> entry : assets.entrySet()) {
      put(zip, entry.getKey(), entry.getValue());
    }
  }

  private static void put(ZipOutputStream zip, String path, byte[] content) throws IOException {
    zip.putNextEntry(new ZipEntry(path));
    zip.write(content);
    zip.closeEntry();
  }
}
