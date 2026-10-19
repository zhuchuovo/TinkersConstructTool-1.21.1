package com.tcmateditor.client;

import com.tcmateditor.EditorPaths;
import com.tcmateditor.client.gen.AssetGenerator;
import com.tcmateditor.client.gen.FluidJarGenerator;
import com.tcmateditor.gen.DataGenerator;
import com.tcmateditor.gen.DataGenerator.Problem;
import com.tcmateditor.gen.FluidGenerator;
import com.tcmateditor.model.Workspace;
import com.tcmateditor.pack.LivePack;
import net.minecraft.client.Minecraft;

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
 * Exports a standalone mod jar and separate packs using Tinkers' Construct formats.
 * New fluids include a registration runtime in the jar and one alternative KubeJS startup script.
 */
public final class Exporter {
  private Exporter() {}

  public record Result(Path folder, Path modJar, Path dataPack, Path resourcePack, Path fluidScript, Path kubeAssets, List<Problem> problems) {}

  /** Mod jar description, data and assets in one file, loaded for every world on client and server */
  private static final String MODS_TOML = """
    modLoader="lowcodefml"
    loaderVersion="[1,)"
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
    type="required"
    versionRange="[3,)"
    ordering="AFTER"
    side="BOTH"

    [[dependencies.%1$s]]
    modId="minecraft"
    type="required"
    versionRange="[1.21.1,1.21.2)"
    ordering="NONE"
    side="BOTH"
    """;

  public static Result export(Workspace workspace) throws IOException {
    workspace.save();
    return export(workspace, EditorPaths.exports(), Minecraft.getInstance().getLanguageManager().getSelected());
  }

