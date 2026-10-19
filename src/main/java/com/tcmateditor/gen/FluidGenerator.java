package com.tcmateditor.gen;

import com.tcmateditor.gen.DataGenerator.Problem;
import com.tcmateditor.model.FluidDoc;
import com.tcmateditor.model.Workspace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** KubeJS 7 (Minecraft 1.21.1) startup registration script. */
public final class FluidGenerator {
  private FluidGenerator() {}

  /** Prevent source/flowing ID collisions from failing the whole registry at startup. */
  public static List<FluidDoc> validFluids(Workspace workspace, List<Problem> problems) {
    List<FluidDoc> fluids = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    for (FluidDoc original : workspace.fluids) {
      if (!Workspace.isValidId(original.id)) {
        problems.add(new Problem(String.valueOf(original.id), "invalid_fluid_id", String.valueOf(original.id)));
        continue;
      }
      FluidDoc doc = original.copy();
      if (ids.contains(doc.id) || ids.contains(doc.flowingId())) {
        problems.add(new Problem(doc.id, "fluid_id_taken", doc.id));
        continue;
      }
      ids.add(doc.id);
      ids.add(doc.flowingId());
      fluids.add(doc);
    }
    return fluids;
  }

  public static String script(List<FluidDoc> fluids, String language) {
    StringBuilder script = new StringBuilder("StartupEvents.registry('fluid', event => {\n");
    for (FluidDoc doc : fluids) {
      String name = doc.displayName(language);
      script.append("\n  // ").append(doc.id).append(' ').append(comment(name)).append('\n');
      script.append("  event.create(").append(quote(doc.id)).append(")\n")
        .append("    .displayName(").append(quote(name)).append(")\n")
        .append("    .translationKey(").append(quote(doc.translationKey())).append(")\n")
        .append("    .tint(").append(String.format(Locale.ROOT, "0x%08X", doc.renderTintColor())).append(")\n")
        .append("    .stillTexture(").append(quote(doc.stillTextureId())).append(")\n")
        .append("    .flowingTexture(").append(quote(doc.flowingTextureId())).append(")\n")
        .append("    .translucent()\n")
        .append("    .type(type => type\n")
        .append("      .descriptionId(").append(quote(doc.translationKey())).append(")\n")
        .append("      .temperature(").append(doc.temperature).append(")\n")
        .append("      .density(").append(doc.density).append(")\n")
        .append("      .viscosity(").append(doc.viscosity).append(")\n")
        .append("      .lightLevel(").append(doc.lightLevel).append(")\n")
        .append("    )");
      if (!doc.bucket) script.append("\n    .noBucket()");
      if (!doc.block) script.append("\n    .noBlock()");
      script.append(";\n");
    }
    return script.append("\n});\n").toString();
  }

  private static String quote(String value) {
    return Workspace.GSON.toJson(value).replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
  }

  private static String comment(String value) {
    return value.replace('\r', ' ').replace('\n', ' ').replace('\u2028', ' ').replace('\u2029', ' ');
  }
}
