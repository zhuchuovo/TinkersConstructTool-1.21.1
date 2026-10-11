package com.tcmateditor.client.dev;

import com.google.gson.JsonObject;
import com.tcmateditor.EditorPaths;
import com.tcmateditor.TCMatEditor;
import com.tcmateditor.client.ClientApply;
import com.tcmateditor.client.EditorSession;
import com.tcmateditor.client.Exporter;
import com.tcmateditor.client.MaterialImporter;
import com.tcmateditor.client.gui.EditorScreen;
import com.tcmateditor.client.gui.Pickers;
import com.tcmateditor.client.gui.StatSchema;
import com.tcmateditor.client.gui.ui.ColorPickerPopup;
import com.tcmateditor.client.gui.ui.UI;
import com.tcmateditor.model.EntryDoc;
import com.tcmateditor.model.LangText;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.TextureDoc;
import com.tcmateditor.model.Workspace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Development self test, only active with {@code -Dtcmateditor.selftest=true}.
 * Builds sample documents, applies them, checks Tinkers' registries and takes screenshots of the editor.
 */
public final class SelfTest {
  private SelfTest() {}

  public static final boolean ENABLED = Boolean.getBoolean("tcmateditor.selftest");
  private static final String NS = "selftest";

  private static int step = 0;
  private static int wait = 0;
  private static final List<String> REPORT = new ArrayList<>();
  private static int failures = 0;

  private static void check(boolean ok, String what) {
    REPORT.add((ok ? "PASS " : "FAIL ") + what);
    TCMatEditor.LOG.info("[SELFTEST] {} {}", ok ? "PASS" : "FAIL", what);
    if (!ok) failures++;
  }

  private static void shot(String name) {
    Minecraft mc = Minecraft.getInstance();
    Screenshot.grab(mc.gameDirectory, "selftest_" + name + ".png", mc.getMainRenderTarget(), c -> {});
  }

  private static boolean busy() {
    Minecraft mc = Minecraft.getInstance();
    return mc.getOverlay() != null || ClientApply.isWaiting();
  }

  public static void tick() {
    Minecraft mc = Minecraft.getInstance();
    if (!ENABLED || mc.player == null || step < 0) {
      return;
    }
    if (wait > 0) {
      wait--;
      return;
    }
    try {
      run(mc);
    } catch (Throwable e) {
      TCMatEditor.LOG.error("[SELFTEST] crashed at step {}", step, e);
      check(false, "exception at step " + step + ": " + e);
      finish();
    }
  }

