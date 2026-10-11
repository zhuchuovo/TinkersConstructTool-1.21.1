package com.tcmateditor.client.gui.pages;

import com.google.gson.JsonObject;
import com.tcmateditor.client.gui.Form;
import com.tcmateditor.client.gui.Names;
import com.tcmateditor.client.gui.StatSchema;
import com.tcmateditor.client.gui.StatSchema.Field;
import com.tcmateditor.client.gui.StatSchema.StatInfo;
import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.client.gui.ui.Dropdown;
import com.tcmateditor.client.gui.ui.Label;
import com.tcmateditor.client.gui.ui.NumberField;
import com.tcmateditor.client.gui.ui.TextField;
import com.tcmateditor.client.gui.ui.Toggle;
import com.tcmateditor.client.gui.ui.UIElement;
import com.tcmateditor.gen.DataGenerator;
import com.tcmateditor.model.MaterialDoc;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Stats for each part type. Enabling a part type allows building that part from the material. */
public class StatsPage implements Page {
  private final MaterialDoc doc;

  public StatsPage(MaterialDoc doc) {
    this.doc = doc;
  }

  @Override
  public void build(Form form) {
    form.hint(Names.tr("page.stats.hint"));
    List<StatInfo> all = StatSchema.all();
    // include stat types stored in the doc that are no longer registered
    for (String id : doc.stats.keySet()) {
      if (all.stream().noneMatch(i -> i.id().equals(id))) {
        all.add(StatSchema.get(id));
      }
    }
    for (String group : StatSchema.GROUPS) {
      List<StatInfo> inGroup = all.stream().filter(i -> i.group().equals(group)).toList();
      if (inGroup.isEmpty()) {
        continue;
      }
      long enabled = inGroup.stream().filter(i -> doc.stats.containsKey(i.id())).count();
      form.header(Names.group(group).copy().append(Component.literal(enabled > 0 ? "  " + enabled + "/" + inGroup.size() : "").withColor(Theme.TEXT_FAINT)));
      for (StatInfo info : inGroup) {
        buildStat(form, info);
      }
    }
    form.finish();
  }

  private void buildStat(Form form, StatInfo info) {
    boolean enabled = doc.stats.containsKey(info.id());
    Toggle toggle = new Toggle(Names.statType(info.id()), () -> doc.stats.containsKey(info.id()), on -> {
      if (on) {
        doc.stats.put(info.id(), StatSchema.defaults(info));
      } else {
        doc.stats.remove(info.id());
      }
      Page.rebuild();
    });
    toggle.size(form.width(), 14);
    toggle.labelColor = enabled ? Theme.TEXT : Theme.TEXT_DIM;
    if (info.fields().isEmpty()) {
      toggle.tooltip(Names.tr("hint.statless"));
    }
    form.full(toggle);
    if (!enabled || info.fields().isEmpty()) {
      return;
    }
    JsonObject json = doc.stats.get(info.id());
    // fields in two columns inside an indented card
    int indent = 22;
    int columnW = (form.width() - indent) / 2;
    int labelW = Math.min(70, columnW / 2);
    int fieldW = columnW - labelW - 6;
    int startY = form.y;
    List<Field> fields = info.fields();
    int rows = (fields.size() + 1) / 2;
    form.view.add(new UIElement() {
      @Override
      public void render(GuiGraphics g, int mx, int my, float partialTick) {
        Theme.box(g, x, y, w, h, Theme.PANEL);
        g.fill(x, y, x + 2, y + h, Theme.ACCENT_DIM);
      }
    }.at(form.pad + indent - 6, startY - 2).size(form.width() - indent + 6, rows * 20 + 2));
    for (int i = 0; i < fields.size(); i++) {
      Field field = fields.get(i);
      int column = i % 2;
      int row = i / 2;
      int fx = indent + column * columnW;
      int fy = startY + row * 20 + 1;
      form.view.add(new Label(Names.field(field.key()), Theme.TEXT_DIM)).at(form.pad + fx, fy + 4).size(labelW, 9)
        .tooltip(Component.translatableWithFallback("tcmateditor.field." + field.key() + ".hint", field.key()));
      UIElement control = control(json, field, fieldW);
      form.view.add(control).at(form.pad + fx + labelW, fy);
    }
    form.y = startY + rows * 20 + 6;
  }

  private UIElement control(JsonObject json, Field field, int width) {
    String key = field.key();
    return switch (field.kind()) {
      case INT -> new NumberField(DataGenerator.getNumber(json, key, field.def()), true, field.min(), field.max(), field.step(), width)
        .onValue(v -> { json.addProperty(key, (int) Math.round(v)); Page.changed(); });
      case FLOAT -> new NumberField(DataGenerator.getNumber(json, key, field.def()), false, field.min(), field.max(), field.step(), width)
        .onValue(v -> { json.addProperty(key, (float) v); Page.changed(); });
      case PERCENT -> new NumberField(Math.round(DataGenerator.getNumber(json, key, 0) * 1000) / 10.0, false, field.min(), field.max(), field.step(), width)
        .suffix("%")
        .onValue(v -> { json.addProperty(key, (float) (v / 100.0)); Page.changed(); });
      case TIER -> {
        List<String> tiers = new ArrayList<>(StatSchema.TIERS);
        String current = DataGenerator.getString(json, key, "minecraft:iron");
        if (!tiers.contains(current)) {
          tiers.add(current);
        }
        yield new Dropdown<>(tiers, Names::harvestTier, () -> DataGenerator.getString(json, key, "minecraft:iron"), v -> {
          json.addProperty(key, v);
          Page.changed();
        }, width);
      }
      case TEXT -> new TextField(DataGenerator.getString(json, key, ""), width).onChange(v -> { json.addProperty(key, v); Page.changed(); });
    };
  }
}
