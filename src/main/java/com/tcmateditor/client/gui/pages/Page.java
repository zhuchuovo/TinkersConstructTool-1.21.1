package com.tcmateditor.client.gui.pages;

import com.tcmateditor.client.ClientApply;
import com.tcmateditor.client.gui.Form;
import com.tcmateditor.client.gui.ui.UI;

/** A tab of the editor */
public interface Page {
  void build(Form form);

  /** Marks the workspace as edited */
  static void changed() {
    ClientApply.markEdited();
  }

  /** Marks as edited and rebuilds the page, for structural changes */
  static void rebuild() {
    ClientApply.markEdited();
    UI.requestRebuild();
  }
}
