package com.tcmateditor.client.gui.pages;

import com.tcmateditor.client.gui.Form;
import com.tcmateditor.client.gui.Names;
import com.tcmateditor.client.gui.ui.ColorSwatch;
import com.tcmateditor.client.gui.ui.Toggle;
import com.tcmateditor.client.texture.Colors;
import com.tcmateditor.model.MaterialDoc;
import net.minecraft.network.chat.Component;

/** Material name, flavor text and encyclopedia description */
public class InfoPage implements Page {
  private final MaterialDoc doc;

  public InfoPage(MaterialDoc doc) {
    this.doc = doc;
  }

  @Override
  public void build(Form form) {
    form.hint(Names.tr("page.info.hint"));
    LangEditor.build(form, doc.lang, Names.tr("label.encyclopedia"), doc::nameColor);

    form.header(Names.tr("section.name_color"));
    boolean custom = !doc.textColor.isBlank();
    form.row(Names.tr("label.follow_texture"), new Toggle(Component.empty(), () -> doc.textColor.isBlank(), follow -> {
      doc.textColor = follow ? "" : Colors.hex(doc.texture.mainColor());
      Page.rebuild();
    })).tooltip(Names.tr("hint.follow_texture"));
    if (custom) {
      form.row(Names.tr("label.color"), new ColorSwatch(doc::nameColor, c -> {
        doc.textColor = Colors.hex(c);
        Page.changed();
      }).withHex());
    }
    form.finish();
  }
}