  private static void run(Minecraft mc) {
    switch (step) {
      case 0 -> { wait = 60; step++; }
      case 1 -> {
        mc.getWindow().setWindowed(854, 480);
        buildWorkspace();
        ClientApply.apply(true);
        wait = 20;
        step++;
      }
      case 2 -> {
        if (busy()) return;
        wait = 20;
        step++;
      }
      case 3 -> {
        if (busy()) return;
        verify();
        step++;
      }
      case 4 -> {
        // give test tools
        mc.player.getInventory().clearContent();
        ClientApply.giveTestTools(NS + ":ruby");
        wait = 20;
        step++;
      }
      case 5 -> {
        int tools = 0;
        for (ItemStack stack : mc.player.getInventory().items) {
          if (stack.getItem() instanceof IModifiable) {
            tools++;
            ToolStack tool = ToolStack.from(stack);
            if (tools == 1) {
              check(tool.getMaterials().toString().contains(NS + ":ruby"), "test tool uses the custom material: " + tool.getMaterials());
              check(tool.getStats().get(ToolStats.DURABILITY) > 0, "test tool has durability " + tool.getStats().get(ToolStats.DURABILITY));
            }
          }
        }
        check(tools >= 3, "received test tools: " + tools);
        // hot reload: change a stat and check the tool updates
        MaterialDoc ruby = EditorSession.workspace().findMaterial(NS + ":ruby");
        ruby.stats.get("tconstruct:head").addProperty("durability", 1234);
        ClientApply.apply(false);
        wait = 20;
        step++;
      }
      case 6 -> {
        if (busy()) return;
        wait = 10;
        step++;
      }
      case 7 -> {
        MaterialId id = new MaterialId(NS, "ruby");
        Object head = MaterialRegistry.getInstance().getMaterialStats(id, new MaterialStatsId("tconstruct", "head")).orElse(null);
        check(head != null && head.toString().contains("1234"), "hot reloaded head stats: " + head);
        int refreshed = 0;
        for (ItemStack stack : mc.player.getInventory().items) {
          if (stack.getItem() instanceof IModifiable) {
            ToolStack tool = ToolStack.from(stack);
            if (tool.getStats().get(ToolStats.DURABILITY) >= 1000) refreshed++;
          }
        }
        check(refreshed > 0, "tools in inventory refreshed after reload: " + refreshed);
        // export
        try {
          Exporter.Result result = Exporter.export(EditorSession.workspace());
          check(Files.size(result.dataPack()) > 500 && Files.size(result.resourcePack()) > 1000, "export zips written " + Files.size(result.dataPack()) + " / " + Files.size(result.resourcePack()));
        } catch (IOException e) {
          check(false, "export failed " + e);
        }
        // open the editor
        EditorSession.mode = EditorSession.Mode.MATERIALS;
        EditorSession.material = EditorSession.workspace().findMaterial(NS + ":ruby");
        EditorSession.materialTab = 0;
        mc.setScreen(new EditorScreen());
        wait = 10;
        step++;
      }
      case 8, 9, 10, 11, 12 -> {
        int tab = step - 8;
        if (EditorSession.materialTab != tab) {
          EditorSession.materialTab = tab;
          UI.requestRebuild();
          wait = 6;
          return;
        }
        shot("small_tab" + tab);
        wait = 4;
        step++;
      }
      case 13 -> {
        EditorSession.mode = EditorSession.Mode.ENTRIES;
        EditorSession.entry = EditorSession.workspace().findEntry(NS + ":sharp");
        EditorSession.entryTab = 0;
        UI.requestRebuild();
        wait = 6;
        step++;
      }
      case 14 -> { shot("small_entry"); wait = 4; step++; }
      case 15 -> {
        EditorSession.mode = EditorSession.Mode.MATERIALS;
        EditorSession.materialTab = 3;
        UI.requestRebuild();
        mc.getWindow().setWindowed(1600, 900);
        wait = 10;
        step++;
      }
      case 16 -> { shot("large_texture"); new ColorPickerPopup(0xFFE03050, c -> {}).openAt(200, 60); wait = 4; step++; }
      case 17 -> { shot("large_colorpicker"); UI.closeAllPopups(); Pickers.modifier(id -> {}); wait = 4; step++; }
      case 18 -> { shot("large_picker"); UI.closeAllPopups(); EditorSession.materialTab = 1; UI.requestRebuild(); wait = 6; step++; }
      case 19 -> { shot("large_stats"); EditorSession.materialTab = 2; UI.requestRebuild(); wait = 6; step++; }
      case 20 -> { shot("large_traits"); EditorSession.materialTab = 4; UI.requestRebuild(); wait = 6; step++; }
      case 21 -> { shot("large_info"); EditorSession.mode = EditorSession.Mode.ENTRIES; UI.requestRebuild(); wait = 6; step++; }
      case 22 -> {
        shot("large_entry");
        EditorSession.mode = EditorSession.Mode.UPGRADES;
        EditorSession.upgrade = EditorSession.workspace().findUpgrade(NS + ":sharp");
        EditorSession.upgradeTab = 0;
        UI.requestRebuild();
        wait = 6;
        step = 100;
      }
      case 100 -> { shot("large_upgrade_recipe"); com.tcmateditor.client.gui.ui.HelpBadge.forceOpen = true; wait = 3; step++; }
      case 101 -> { shot("large_upgrade_help"); com.tcmateditor.client.gui.ui.HelpBadge.forceOpen = false; EditorSession.upgradeTab = 1; UI.requestRebuild(); wait = 6; step++; }
      case 102 -> { shot("large_upgrade_tools"); wait = 2; step = 23; }
      // light theme
      case 23 -> {
        com.tcmateditor.client.ClientSettings.setLightTheme(true);
        check(com.tcmateditor.client.gui.Theme.isLight(), "light theme applied");
        EditorSession.mode = EditorSession.Mode.MATERIALS;
        EditorSession.materialTab = 3;
        UI.requestRebuild();
        wait = 6;
        step++;
      }
      case 24 -> { shot("light_texture"); new ColorPickerPopup(0xFFE03050, c -> {}).openAt(200, 60); wait = 4; step++; }
      case 25 -> { shot("light_colorpicker"); UI.closeAllPopups(); EditorSession.materialTab = 1; UI.requestRebuild(); wait = 6; step++; }
      case 26 -> { shot("light_stats"); EditorSession.materialTab = 4; UI.requestRebuild(); wait = 6; step++; }
      case 27 -> { shot("light_info"); EditorSession.mode = EditorSession.Mode.ENTRIES; UI.requestRebuild(); wait = 6; step++; }
      case 28 -> {
        shot("light_entry");
        EditorSession.mode = EditorSession.Mode.UPGRADES;
        EditorSession.upgradeTab = 0;
        UI.requestRebuild();
        wait = 6;
        step = 110;
      }
      case 110 -> { shot("light_upgrade_recipe"); EditorSession.mode = EditorSession.Mode.ENTRIES; UI.requestRebuild(); com.tcmateditor.client.gui.Dialogs.help(); wait = 4; step = 29; }
      case 29 -> {
        shot("light_help");
        UI.closeAllPopups();
        EditorSession.mode = EditorSession.Mode.MATERIALS;
        EditorSession.materialTab = 0;
        UI.requestRebuild();
        mc.getWindow().setWindowed(854, 480);
        wait = 10;
        step++;
      }
      case 30 -> {
        shot("light_small_general");
        com.tcmateditor.client.ClientSettings.setLightTheme(false);
        check(!com.tcmateditor.client.gui.Theme.isLight(), "dark theme restored");
        step++;
      }
      case 31 -> finish();
      default -> {}
    }
  }

