package com.tcmateditor.client.gui.pages;

import com.tcmateditor.client.gui.Form;
import com.tcmateditor.client.gui.Icons;
import com.tcmateditor.client.gui.Names;
import com.tcmateditor.client.gui.Pickers;
import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.client.gui.ui.Button;
import com.tcmateditor.client.gui.ui.ItemIcon;
import com.tcmateditor.client.gui.ui.Label;
import com.tcmateditor.model.UpgradeDoc;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Which equipment may (and may not) receive the upgrade */
public class UpgradeToolsPage implements Page {
  /** Tinkers' tool tags offered as quick choices */
  public static final List<String> CATEGORIES = List.of(
    "modifiable", "melee", "melee/weapon", "harvest", "aoe", "ranged", "ranged/bows", "ranged/crossbows",
    "armor", "armor/helmets", "armor/chestplate", "armor/leggings", "armor/boots", "shields", "staffs", "ammo", "fishing_rods");

  private final UpgradeDoc doc;

  public UpgradeToolsPage(UpgradeDoc doc) {
    this.doc = doc;
  }

  private static String tag(String category) {
    return "#tconstruct:modifiable" + (category.equals("modifiable") ? "" : "/" + category);
  }

  public static Component categoryName(String ref) {
    String prefix = "#tconstruct:modifiable";
    if (ref.equals(prefix)) {
      return Names.tr("tooltag.modifiable");
    }
    if (ref.startsWith(prefix + "/")) {
      String category = ref.substring(prefix.length() + 1);
      if (CATEGORIES.contains(category)) {
        return Names.tr("tooltag." + category.replace('/', '.'));
      }
    }
    if (!ref.startsWith("#")) {
      ResourceLocation id = ResourceLocation.tryParse(ref);
      if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
        return new ItemStack(BuiltInRegistries.ITEM.get(id)).getHoverName();
      }
    }
    return Component.literal(ref);
  }

  @Override
  public void build(Form form) {
    form.hint(Names.tr("page.upgrade_tools.hint"));
    form.header(Names.tr("section.allow_tools"));
    refList(form, doc.allowTools, false);
    form.header(Names.tr("section.deny_tools"));
    form.hint(Names.tr("section.deny_tools.hint"));
    refList(form, doc.denyTools, true);

    // preview of Tinkers' equipment that can get the upgrade
    form.header(Names.tr("section.tools_preview"));
    List<ItemStack> allowed = new ArrayList<>();
    int total = 0;
    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, new ResourceLocation("tconstruct", "modifiable")))) {
      total++;
      if (matchesAny(holder, doc.allowTools) && !matchesAny(holder, doc.denyTools)) {
        allowed.add(slimeknights.tconstruct.library.tools.item.IModifiableDisplay.getDisplayStack(holder.value()));
      }
    }
    form.hint(Names.tr("section.tools_preview.count", allowed.size(), total));
    int x = 0;
    for (ItemStack stack : allowed) {
      if (x + 18 > form.width()) {
        form.next(16);
        x = 0;
      }
      ItemIcon icon = new ItemIcon(() -> stack);
      icon.frame = false;
      form.place(icon, x);
      x += 18;
    }
    if (x > 0) {
      form.next(16);
    }
    form.finish();
  }

  private static boolean matchesAny(Holder<Item> holder, List<String> refs) {
    for (String ref : refs) {
      if (ref.startsWith("#")) {
        ResourceLocation tag = ResourceLocation.tryParse(ref.substring(1));
        if (tag != null && holder.is(TagKey.create(Registries.ITEM, tag))) {
          return true;
        }
      } else {
        ResourceLocation id = ResourceLocation.tryParse(ref);
        if (id != null && holder.is(id)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Category chips plus any extra items or tags */
  private void refList(Form form, List<String> refs, boolean deny) {
    int x = 0;
    for (String category : CATEGORIES) {
      String ref = tag(category);
      Button chip = new Button(categoryName(ref), () -> {
        if (refs.contains(ref)) {
          refs.remove(ref);
        } else {
          refs.add(ref);
        }
        Page.rebuild();
      }).style(Button.Style.CHIP).selected(() -> refs.contains(ref));
      chip.h = 14;
      if (deny && refs.contains(ref)) {
        chip.dotColor = Theme.DANGER;
        chip.fit();
      }
      if (x + chip.w > form.width()) {
        form.next(14);
        x = 0;
      }
      form.place(chip, x).tooltip(Component.literal(ref));
      x += chip.w + 3;
    }
    form.next(14);
    // extra items and tags that are not in the quick list
    for (String ref : new ArrayList<>(refs)) {
      if (ref.startsWith("#tconstruct:modifiable") && CATEGORIES.stream().anyMatch(c -> tag(c).equals(ref))) {
        continue;
      }
      form.place(new ItemIcon(() -> Icons.forRef(ref)).size(18, 16), 0);
      form.place(new Label(categoryName(ref), Theme.TEXT), 22).at(form.pad + 22, form.y + 4).size(form.width() - 44, 9).tooltip(Component.literal(ref));
      form.place(new Button(Component.literal("×"), () -> {
        refs.remove(ref);
        Page.rebuild();
      }).style(Button.Style.GHOST).size(16, 16), form.width() - 16).tooltip(Names.tr("button.remove"));
      form.next(16);
    }
    form.full(new Button(Names.tr("button.add_tool_ref"), () -> Pickers.item(picked -> {
      if (!refs.contains(picked)) {
        refs.add(picked);
        Page.rebuild();
      }
    })).fit()).tooltip(Names.tr("hint.add_tool_ref"));
    if (!deny && refs.isEmpty()) {
      form.full(new Label(Names.tr("warn.no_tools"), Theme.WARNING).wrap(form.width()));
    }
  }
}
