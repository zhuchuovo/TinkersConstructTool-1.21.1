package com.tcmateditor.client;

import com.google.gson.JsonParser;
import com.tcmateditor.client.gen.AssetGenerator;
import com.tcmateditor.client.gen.FluidJarGenerator;
import com.tcmateditor.model.FluidDoc;
import com.tcmateditor.model.LangText;
import com.tcmateditor.model.Workspace;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.IModBusEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class FluidExportTest {
  @TempDir Path folder;

  private static Workspace workspace() {
    Workspace workspace = new Workspace();
    FluidDoc doc = new FluidDoc();
    doc.id = "custommats:molten_ruby";
    doc.color = "#CC2233";
    doc.lang.put("zh_cn", new LangText("熔融红宝石", "", ""));
    doc.lang.put("en_us", new LangText("Molten Ruby", "", ""));
    workspace.fluids.add(doc);
    return workspace;
  }

  @Test
  void jarContainsRuntimeAndPacksDoNotPretendToRegisterFluids() throws Exception {
    Workspace workspace = workspace();
    Exporter.Result result = Exporter.export(workspace, folder, "zh_cn");
    assertTrue(result.problems().isEmpty());
    assertTrue(Files.readString(result.fluidScript()).contains("// custommats:molten_ruby 熔融红宝石"));
    try (ZipFile jar = new ZipFile(result.modJar().toFile()); ZipFile data = new ZipFile(result.dataPack().toFile())) {
      String toml = new String(jar.getInputStream(jar.getEntry("META-INF/neoforge.mods.toml")).readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(toml.contains("modLoader=\"javafml\""));
      assertNotNull(jar.getEntry("exported/tinkers_materials_custommats/FluidBootstrap.class"));
      assertNotNull(jar.getEntry("exported/tinkers_materials_custommats/fluids.json"));
      assertNotNull(jar.getEntry("assets/custommats/models/item/molten_ruby_bucket.json"));
      assertTrue(data.stream().noneMatch(entry -> entry.getName().endsWith(".class") || entry.getName().endsWith("fluids.json")));
    }
    workspace.fluids.clear();
    Exporter.Result empty = Exporter.export(workspace, folder, "en_us");
    assertNull(empty.fluidScript());
    assertFalse(Files.exists(result.fluidScript()), "old scripts must not keep registering deleted fluids");
    try (ZipFile jar = new ZipFile(empty.modJar().toFile())) {
      String toml = new String(jar.getInputStream(jar.getEntry("META-INF/neoforge.mods.toml")).readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(toml.contains("modLoader=\"lowcodefml\""));
      assertTrue(jar.stream().noneMatch(entry -> entry.getName().endsWith(".class")));
    }
  }

  @Test
  void localizedFluidAssetsMatchScriptNamesAndLiveReloadSkipsUnknownModels() {
    Workspace workspace = workspace();
    Map<String,byte[]> files = AssetGenerator.generateForExport(workspace);
    var texts = JsonParser.parseString(new String(files.get("assets/custommats/lang/zh_cn.json"), StandardCharsets.UTF_8)).getAsJsonObject();
    assertEquals("熔融红宝石", texts.get("fluid.custommats.molten_ruby").getAsString());
    assertEquals("熔融红宝石桶", texts.get("item.custommats.molten_ruby_bucket").getAsString());
    assertFalse(AssetGenerator.generate(workspace).containsKey("assets/custommats/models/item/molten_ruby_bucket.json"));
  }

  @Test
  void generatedTexturesIncludeAnimationAndKubeJsInstallAssets() throws Exception {
    Workspace workspace = workspace();
    FluidDoc doc = workspace.fluids.getFirst();
    doc.generatedTexture = true;
    doc.texture = FluidDoc.createTexture(doc.baseColorInt());
    Exporter.Result result = Exporter.export(workspace, folder, "zh_cn");
    assertNotNull(result.kubeAssets());
    String still = "assets/custommats/textures/block/fluid/molten_ruby_still.png";
    String flow = "assets/custommats/textures/block/fluid/molten_ruby_flow.png";
    try (ZipFile jar = new ZipFile(result.modJar().toFile()); ZipFile assets = new ZipFile(result.kubeAssets().toFile())) {
      assertNotNull(jar.getEntry(still));
      assertNotNull(jar.getEntry(flow));
      assertNotNull(assets.getEntry(still));
      try (ZipFile resourcePack = new ZipFile(result.resourcePack().toFile())) {
        for (boolean flowing : new boolean[] {false, true}) {
          String source = flowing ? flow : still;
          String kubeTexture = AssetGenerator.kubeFluidTexturePath(doc, flowing);
          for (String suffix : new String[] {"", ".mcmeta"}) {
            byte[] expected = jar.getInputStream(jar.getEntry(source + suffix)).readAllBytes();
            assertArrayEquals(expected, jar.getInputStream(jar.getEntry(kubeTexture + suffix)).readAllBytes());
            assertArrayEquals(expected, resourcePack.getInputStream(resourcePack.getEntry(kubeTexture + suffix)).readAllBytes());
            assertArrayEquals(expected, assets.getInputStream(assets.getEntry(kubeTexture + suffix)).readAllBytes());
          }
        }
      }
      var meta = JsonParser.parseString(new String(assets.getInputStream(assets.getEntry(flow + ".mcmeta")).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animation");
      assertEquals(32, meta.get("height").getAsInt());
      var registrations = JsonParser.parseString(new String(jar.getInputStream(jar.getEntry("exported/tinkers_materials_custommats/fluids.json")).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonArray();
      assertEquals(doc.stillTextureId(), registrations.get(0).getAsJsonObject().get("stillTexture").getAsString());
      assertEquals("#FFFFFF", registrations.get(0).getAsJsonObject().get("color").getAsString());
    }
    String script = Files.readString(result.fluidScript());
    assertTrue(script.contains(".stillTexture(\"custommats:block/fluid/molten_ruby_still\")"));
    assertTrue(script.contains(".tint(0xFFFFFFFF)"));
  }

  @Test
  void startupRepairsModelsAndKubeTexturesSavedBeforeRegistration() throws Exception {
    Workspace workspace = workspace();
    FluidDoc doc = workspace.fluids.getFirst();
    doc.id = "minecraft:water"; // An already registered fluid stands in for a newly installed startup script.
    doc.generatedTexture = true;
    doc.validate();
    Map<String,byte[]> assets = AssetGenerator.generateForExport(workspace);
    Path pack = folder.resolve("live_pack");
    // Older live packs contain only the input PNGs and metadata, with no models or KubeJS aliases.
    for (var entry : assets.entrySet()) {
      if (!entry.getKey().startsWith("assets/minecraft/textures/block/fluid/")) continue;
      Path target = pack.resolve(entry.getKey());
      Files.createDirectories(target.getParent());
      Files.write(target, entry.getValue());
    }
    ClientApply.prepareFluidAssets(workspace, pack);
    var bucket = JsonParser.parseString(Files.readString(pack.resolve("assets/minecraft/models/item/water_bucket.json"))).getAsJsonObject();
    assertEquals("tconstruct:fluid_container", bucket.get("loader").getAsString());
    assertEquals("minecraft:water", bucket.get("fluid").getAsString());
    assertTrue(Files.exists(pack.resolve("assets/minecraft/blockstates/water.json")));
    for (boolean flowing : new boolean[] {false, true}) {
      String source = "assets/minecraft/textures/block/fluid/water" + (flowing ? "_flow" : "_still") + ".png";
      String alias = AssetGenerator.kubeFluidTexturePath(doc, flowing);
      assertArrayEquals(assets.get(source), Files.readAllBytes(pack.resolve(alias)));
      assertArrayEquals(assets.get(source + ".mcmeta"), Files.readAllBytes(pack.resolve(alias + ".mcmeta")));
      Files.delete(pack.resolve(source + ".mcmeta"));
    }
    ClientApply.prepareFluidAssets(workspace, pack);
    assertFalse(Files.exists(pack.resolve(AssetGenerator.kubeFluidTexturePath(doc, false) + ".mcmeta")));
    assertFalse(Files.exists(pack.resolve(AssetGenerator.kubeFluidTexturePath(doc, true) + ".mcmeta")));

    doc.id = "custommats:unregistered";
    assertTrue(AssetGenerator.registeredFluidModels(workspace).isEmpty());
    assertFalse(AssetGenerator.generate(workspace).containsKey("assets/custommats/models/item/unregistered_bucket.json"));
    assertNotNull(AssetGenerator.generate(workspace).get(AssetGenerator.kubeFluidTexturePath(doc, true)));
  }

  @Test
  void exportedBootstrapLoadsWithoutEditorClassesAndRegistersOnModBus() throws Exception {
    Map<String,byte[]> runtime = FluidJarGenerator.generate("tinkers_materials_test", workspace().fluids);
    ClassLoader isolated = new ClassLoader(getClass().getClassLoader()) {
      @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("com.tcmateditor.")) throw new ClassNotFoundException("Editor is not installed: " + name);
        return super.loadClass(name, resolve);
      }
      @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = runtime.get(name.replace('.', '/') + ".class");
        if (bytes == null) throw new ClassNotFoundException(name);
        return defineClass(name, bytes, 0, bytes.length);
      }
      @Override public InputStream getResourceAsStream(String name) {
        byte[] bytes = runtime.get(name);
        return bytes == null ? super.getResourceAsStream(name) : new ByteArrayInputStream(bytes);
      }
    };
    Class<?> bootstrap = isolated.loadClass("exported.tinkers_materials_test.FluidBootstrap");
    assertEquals("tinkers_materials_test", bootstrap.getAnnotation(Mod.class).value());
    IEventBus bus = BusBuilder.builder().markerType(IModBusEvent.class).build();
    assertNotNull(bootstrap.getConstructor(IEventBus.class).newInstance(bus));
  }
}