  private static void finish() {
    step = -1;
    REPORT.add(failures == 0 ? "ALL PASSED" : failures + " FAILED");
    try {
      Path file = EditorPaths.root().resolve("selftest.txt");
      Files.writeString(file, String.join("\n", REPORT), StandardCharsets.UTF_8);
    } catch (IOException e) {
      TCMatEditor.LOG.error("[SELFTEST] could not write report", e);
    }
    TCMatEditor.LOG.info("[SELFTEST] finished: {}", REPORT.get(REPORT.size() - 1));
    if (Boolean.getBoolean("tcmateditor.selftest.quit")) {
      Minecraft.getInstance().stop();
    }
  }

  /** Builds a station container with inputs at the given station input indices */
  private static slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer station(ItemStack tool, ItemStack[] inputs) {
    return new slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer() {
      @Override
      public ItemStack getTinkerableStack() {
        return tool;
      }

      @Override
      public ItemStack getInput(int index) {
        return index >= 0 && index < inputs.length ? inputs[index] : ItemStack.EMPTY;
      }

      @Override
      public int getInputCount() {
        return inputs.length;
      }

      @Override
      public slimeknights.tconstruct.library.recipe.material.MaterialRecipe getInputMaterial(int index) {
        return slimeknights.tconstruct.library.recipe.material.MaterialRecipe.EMPTY;
      }
    };
  }

  private static void verifyUpgrade() {
    Minecraft mc = Minecraft.getInstance();
    var recipes = mc.level.getRecipeManager();
    var strict = recipes.byKey(ResourceLocation.fromNamespaceAndPath(NS, "tcmateditor/upgrade/sharp"));
    check(strict.isPresent() && strict.get().value() instanceof com.tcmateditor.recipe.PositionalModifierRecipe, "positional upgrade recipe loaded: " + strict.map(r -> r.value().getClass().getSimpleName()).orElse("missing"));
    check(recipes.byKey(ResourceLocation.fromNamespaceAndPath(NS, "tcmateditor/upgrade/sharp_any_order")).isEmpty(), "fallback recipe skipped while the mod is loaded");
    if (strict.isEmpty() || !(strict.get().value() instanceof com.tcmateditor.recipe.PositionalModifierRecipe recipe)) {
      return;
    }
    ItemStack sword = slimeknights.tconstruct.library.tools.helper.ToolBuildHandler.createSingleMaterial(slimeknights.tconstruct.tools.TinkerTools.sword.get(),
      slimeknights.tconstruct.library.materials.definition.MaterialVariant.of(new MaterialId("tconstruct", "iron"), ""));
    ItemStack dagger = slimeknights.tconstruct.library.tools.helper.ToolBuildHandler.createSingleMaterial(slimeknights.tconstruct.tools.TinkerTools.dagger.get(),
      slimeknights.tconstruct.library.materials.definition.MaterialVariant.of(new MaterialId("tconstruct", "iron"), ""));
    ItemStack diamond = new ItemStack(net.minecraft.world.item.Items.DIAMOND);
    ItemStack redstone = new ItemStack(net.minecraft.world.item.Items.REDSTONE, 2);
    // slot 1 = lapis = input 2, slot 3 = gem = input 4
    ItemStack[] correct = { ItemStack.EMPTY, ItemStack.EMPTY, diamond, ItemStack.EMPTY, redstone };
    ItemStack[] swapped = { ItemStack.EMPTY, ItemStack.EMPTY, redstone, ItemStack.EMPTY, diamond };
    ItemStack[] extra = { new ItemStack(net.minecraft.world.item.Items.STICK), ItemStack.EMPTY, diamond, ItemStack.EMPTY, redstone };
    check(recipe.matches(station(sword, correct), mc.level), "upgrade matches materials in the right slots");
    check(!recipe.matches(station(sword, swapped), mc.level), "upgrade rejects materials in swapped slots");
    check(!recipe.matches(station(sword, extra), mc.level), "upgrade rejects extra materials");
    check(!recipe.matches(station(dagger, correct), mc.level), "upgrade rejects denied equipment (dagger)");
    check(recipe.getDisplayItems(1).stream().anyMatch(s -> s.is(net.minecraft.world.item.Items.DIAMOND)), "JEI shows slot 1 at the top");
  }

