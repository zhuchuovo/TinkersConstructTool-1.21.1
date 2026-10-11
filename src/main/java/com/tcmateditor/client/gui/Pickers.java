package com.tcmateditor.client.gui;

import com.tcmateditor.client.EditorSession;
import com.tcmateditor.client.MaterialImporter;
import com.tcmateditor.client.gui.ui.SearchPopup;
import com.tcmateditor.client.gui.ui.SearchPopup.Entry;
import com.tcmateditor.gen.EntryGenerator;
import com.tcmateditor.model.EntryDoc;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;
import slimeknights.tconstruct.library.tools.stat.IToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Search popups for the different ID types */
public final class Pickers {
  private Pickers() {}

  public static void item(Consumer<String> onPick) {
    List<Entry> entries = new ArrayList<>();
    for (Item item : BuiltInRegistries.ITEM) {
      if (item == Items.AIR) continue;
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
      ItemStack stack = new ItemStack(item);
      entries.add(new Entry(id.toString(), stack.getHoverName(), Component.literal(id.toString()), 0, stack));
    }
    BuiltInRegistries.ITEM.getTagNames().sorted(Comparator.comparing(t -> t.location().toString())).forEach(tag -> {
      List<ItemStack> items = tagItems(tag.location().toString());
      if (!items.isEmpty()) {
        String id = "#" + tag.location();
        entries.add(new Entry(id, Component.literal(id), Names.tr("label.tag"), 0, items.get(0)));
      }
    });
    new SearchPopup(Names.tr("picker.item"), entries, entry -> onPick.accept(entry.id())).open();
  }

  /** Items in an item tag */
  public static List<ItemStack> tagItems(String tag) {
    ResourceLocation id = ResourceLocation.tryParse(tag);
    List<ItemStack> list = new ArrayList<>();
    if (id != null) {
      BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id)).ifPresent(set -> {
        for (Holder<Item> holder : set) {
          list.add(new ItemStack(holder.value()));
        }
      });
    }
    return list;
  }

  public static void fluid(Consumer<String> onPick) {
    List<Entry> entries = new ArrayList<>();
    for (Fluid fluid : BuiltInRegistries.FLUID) {
      if (fluid == Fluids.EMPTY) continue;
      if (fluid instanceof FlowingFluid flowing && flowing.getSource() != fluid) continue;
      ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
      ItemStack bucket = new ItemStack(fluid.getBucket());
      entries.add(new Entry(id.toString(), fluid.getFluidType().getDescription(), Component.literal(id.toString()), 0, bucket));
    }
    new SearchPopup(Names.tr("picker.fluid"), entries, entry -> onPick.accept(entry.id())).open();
  }

  /** Modifiers usable as traits, custom entries first */
  public static void modifier(Consumer<String> onPick) {
    List<Entry> entries = new ArrayList<>();
    for (EntryDoc doc : EditorSession.workspace().entries) {
      entries.add(new Entry(doc.id, Component.literal(doc.displayName(EditorSession.uiLanguage())), Names.tr("label.custom_entry"), doc.colorInt(), ItemStack.EMPTY));
    }
    List<Modifier> modifiers = new ArrayList<>(ModifierManager.INSTANCE.getAllValues().toList());
    modifiers.sort(Comparator.comparing(m -> Names.modifierName(m).getString()));
    for (Modifier modifier : modifiers) {
      String id = modifier.getId().toString();
      if (EditorSession.workspace().findEntry(id) != null) continue;
      entries.add(new Entry(id, Names.modifierName(modifier), Component.literal(id), modifier.getColor() | 0xFF000000, ItemStack.EMPTY));
    }
    new SearchPopup(Names.tr("picker.modifier"), entries, entry -> onPick.accept(entry.id())).open();
  }

  /** Prefix of the modifier slot choices in the tool stat picker */
  public static final String SLOT_PREFIX = "slot:";

  /** Numeric tool stats, with the modifier slot types listed first as {@code slot:<type>} */
  public static void toolStat(Consumer<String> onPick) {
    List<Entry> entries = new ArrayList<>();
    for (IToolStat<?> stat : ToolStats.getAllStats()) {
      if (stat instanceof INumericToolStat<?>) {
        String id = stat.getName().toString();
        entries.add(new Entry(id, Names.toolStat(id), Component.literal(id), 0, ItemStack.EMPTY));
      }
    }
    entries.sort(Comparator.comparing(e -> e.label().getString()));
    List<String> slotTypes = EntryGenerator.MODIFIER_SLOT_TYPES;
    for (int i = slotTypes.size() - 1; i >= 0; i--) {
      entries.add(0, new Entry(SLOT_PREFIX + slotTypes.get(i), Names.tr("slot_type." + slotTypes.get(i)), Names.tr("picker.slot_sub"), Theme.ACCENT, ItemStack.EMPTY));
    }
    new SearchPopup(Names.tr("picker.tool_stat"), entries, entry -> onPick.accept(entry.id())).open();
  }

  /** Mob (potion) effects */
  public static void mobEffect(Consumer<String> onPick) {
    List<Entry> entries = new ArrayList<>();
    for (ResourceLocation id : BuiltInRegistries.MOB_EFFECT.keySet()) {
      entries.add(new Entry(id.toString(), Names.effect(id.toString()), Component.literal(id.toString()), Names.effectColor(id.toString()), ItemStack.EMPTY));
    }
    entries.sort(Comparator.comparing(e -> e.label().getString()));
    new SearchPopup(Names.tr("picker.effect"), entries, entry -> onPick.accept(entry.id())).open();
  }

  public static void attribute(Consumer<String> onPick) {
    List<Entry> entries = new ArrayList<>();
    for (ResourceLocation id : BuiltInRegistries.ATTRIBUTE.keySet()) {
      entries.add(new Entry(id.toString(), Names.attribute(id.toString()), Component.literal(id.toString()), 0, ItemStack.EMPTY));
    }
    entries.sort(Comparator.comparing(e -> e.label().getString()));
    new SearchPopup(Names.tr("picker.attribute"), entries, entry -> onPick.accept(entry.id())).open();
  }

  /** Loaded Tinkers' materials */
  public static void material(Component title, Consumer<IMaterial> onPick) {
    List<Entry> entries = new ArrayList<>();
    List<IMaterial> materials = new ArrayList<>(MaterialRegistry.getInstance().getAllMaterials());
    materials.sort(Comparator.comparing(m -> m.getIdentifier().toString()));
    for (IMaterial material : materials) {
      MaterialId id = material.getIdentifier();
      Component name = Component.translatableWithFallback("material." + id.getNamespace() + "." + id.getPath(), id.getPath());
      entries.add(new Entry(id.toString(), name, Component.literal(id.toString()), MaterialImporter.materialColor(id), ItemStack.EMPTY));
    }
    new SearchPopup(title, entries, entry -> {
      MaterialId id = MaterialId.tryParse(entry.id());
      if (id != null) {
        onPick.accept(MaterialRegistry.getMaterial(id));
      }
    }).open();
  }

  /** Material stat types */
  public static void statType(Consumer<String> onPick) {
    List<Entry> entries = new ArrayList<>();
    for (StatSchema.StatInfo info : StatSchema.all()) {
      entries.add(new Entry(info.id(), Names.statType(info.id()), Component.literal(info.id()), 0, ItemStack.EMPTY));
    }
    new SearchPopup(Names.tr("picker.stat_type"), entries, entry -> onPick.accept(entry.id())).open();
  }
}
