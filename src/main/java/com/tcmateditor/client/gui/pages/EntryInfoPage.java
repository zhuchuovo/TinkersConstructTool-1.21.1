package com.tcmateditor.client.gui.pages;

import com.tcmateditor.client.gui.Form;
import com.tcmateditor.client.gui.Names;
import com.tcmateditor.model.EntryDoc;

/** Texts of a custom entry */
public class EntryInfoPage implements Page {
  private final EntryDoc doc;

  public EntryInfoPage(EntryDoc doc) {
    this.doc = doc;
  }

  @Override
  public void build(Form form) {
    form.hint(Names.tr("page.entry_info.hint"));
    LangEditor.build(form, doc.lang, Names.tr("label.description"), doc::colorInt);
    form.finish();
  }
}