  /** Sample workspace: an imported template, a new material and a custom entry */
  private static void buildWorkspace() {
    Workspace workspace = EditorSession.workspace();
    workspace.materials.removeIf(m -> m.namespace().equals(NS));
    workspace.entries.removeIf(e -> e.namespace().equals(NS));

    EntryDoc sharp = new EntryDoc();
    sharp.id = NS + ":sharp";
    sharp.color = "#FF5577";
    EntryDoc.Line damage = new EntryDoc.Line();
    damage.target = "tconstruct:attack_damage";
    damage.op = "add";
    damage.perLevel = 2;
    sharp.lines.add(damage);
    EntryDoc.Line speed = new EntryDoc.Line();
    speed.kind = "attribute";
    speed.target = "minecraft:generic.attack_speed";
    speed.op = "add_multiplied_total";
    speed.perLevel = -0.1f;
    speed.slots = new ArrayList<>(List.of("mainhand"));
    sharp.lines.add(speed);
    sharp.lang.put("zh_cn", new LangText("锋锐", "更痛，但更慢", "每级 +2 攻击伤害，-10% 攻击速度"));
    sharp.lang.put("en_us", new LangText("Sharp", "Hurts more, swings slower", "+2 damage and -10% attack speed per level"));
    workspace.entries.add(sharp);

    workspace.upgrades.removeIf(u -> u.namespace().equals(NS));
    com.tcmateditor.model.UpgradeDoc upgrade = new com.tcmateditor.model.UpgradeDoc();
    upgrade.id = NS + ":sharp";
    upgrade.modifier = NS + ":sharp";
    upgrade.slot(0).ingredient = "minecraft:diamond";
    upgrade.slot(2).ingredient = "minecraft:redstone";
    upgrade.slot(2).count = 2;
    upgrade.allowTools = new ArrayList<>(List.of("#tconstruct:modifiable/melee"));
    upgrade.denyTools = new ArrayList<>(List.of("tconstruct:dagger"));
    upgrade.maxLevel = 3;
    workspace.upgrades.add(upgrade);

    MaterialDoc ruby = new MaterialDoc();
    ruby.id = NS + ":ruby";
    ruby.tier = 3;
    for (StatSchema.StatInfo info : StatSchema.all()) {
      if (List.of("tconstruct:head", "tconstruct:handle", "tconstruct:binding", "tconstruct:limb", "tconstruct:grip", "tconstruct:bowstring",
        "tconstruct:plating_helmet", "tconstruct:plating_chestplate", "tconstruct:plating_leggings", "tconstruct:plating_boots", "tconstruct:maille").contains(info.id())) {
        ruby.stats.put(info.id(), StatSchema.defaults(info));
      }
    }
    JsonObject handle = ruby.stats.get("tconstruct:handle");
    handle.addProperty("melee_damage", 0.1f);
    ruby.defaultTraits.add(new MaterialDoc.TraitRef(NS + ":sharp", 2));
    ruby.items.add(new MaterialDoc.ItemRef("minecraft:redstone_block", 1, 1));
    ruby.texture = TextureDoc.createDefault(0xFFE0304A);
    TextureDoc.Effect sparkle = new TextureDoc.Effect();
    sparkle.type = "sparkle";
    sparkle.region = "light";
    sparkle.strength = 0.6f;
    ruby.texture.effects.add(sparkle);
    TextureDoc.Effect noise = new TextureDoc.Effect();
    noise.type = "noise";
    noise.strength = 0.3f;
    ruby.texture.effects.add(noise);
    ruby.texture.animation.type = "shimmer";
    ruby.texture.luminosity = 4;
    ruby.lang.put("zh_cn", new LangText("红宝石", "闪耀而锋利", "带有锋锐词条的宝石材料。"));
    ruby.lang.put("zh_tw", new LangText("紅寶石", "閃耀而鋒利", "帶有鋒銳詞條的寶石材料。"));
    ruby.lang.put("en_us", new LangText("Ruby", "Shiny and sharp", "A gem material with the Sharp entry."));
    ruby.lang.put("ja_jp", new LangText("ルビー", "輝いて鋭い", "シャープを持つ宝石素材。"));
    workspace.materials.add(ruby);

    IMaterial iron = MaterialRegistry.getMaterial(new MaterialId("tconstruct", "iron"));
    check(iron != IMaterial.UNKNOWN, "tconstruct:iron exists for import");
    MaterialDoc copy = MaterialImporter.importMaterial(iron, NS + ":iron_copy");
    check(copy.stats.containsKey("tconstruct:head") && copy.stats.size() >= 5, "imported iron stats: " + copy.stats.keySet());
    check(!copy.defaultTraits.isEmpty(), "imported iron traits: " + copy.defaultTraits.size());
    check(!copy.items.isEmpty(), "imported iron items: " + copy.items.size());
    check(copy.statTraits.keySet().equals(java.util.Set.of("tconstruct:armor", "tconstruct:skull")), "imported iron trait overrides collapsed: " + copy.statTraits.keySet());
    check(copy.texture.palette.size() >= 5, "imported iron palette: " + copy.texture.palette.size());
    check(copy.lang.containsKey("zh_cn") && !copy.lang.get("zh_cn").name.isEmpty(), "imported iron zh_cn name: " + (copy.lang.containsKey("zh_cn") ? copy.lang.get("zh_cn").name : "-"));
    workspace.materials.add(copy);
  }

