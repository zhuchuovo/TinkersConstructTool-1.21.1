package com.tcmateditor.client.gui.pages;

import com.tcmateditor.client.ClientApply;
import com.tcmateditor.client.EditorSession;
import com.tcmateditor.client.gen.AssetGenerator;
import com.tcmateditor.client.gui.Form;
import com.tcmateditor.client.gui.Icons;
import com.tcmateditor.client.gui.Names;
import com.tcmateditor.client.gui.Pickers;
import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.client.gui.ui.Button;
import com.tcmateditor.client.gui.ui.ItemIcon;
import com.tcmateditor.client.gui.ui.Label;
import com.tcmateditor.client.gui.ui.NumberField;
import com.tcmateditor.client.gui.ui.TextField;
import com.tcmateditor.client.gui.ui.Toggle;
import com.tcmateditor.gen.GenUtil;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.MaterialDoc.ItemRef;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Basic settings: ID, tier, crafting items and fluids */
public class GeneralPage implements Page {
  private final MaterialDoc doc;

  public GeneralPage(MaterialDoc doc) {
    this.doc = doc;
  }

  @Override
  public void build(Form form) {
    form.hint(Names.tr("page.general.hint"));

    TextField id = form.row(Names.tr("label.id"), new TextField(doc.id, form.controlWidth()));
    id.validator(text -> AssetGenerator.isValidMaterialId(text) && (text.equals(doc.id) || EditorSession.workspace().findMaterial(text) == null));
    id.onChange(text -> {
      if (id.isValid() && !text.equals(doc.id)) {
        doc.id = text;
        Page.changed();
      }
    });
    id.tooltip(Names.tr("hint.material_id"));

    form.row(Names.tr("label.tier"), new NumberField(doc.tier, true, 0, 100, 1, 60)
      .onValue(v -> { doc.tier = (int) v; Page.changed(); })).tooltip(Names.tr("hint.tier"));
    form.row(Names.tr("label.sort_order"), new NumberField(doc.sortOrder, true, -10000, 10000, 1, 60)
      .onValue(v -> { doc.sortOrder = (int) v; Page.changed(); })).tooltip(Names.tr("hint.sort_order"));
    form.row(Names.tr("label.craftable"), new Toggle(Component.empty(), () -> doc.craftable, v -> { doc.craftable = v; Page.changed(); }))
      .tooltip(Names.tr("hint.craftable"));
    form.row(Names.tr("label.hidden"), new Toggle(Component.empty(), () -> doc.hidden, v -> { doc.hidden = v; Page.changed(); }))
      .tooltip(Names.tr("hint.hidden"));

    // crafting items
    form.header(Names.tr("section.items"));
    form.hint(Names.tr("section.items.hint"));
    int width = form.width();
    int numberW = 44;
    int fieldW = width - 20 - numberW * 2 - 30;
    if (!doc.items.isEmpty()) {
      form.place(new Label(Names.tr("label.item"), Theme.TEXT_FAINT), 22);
      form.place(new Label(Names.tr("label.value"), Theme.TEXT_FAINT), 24 + fieldW).tooltip(Names.tr("hint.value"));
      form.place(new Label(Names.tr("label.needed"), Theme.TEXT_FAINT), 28 + fieldW + numberW).tooltip(Names.tr("hint.needed"));
      form.next(8);
    }
    for (ItemRef item : new ArrayList<>(doc.items)) {
      TextField[] field = new TextField[1];
      form.place(new ItemIcon(() -> Icons.forRef(item.ingredient)).onClick(() -> Pickers.item(picked -> {
        item.ingredient = picked;
        field[0].setValue(picked);
        Page.changed();
      })).size(18, 16), 0).tooltip(Names.tr("hint.pick_item"));
      field[0] = form.place(new TextField(item.ingredient, fieldW), 20);
      field[0].hint(Component.literal("minecraft:diamond / #forge:gems"));
      field[0].validator(GenUtil::isKnownItem);
      field[0].onChange(text -> { item.ingredient = text.trim(); Page.changed(); });
      form.place(new NumberField(item.value, true, 1, 10000, 1, numberW).onValue(v -> { item.value = (int) v; Page.changed(); }), 24 + fieldW);
      form.place(new NumberField(item.needed, true, 1, 10000, 1, numberW).onValue(v -> { item.needed = (int) v; Page.changed(); }), 28 + fieldW + numberW);
      form.place(new Button(Component.literal("×"), () -> { doc.items.remove(item); Page.rebuild(); }).style(Button.Style.GHOST).size(16, 16), width - 16)
        .tooltip(Names.tr("button.remove"));
      form.next(16);
    }
    Button add = form.place(new Button(Names.tr("button.add_item"), () -> Pickers.item(picked -> {
      doc.items.add(new ItemRef(picked, 1, 1));
      Page.rebuild();
    })), 0);
    form.place(new Button(Names.tr("button.add_held"), () -> {
      ItemStack held = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getMainHandItem() : ItemStack.EMPTY;
      if (!held.isEmpty()) {
        doc.items.add(new ItemRef(BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), 1, 1));
        Page.rebuild();
      }
    }), add.w + 4).tooltip(Names.tr("hint.add_held"));
    form.next(16);

