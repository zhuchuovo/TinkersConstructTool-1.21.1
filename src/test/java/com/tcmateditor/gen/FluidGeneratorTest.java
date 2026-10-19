package com.tcmateditor.gen;

import com.tcmateditor.model.FluidDoc;
import com.tcmateditor.model.LangText;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.Workspace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FluidGeneratorTest {
  private static FluidDoc fluid(String id) {
    FluidDoc doc = new FluidDoc();
    doc.id = id;
    doc.lang.put("zh_cn", new LangText("熔融红宝石", "", ""));
    doc.lang.put("en_us", new LangText("Molten Ruby", "", ""));
    return doc;
  }

  @Test
  void singleScriptHasLocalizedCommentsAndSpacing() {
    FluidDoc ruby = fluid("custommats:molten_ruby");
    FluidDoc sapphire = fluid("custommats:molten_sapphire");
    sapphire.bucket = false;
    sapphire.block = false;
    String chinese = FluidGenerator.script(List.of(ruby, sapphire), "zh_cn");
    assertEquals(1, chinese.split("StartupEvents.registry", -1).length - 1);
    assertTrue(chinese.contains("\n  // custommats:molten_ruby 熔融红宝石\n  event.create(\"custommats:molten_ruby\")"));
    assertTrue(chinese.contains(";\n\n  // custommats:molten_sapphire"));
    assertTrue(chinese.contains(".noBucket()\n    .noBlock();"));
    assertTrue(chinese.contains(".type(type => type\n"));
    assertTrue(chinese.contains(".tint(0xFFFFFFFF)"));
    String english = FluidGenerator.script(List.of(ruby), "en_us");
    assertTrue(english.contains("// custommats:molten_ruby Molten Ruby"));
    assertFalse(english.contains("熔融红宝石"));
  }

  @Test
  void namesCannotBreakJavaScriptOrComments() {
    FluidDoc doc = fluid("custommats:quoted");
    doc.lang.put("en_us", new LangText("Quoted \"name\"\nline\u2028tail", "", ""));
    String script = FluidGenerator.script(List.of(doc), "en_us");
    assertTrue(script.contains("// custommats:quoted Quoted \"name\" line tail\n"));
    assertTrue(script.contains(".displayName(\"Quoted \\\"name\\\"\\nline\\u2028tail\")"));
  }

  @Test
  void collidingFlowingIdsAreReportedAndSkipped() {
    Workspace workspace = new Workspace();
    workspace.fluids.add(fluid("custommats:ruby"));
    workspace.fluids.add(fluid("custommats:flowing_ruby"));
    workspace.fluids.add(fluid("INVALID"));
    List<DataGenerator.Problem> problems = new ArrayList<>();
    assertEquals(List.of("custommats:ruby"), FluidGenerator.validFluids(workspace, problems).stream().map(doc -> doc.id).toList());
    assertEquals(List.of("fluid_id_taken", "invalid_fluid_id"), problems.stream().map(DataGenerator.Problem::key).toList());
    assertEquals("custommats:flowing_ruby_2", workspace.freeFluidId("custommats", "flowing_ruby"));
  }

  @Test
  void newFluidRecipesExportButCannotBeLiveReloaded() {
    Workspace workspace = new Workspace();
    workspace.fluids.add(fluid("custommats:molten_ruby"));
    MaterialDoc material = new MaterialDoc();
    material.id = "custommats:ruby";
    material.fluid.enabled = true;
    material.fluid.fluid = "custommats:molten_ruby";
    workspace.materials.add(material);
    String casting = "data/custommats/recipe/tcmateditor/ruby/casting.json";
    List<DataGenerator.Problem> problems = new ArrayList<>();
    assertTrue(GenUtil.isKnownFluid(material.fluid.fluid, workspace));
    assertFalse(DataGenerator.generate(workspace, problems).containsKey(casting));
    assertTrue(problems.stream().anyMatch(problem -> problem.key().equals("unknown_fluid")));
    problems.clear();
    assertTrue(DataGenerator.generateForExport(workspace, problems).containsKey(casting));
    assertTrue(problems.isEmpty());
  }
}
