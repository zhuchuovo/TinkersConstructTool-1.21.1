package com.tcmateditor.dev;

import com.google.gson.JsonParser;
import com.tcmateditor.TCMatEditor;
import com.tcmateditor.gen.DataGenerator.Problem;
import com.tcmateditor.gen.UpgradeGenerator;
import com.tcmateditor.model.UpgradeDoc;
import com.tcmateditor.model.Workspace;
import com.tcmateditor.pack.LivePack;
import com.tcmateditor.recipe.EditorRecipes;
import com.tcmateditor.recipe.PositionalModifierRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.adding.ModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Dedicated server checks enabled only by the development GameTest run. */
@Mod.EventBusSubscriber(modid = TCMatEditor.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class EditorGameTests {
  private static final String BATCH = "tcmateditor.server";
  private static final ResourceLocation TEMPLATE = TCMatEditor.id("empty");
  private static final String EXPORT_MOD = "tinkers_materials_custommats";

  /** Empty enabled namespaces mean all tests in Forge 47, so explicitly gate registration. */
  @SubscribeEvent
  public static void register(RegisterGameTestsEvent event) {
    if (Boolean.getBoolean("tcmateditor.gametest")) {
      event.register(EditorGameTests.class);
    }
  }

  @GameTest(templateNamespace = TCMatEditor.MOD_ID, template = "empty", batch = BATCH)
  public static void serverInitialization(GameTestHelper helper) {
    helper.assertTrue(FMLLoader.getDist() == Dist.DEDICATED_SERVER, "GameTest must run without client classes");
    helper.assertTrue(helper.getLevel().getServer() instanceof GameTestServer, "Expected native GameTest server");
    helper.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.get(TCMatEditor.id("positional_modifier"))
      == EditorRecipes.POSITIONAL_MODIFIER.get(), "Positional recipe serializer was not registered");
    helper.assertTrue(MaterialRegistry.getMaterial(new MaterialId("tconstruct", "iron")) != IMaterial.UNKNOWN,
      "Official Tinkers' iron material was not loaded on the server");
    helper.assertTrue(helper.getLevel().getServer().getPackRepository().getSelectedIds().contains(LivePack.PACK_ID),
      "Required editor live data pack was not selected");
    helper.succeed();
  }

  @GameTest(templateNamespace = TCMatEditor.MOD_ID, template = "empty", batch = BATCH)
  public static void generatedUpgradeLoads(GameTestHelper helper) {
    UpgradeDoc doc = new UpgradeDoc();
    doc.id = "tcmateditor:gametest_upgrade";
    doc.modifier = "tconstruct:diamond";
    doc.slot(0).ingredient = "minecraft:diamond";
    doc.slot(2).ingredient = "minecraft:redstone";
    doc.slot(2).count = 2;
    doc.denyTools.add("tconstruct:dagger");
    Map<String,String> files = new TreeMap<>();
    List<Problem> problems = new ArrayList<>();
    UpgradeGenerator.generate(doc, new Workspace(), files, problems);
    helper.assertTrue(problems.isEmpty(), "Generated upgrade references were rejected: " + problems);
    String base = "data/tcmateditor/recipes/tcmateditor/upgrade/gametest_upgrade";
    helper.assertTrue(files.containsKey(base + ".json") && files.containsKey(base + "_any_order.json"),
      "Strict and fallback recipes must both be generated under the 1.20.1 recipes directory");
    PositionalModifierRecipe strict = EditorRecipes.POSITIONAL_MODIFIER.get().fromJson(
      new ResourceLocation(doc.id), JsonParser.parseString(files.get(base + ".json")).getAsJsonObject());
    ModifierRecipe fallback = TinkerModifiers.modifierSerializer.get().fromJson(
      TCMatEditor.id("gametest_upgrade_any_order"), JsonParser.parseString(files.get(base + "_any_order.json")).getAsJsonObject());
    helper.assertTrue(fallback != null, "Official Tinkers' 1.20.1 modifier loader rejected the fallback");
    verifyPositions(helper, strict);
    helper.succeed();
  }

  @GameTest(templateNamespace = TCMatEditor.MOD_ID, template = "empty", batch = BATCH)
  public static void exportedModLoads(GameTestHelper helper) {
    boolean fixtureLoaded = ModList.get().isLoaded(EXPORT_MOD);
    helper.assertTrue(fixtureLoaded || !Boolean.getBoolean("tcmateditor.exportGameTest"),
      "Export GameTest requires tinkers_materials_custommats.jar from the client SelfTest in the run mods folder");
    if (!fixtureLoaded) {
      TCMatEditor.LOG.info("[GAMETEST] Export fixture not installed; run with -PexportGameTest to require its checks");
      helper.succeed();
      return;
    }
    MaterialId rubyId = new MaterialId("selftest", "ruby");
    helper.assertTrue(MaterialRegistry.getMaterial(rubyId) != IMaterial.UNKNOWN,
      "Low-code export JAR did not load selftest:ruby");
    HeadMaterialStats head = MaterialRegistry.getInstance().<HeadMaterialStats>getMaterialStats(rubyId, HeadMaterialStats.ID).orElse(null);
    helper.assertTrue(head != null && head.durability() == 1234,
      "Low-code export JAR did not load ruby head durability 1234");
    ModifierId sharp = new ModifierId("selftest", "sharp");
    helper.assertTrue(ModifierManager.INSTANCE.contains(sharp), "Low-code export JAR did not load selftest:sharp");
    ItemStack rubyPickaxe = ToolBuildHandler.createSingleMaterial(TinkerTools.pickaxe.get(), MaterialVariant.of(rubyId, ""));
    helper.assertTrue(!rubyPickaxe.isEmpty() && ToolStack.from(rubyPickaxe).getModifierLevel(sharp) > 0,
      "Exported material traits did not apply when building a server-side ruby tool");
    var recipes = helper.getLevel().getRecipeManager();
    var strict = recipes.byKey(new ResourceLocation("selftest", "tcmateditor/upgrade/sharp"));
    helper.assertTrue(strict.isPresent() && strict.get() instanceof PositionalModifierRecipe,
      "Exported positional upgrade was not loaded through Forge recipe conditions");
    helper.assertTrue(recipes.byKey(new ResourceLocation("selftest", "tcmateditor/upgrade/sharp_any_order")).isEmpty(),
      "Exported any-order fallback must be skipped while the editor is installed");
    verifyPositions(helper, (PositionalModifierRecipe) strict.orElseThrow());
    helper.succeed();
  }

  private static void verifyPositions(GameTestHelper helper, PositionalModifierRecipe recipe) {
    MaterialVariant iron = MaterialVariant.of(new MaterialId("tconstruct", "iron"), "");
    ItemStack sword = ToolBuildHandler.createSingleMaterial(TinkerTools.sword.get(), iron);
    ItemStack dagger = ToolBuildHandler.createSingleMaterial(TinkerTools.dagger.get(), iron);
    ItemStack diamond = new ItemStack(Items.DIAMOND);
    ItemStack redstone = new ItemStack(Items.REDSTONE, 2);
    ItemStack[] correct = { ItemStack.EMPTY, ItemStack.EMPTY, diamond, ItemStack.EMPTY, redstone };
    ItemStack[] swapped = { ItemStack.EMPTY, ItemStack.EMPTY, redstone, ItemStack.EMPTY, diamond };
    ItemStack[] extra = { new ItemStack(Items.STICK), ItemStack.EMPTY, diamond, ItemStack.EMPTY, redstone };
    helper.assertTrue(recipe.matches(station(sword, correct), helper.getLevel()), "Correct anvil slots must match");
    helper.assertFalse(recipe.matches(station(sword, swapped), helper.getLevel()), "Swapped anvil slots must be rejected");
    helper.assertFalse(recipe.matches(station(sword, extra), helper.getLevel()), "Extra anvil ingredients must be rejected");
    helper.assertFalse(recipe.matches(station(dagger, correct), helper.getLevel()), "Denied dagger must be rejected by Forge difference ingredient");
  }

  private static ITinkerStationContainer station(ItemStack tool, ItemStack[] inputs) {
    return new ITinkerStationContainer() {
      @Override public ItemStack getTinkerableStack() { return tool; }
      @Override public ItemStack getInput(int index) { return inputs[index]; }
      @Override public int getInputCount() { return inputs.length; }
      @Override public MaterialRecipe getInputMaterial(int index) { return MaterialRecipe.EMPTY; }
    };
  }

  /** Prepare the tiny template before GameTestBatchRunner creates structures (before-batch is too late). */
  @Mod.EventBusSubscriber(modid = TCMatEditor.MOD_ID)
  public static final class ServerEvents {
    @SubscribeEvent
    public static void prepareTemplate(ServerStartingEvent event) {
      if (!Boolean.getBoolean("tcmateditor.gametest") || !(event.getServer() instanceof GameTestServer)) {
        return;
      }
      CompoundTag template = new CompoundTag();
      template.put("size", ints(1, 1, 1));
      CompoundTag air = new CompoundTag();
      air.putString("Name", "minecraft:air");
      ListTag palette = new ListTag();
      palette.add(air);
      template.put("palette", palette);
      CompoundTag block = new CompoundTag();
      block.put("pos", ints(0, 0, 0));
      block.putInt("state", 0);
      ListTag blocks = new ListTag();
      blocks.add(block);
      template.put("blocks", blocks);
      template.put("entities", new ListTag());
      event.getServer().getStructureManager().getOrCreate(TEMPLATE).load(BuiltInRegistries.BLOCK.asLookup(), template);
    }

    private static ListTag ints(int... values) {
      ListTag list = new ListTag();
      Arrays.stream(values).forEach(value -> list.add(IntTag.valueOf(value)));
      return list;
    }
  }
}