  private static void verify() {
    MaterialId rubyId = new MaterialId(NS, "ruby");
    IMaterial ruby = MaterialRegistry.getMaterial(rubyId);
    check(ruby != IMaterial.UNKNOWN, "ruby material loaded by Tinkers'");
    check(ruby.getTier() == 3, "ruby tier " + ruby.getTier());
    check(MaterialRegistry.getInstance().getAllStats(rubyId).size() == 11, "ruby stat types " + MaterialRegistry.getInstance().getAllStats(rubyId).size());
    List<ModifierEntry> traits = MaterialRegistry.getInstance().getDefaultTraits(rubyId);
    check(traits.size() == 1 && traits.get(0).getId().toString().equals(NS + ":sharp") && traits.get(0).getLevel() == 2, "ruby traits " + traits);
    check(MaterialRegistry.getMaterial(new MaterialId(NS, "iron_copy")) != IMaterial.UNKNOWN, "imported copy loaded");
    ModifierId sharp = new ModifierId(NS, "sharp");
    check(ModifierManager.INSTANCE.contains(sharp), "custom entry loaded as modifier");
    check(I18n.exists("material.selftest.ruby"), "material name translated: " + I18n.get("material.selftest.ruby"));
    check(I18n.exists("modifier.selftest.sharp"), "entry name translated: " + I18n.get("modifier.selftest.sharp"));
    TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse("tconstruct:item/tool/pickaxe/head_selftest_ruby"));
    check(!sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()), "generated pickaxe head sprite in atlas " + sprite.contents().name());
    TextureAtlasSprite ironHead = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse("tconstruct:item/tool/pickaxe/head_selftest_iron_copy"));
    check(!ironHead.contents().name().equals(MissingTextureAtlasSprite.getLocation()), "generated iron copy sprite in atlas");
    Path data = EditorPaths.livePack().resolve("data/selftest/tinkering/materials/stats/ruby.json");
    check(Files.exists(data), "live pack data written");
    verifyUpgrade();
  }
}