  static Result export(Workspace workspace, Path folder, String language) throws IOException {
    Files.createDirectories(folder);
    String name = "tinkers_materials_" + workspace.namespace;

    List<Problem> problems = new ArrayList<>();
    Workspace exportWorkspace = new Workspace();
    exportWorkspace.namespace = workspace.namespace;
    exportWorkspace.materials.addAll(workspace.materials);
    exportWorkspace.entries.addAll(workspace.entries);
    exportWorkspace.upgrades.addAll(workspace.upgrades);
    exportWorkspace.fluids.addAll(FluidGenerator.validFluids(workspace, problems));
    Map<String,String> data = DataGenerator.generateForExport(exportWorkspace, problems);
    Map<String,byte[]> assets = AssetGenerator.generateForExport(exportWorkspace);
    boolean hasFluids = !exportWorkspace.fluids.isEmpty();
    String summary = workspace.materials.size() + " materials, " + workspace.entries.size() + " entries, " + workspace.upgrades.size() + " upgrades, " + exportWorkspace.fluids.size() + " fluids.";

    // one file for the mods folder
    String modId = modId(workspace.namespace);
    Map<String,byte[]> runtime = hasFluids ? FluidJarGenerator.generate(modId, exportWorkspace.fluids) : Map.of();
    Path modJar = folder.resolve(name + ".jar");
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(new Attributes.Name("Automatic-Module-Name"), modId);
    try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(modJar), manifest)) {
      String metadata = MODS_TOML.formatted(modId, workspace.namespace, summary);
      if (hasFluids) metadata = metadata.replace("modLoader=\"lowcodefml\"", "modLoader=\"javafml\"").replace("loaderVersion=\"[1,)\"", "loaderVersion=\"[4,)\"");
      put(jar, "META-INF/neoforge.mods.toml", metadata.getBytes(StandardCharsets.UTF_8));
      put(jar, "pack.mcmeta", LivePack.mcmeta("Tinkers' Construct materials (" + workspace.namespace + ")", 48).getBytes(StandardCharsets.UTF_8));
      putAll(jar, data, assets);
      putAll(jar, Map.of(), runtime);
    }

    // or a data pack and a resource pack
    Path dataZip = folder.resolve(name + "-datapack.zip");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(dataZip))) {
      put(zip, "pack.mcmeta", LivePack.mcmeta("Tinkers' Construct materials (" + workspace.namespace + ")", 48).getBytes(StandardCharsets.UTF_8));
      putAll(zip, data, Map.of());
    }
    Path resourceZip = folder.resolve(name + "-resourcepack.zip");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(resourceZip))) {
      put(zip, "pack.mcmeta", LivePack.mcmeta("Tinkers' Construct material textures (" + workspace.namespace + ")", 34).getBytes(StandardCharsets.UTF_8));
      putAll(zip, Map.of(), assets);
    }

    Path script = folder.resolve(name + "-fluids.js");
    if (hasFluids) {
      Files.writeString(script, FluidGenerator.script(exportWorkspace.fluids, language), StandardCharsets.UTF_8);
    } else {
      Files.deleteIfExists(script);
      script = null;
    }

    Path kubeAssets = folder.resolve(name + "-kubejs-assets.zip");
    if (exportWorkspace.fluids.stream().anyMatch(doc -> doc.generatedTexture)) {
      try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(kubeAssets))) {
        putAll(zip, Map.of(), assets);
      }
    } else {
      Files.deleteIfExists(kubeAssets);
      kubeAssets = null;
    }

    Path readme = folder.resolve("README.txt");
    try (OutputStream out = Files.newOutputStream(readme)) {
      out.write(("""
        Tinkers' Construct Tool export
        ===============================
        Use ONE of the options below. All work without Tinkers' Construct Tool.

        A) %1$s.jar
           Put it into the mods folder (client and server). Contains everything, works in every world.

        B) %1$s-datapack.zip      -> <world>/datapacks  (server: world/datapacks)
           %1$s-resourcepack.zip  -> resourcepacks, then enable it in Options > Resource Packs
           The resource pack does nothing inside datapacks: without it textures and names are missing.

           If the workspace has new fluids, also copy %1$s-fluids.js into kubejs/startup_scripts.
           The script registers ALL fluids in one file and requires KubeJS for Minecraft 1.21.1.
           New fluids cannot be registered by a data pack. Restart the game/server after installing.
           With option A the jar registers fluids itself: KubeJS is not required.
           Use either the jar OR the script to register fluids; installing both duplicates registry IDs.
           Custom texture IDs require their textures in the target instance as well.
           Generated fluid textures: unzip %1$s-kubejs-assets.zip into <game>/kubejs.
           This places assets/<namespace>/... inside kubejs/assets before KubeJS builds its fluid textures.

        Tinkers' 材料编辑器导出
        两种方式任选一种，卸载本编辑器后依然可用。

        A) %1$s.jar
           放进 mods 文件夹（客户端和服务端都放），一个文件包含全部内容，所有存档通用。

        B) %1$s-datapack.zip      -> 存档/datapacks（服务器：world/datapacks）
           %1$s-resourcepack.zip  -> resourcepacks，并在「选项 > 资源包」中启用
           资源包放进 datapacks 不会生效，缺少它会导致纹理和名称丢失。

           如有新流体，还需将 %1$s-fluids.js 放入 kubejs/startup_scripts（KubeJS 1.21.1）。
           全部流体注册集中在一个脚本中。流体无法导出为数据包，只能导出到kubejs注册脚本或jar模组。
           方式 A 的 JAR 自带流体注册，不需要 KubeJS；JAR 和脚本不可同时安装，以免重复注册。
           安装后需重启游戏和服务器。自定义纹理也需存在于目标游戏中。
           使用生成的流体纹理时，将 %1$s-kubejs-assets.zip 解压到 <游戏目录>/kubejs。
           解压后纹理位于 kubejs/assets/<命名空间>/...，KubeJS 才能生成流体和桶的纹理。
        """).formatted(name).getBytes(StandardCharsets.UTF_8));
    }
    return new Result(folder, modJar, dataZip, resourceZip, script, kubeAssets, problems);
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