    // fluid
    form.header(Names.tr("section.fluid"));
    form.row(Names.tr("label.fluid_enabled"), new Toggle(Component.empty(), () -> doc.fluid.enabled, v -> { doc.fluid.enabled = v; Page.rebuild(); }))
      .tooltip(Names.tr("hint.fluid"));
    if (doc.fluid.enabled) {
      TextField[] fluidField = new TextField[1];
      fluidField[0] = form.row(Names.tr("label.fluid"), new TextField(doc.fluid.fluid, form.controlWidth() - 20));
      fluidField[0].hint(Component.literal("tconstruct:molten_iron"));
      fluidField[0].validator(text -> text.isBlank() || GenUtil.isKnownFluid(text));
      fluidField[0].tooltip(Names.tr("hint.fluid_id"));
      fluidField[0].onChange(text -> { doc.fluid.fluid = text.trim(); Page.changed(); });
      form.view.add(new Button(Component.literal("…"), () -> Pickers.fluid(picked -> {
        doc.fluid.fluid = picked;
        fluidField[0].setValue(picked);
        Page.changed();
      })).size(16, 16)).at(fluidField[0].lx + fluidField[0].w + 4, fluidField[0].ly).tooltip(Names.tr("hint.pick_fluid"));
      form.row(Names.tr("label.fluid_amount"), new NumberField(doc.fluid.amount, true, 1, 100000, 10, 60).suffix("mB")
        .onValue(v -> { doc.fluid.amount = (int) v; Page.changed(); })).tooltip(Names.tr("hint.fluid_amount"));
      form.row(Names.tr("label.temperature"), new NumberField(doc.fluid.temperature, true, 1, 100000, 50, 60).suffix("°C")
        .onValue(v -> { doc.fluid.temperature = (int) v; Page.changed(); }));
    }

    // in game preview
    form.header(Names.tr("section.ingame"));
    MaterialId materialId = MaterialId.tryParse(doc.id);
    boolean loaded = materialId != null && MaterialRegistry.getMaterial(materialId) != IMaterial.UNKNOWN;
    if (!loaded) {
      form.hint(Names.tr("section.ingame.not_loaded"));
    } else {
      List<Supplier<ItemStack>> stacks = new ArrayList<>();
      stacks.add(() -> TinkerToolParts.pickHead.get().withMaterial(materialId));
      stacks.add(() -> TinkerToolParts.toolHandle.get().withMaterial(materialId));
      stacks.add(() -> TinkerToolParts.toolBinding.get().withMaterial(materialId));
      stacks.add(() -> TinkerToolParts.plating.get(ArmorItem.Type.CHESTPLATE).withMaterial(materialId));
      stacks.add(tool(TinkerTools.pickaxe.get(), materialId));
      stacks.add(tool(TinkerTools.sword.get(), materialId));
      stacks.add(tool(TinkerTools.plateArmor.get(ArmorItem.Type.CHESTPLATE), materialId));
      int x = 0;
      for (Supplier<ItemStack> stack : stacks) {
        ItemStack cached = safe(stack);
        form.place(new ItemIcon(() -> cached), x);
        x += 22;
      }
      form.next(18);
      form.full(new Button(Names.tr("button.give_test"), () -> ClientApply.giveTestTools(doc.id)).fit()).tooltip(Names.tr("hint.give_test"));
    }
    form.finish();
  }

  private static ItemStack safe(Supplier<ItemStack> supplier) {
    try {
      return supplier.get();
    } catch (RuntimeException e) {
      return ItemStack.EMPTY;
    }
  }

  private static Supplier<ItemStack> tool(IModifiable item, MaterialId id) {
    return () -> ToolBuildHandler.createSingleMaterial(item, MaterialVariant.of(id, ""));
  }
}
