package com.tcmateditor.client.gui.pages;

import com.tcmateditor.client.EditorSession;
import com.tcmateditor.client.gui.Form;
import com.tcmateditor.client.gui.Names;
import com.tcmateditor.client.gui.ui.ColorSwatch;
import com.tcmateditor.client.gui.ui.NumberField;
import com.tcmateditor.client.gui.ui.TextField;
import com.tcmateditor.client.gui.ui.Toggle;
import com.tcmateditor.client.texture.Colors;
import com.tcmateditor.model.FluidDoc;
import com.tcmateditor.model.LangText;
import com.tcmateditor.model.Languages;
import com.tcmateditor.model.Workspace;
import net.minecraft.network.chat.Component;

public class FluidPage implements Page {
  private final FluidDoc doc;
  private final boolean info;

  public FluidPage(FluidDoc doc, boolean info) {
    this.doc = doc;
    this.info = info;
  }

  @Override
  public void build(Form form) {
    form.hint(Names.tr("page.fluid.hint"));
    if (info) {
      String code = EditorSession.editLanguage();
      form.hint(Names.tr("hint.editing_language", Languages.LABELS.get(code)));
      LangText text = doc.lang.computeIfAbsent(code, key -> new LangText());
      form.row(Names.tr("label.name"), new TextField(text.name, form.controlWidth()).maxLength(64)
        .hint(Component.literal(doc.displayName(code)))
        .onChange(value -> { text.name = value; Page.changed(); }));
      form.finish();
      return;
    }

    TextField id = form.row(Names.tr("label.id"), new TextField(doc.id, form.controlWidth()));
    id.validator(value -> Workspace.isValidId(value) && (value.equals(doc.id) || EditorSession.workspace().findFluid(value) == null));
    id.onChange(value -> {
      if (id.isValid() && !value.equals(doc.id)) { doc.id = value; Page.changed(); }
    });
    id.tooltip(Names.tr("hint.new_fluid_id"));
    if (doc.generatedTexture) {
      form.hint(Names.tr("hint.fluid_generated_settings"));
    } else {
      form.row(Names.tr("label.color"), new ColorSwatch(doc::baseColorInt, value -> {
        doc.color = Colors.hex(value);
        Page.changed();
      }).withHex());
      form.row(Names.tr("label.still_texture"), new TextField(doc.stillTexture, form.controlWidth())
        .validator(Workspace::isValidId).onChange(value -> { doc.stillTexture = value.trim(); Page.changed(); }))
        .tooltip(Names.tr("hint.fluid_texture"));
      form.row(Names.tr("label.flowing_texture"), new TextField(doc.flowingTexture, form.controlWidth())
        .validator(Workspace::isValidId).onChange(value -> { doc.flowingTexture = value.trim(); Page.changed(); }))
        .tooltip(Names.tr("hint.fluid_texture"));
    }
    form.row(Names.tr("label.temperature"), new NumberField(doc.temperature, true, 1, 100000, 50, 72).suffix("K")
      .onValue(value -> { doc.temperature = (int) value; Page.changed(); })).tooltip(Names.tr("hint.fluid_temperature"));
    form.row(Names.tr("label.density"), new NumberField(doc.density, true, 1, 100000, 100, 72)
      .onValue(value -> { doc.density = (int) value; Page.changed(); }));
    form.row(Names.tr("label.viscosity"), new NumberField(doc.viscosity, true, 1, 100000, 100, 72)
      .onValue(value -> { doc.viscosity = (int) value; Page.changed(); }));
    form.row(Names.tr("label.luminosity"), new NumberField(doc.lightLevel, true, 0, 15, 1, 72)
      .onValue(value -> { doc.lightLevel = (int) value; Page.changed(); }));
    form.row(Names.tr("label.fluid_bucket"), new Toggle(Component.empty(), () -> doc.bucket, value -> { doc.bucket = value; Page.changed(); }));
    form.row(Names.tr("label.fluid_block"), new Toggle(Component.empty(), () -> doc.block, value -> { doc.block = value; Page.changed(); }));
    form.finish();
  }
}
