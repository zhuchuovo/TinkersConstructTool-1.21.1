package com.tcmateditor.client.gen;

import com.tcmateditor.model.FluidDoc;
import com.tcmateditor.model.Workspace;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Copies a compiled registration runtime into the export without requiring a Java compiler in game. */
public final class FluidJarGenerator {
  private static final String TEMPLATE = "com/tcmateditor/exporttemplate/";
  private static final List<String> CLASSES = List.of("FluidBootstrap", "FluidBootstrap$Registration", "FluidBootstrap$Client", "FluidBootstrap$Client$1");

  private FluidJarGenerator() {}

  public static Map<String,byte[]> generate(String modId, List<FluidDoc> fluids) throws IOException {
    String target = "exported/" + modId + "/";
    Map<String,byte[]> files = new LinkedHashMap<>();
    Remapper remapper = new Remapper() {
      @Override public String map(String name) {
        return name.startsWith(TEMPLATE) ? target + name.substring(TEMPLATE.length()) : name;
      }
    };
    for (String name : CLASSES) {
      try (InputStream input = FluidJarGenerator.class.getResourceAsStream("/" + TEMPLATE + name + ".class")) {
        if (input == null) throw new IOException("Missing fluid export runtime: " + name);
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(input).accept(new ClassRemapper(writer, remapper), 0);
        if (name.equals("FluidBootstrap")) {
          var annotation = writer.visitAnnotation("Lnet/neoforged/fml/common/Mod;", true);
          annotation.visit("value", modId);
          annotation.visitEnd();
        }
        files.put(target + name + ".class", writer.toByteArray());
      }
    }
    List<FluidDoc> registrations = fluids.stream().map(doc -> {
      FluidDoc copy = doc.copy();
      copy.stillTexture = doc.stillTextureId();
      copy.flowingTexture = doc.flowingTextureId();
      if (doc.generatedTexture) copy.color = "#FFFFFF";
      return copy;
    }).toList();
    files.put(target + "fluids.json", Workspace.GSON.toJson(registrations).getBytes(StandardCharsets.UTF_8));
    return files;
  }
}
