package com.tcmateditor.client.gui.pages;

import com.tcmateditor.client.EditorSession;
import com.tcmateditor.client.gui.Form;
import com.tcmateditor.client.gui.Icons;
import com.tcmateditor.client.gui.Names;
import com.tcmateditor.client.gui.Pickers;
import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.client.gui.ui.AnvilSlotsEditor;
import com.tcmateditor.client.gui.ui.Button;
import com.tcmateditor.client.gui.ui.Dropdown;
import com.tcmateditor.client.gui.ui.HelpBadge;
import com.tcmateditor.client.gui.ui.ItemIcon;
import com.tcmateditor.client.gui.ui.Label;
import com.tcmateditor.client.gui.ui.NumberField;
import com.tcmateditor.client.gui.ui.TextField;
import com.tcmateditor.client.gui.ui.Toggle;
import com.tcmateditor.gen.GenUtil;
import com.tcmateditor.gen.UpgradeGenerator;
import com.tcmateditor.model.AnvilSlots;
import com.tcmateditor.model.UpgradeDoc;
import com.tcmateditor.model.Workspace;
import net.minecraft.network.chat.Component;

/** Upgrade recipe: which entry it applies, what goes in each anvil slot and slot/level rules */
public class UpgradeRecipePage implements Page {
  /** Selected anvil slot, kept while editing */
  private static int selectedSlot = 0;

  private final UpgradeDoc doc;

  public UpgradeRecipePage(UpgradeDoc doc) {
    this.doc = doc;
  }

  @Override
  public void build(Form form) {
    form.hint(Names.tr("page.upgrade.hint"));

    // what it applies
    Button modifier = new Button(() -> doc.modifier.isBlank() ? Names.tr("label.pick_modifier") : Names.modifier(doc.modifier), () -> Pickers.modifier(id -> {
      doc.modifier = id;
      Page.rebuild();
    }));
    modifier.size(form.controlWidth(), 16);
    modifier.dotColor = doc.modifier.isBlank() ? 0 : Names.modifierColor(doc.modifier);
    modifier.tooltip(() -> Component.literal(doc.modifier.isBlank() ? "-" : doc.modifier));
    form.row(Names.tr("label.upgrade_modifier"), modifier);
    TextField id = form.row(Names.tr("label.id"), new TextField(doc.id, form.controlWidth()));
    id.validator(text -> Workspace.isValidId(text) && (text.equals(doc.id) || EditorSession.workspace().findUpgrade(text) == null));
    id.onChange(text -> {
      if (id.isValid() && !text.equals(doc.id)) {
        doc.id = text;
        Page.changed();
      }
    });
    id.tooltip(Names.tr("hint.upgrade_id"));

    // anvil slots
    int headerY = form.y;
    form.header(Names.tr("section.anvil"));
    form.view.add(new HelpBadge()).at(form.pad + Theme.font().width(Names.tr("section.anvil")) + 6, headerY + 3);
    form.hint(Names.tr("section.anvil.hint"));

    AnvilSlotsEditor anvil = new AnvilSlotsEditor(() -> doc, () -> selectedSlot, slot -> {
      if (slot != selectedSlot) {
        selectedSlot = slot;
        Page.rebuild();
      }
    }, () -> {
      doc.slot(selectedSlot).ingredient = "";
      doc.slot(selectedSlot).count = 1;
      Page.rebuild();
    });
    int top = form.y + 4;
    form.view.add(anvil).at(form.pad + 6, top + 4);

    // editor for the selected slot, right of the anvil or below on narrow screens
    int panelX = anvil.w + 22;
    int panelW = form.width() - panelX;
    int panelY = top;
    if (panelW < 150) {
      panelX = 0;
      panelW = form.width();
      panelY = top + anvil.h + 14;
    }
    UpgradeDoc.SlotInput input = doc.slot(selectedSlot);
    form.view.add(new Label(AnvilSlotsEditor.slotName(selectedSlot), Theme.TEXT)).at(form.pad + panelX, panelY + 2);
    int rowY = panelY + 16;
    TextField[] field = new TextField[1];
    form.view.add(new ItemIcon(() -> Icons.forRef(input.ingredient)).onClick(() -> Pickers.item(picked -> {
      input.ingredient = picked;
      field[0].setValue(picked);
      Page.changed();
    })).size(18, 16)).at(form.pad + panelX, rowY).tooltip(Names.tr("hint.pick_item"));
    field[0] = new TextField(input.ingredient, Math.max(60, panelW - 22));
    field[0].hint(Component.literal("minecraft:diamond / #forge:gems"));
    field[0].validator(text -> text.isBlank() || GenUtil.isKnownItem(text));
    field[0].onChange(text -> {
      input.ingredient = text.trim();
      Page.changed();
    });
    form.view.add(field[0]).at(form.pad + panelX + 20, rowY);
    rowY += 20;
    form.view.add(new Label(Names.tr("label.count"), Theme.TEXT_DIM)).at(form.pad + panelX, rowY + 4);
    int countX = panelX + Theme.font().width(Names.tr("label.count")) + 6;
    form.view.add(new NumberField(input.count, true, 1, 64, 1, 44).onValue(v -> {
      input.count = (int) v;
      Page.changed();
    })).at(form.pad + countX, rowY);
    Button clear = new Button(Names.tr("button.clear_slot"), () -> {
      input.ingredient = "";
      input.count = 1;
      Page.rebuild();
    }).style(Button.Style.GHOST);
    form.view.add(clear).at(form.pad + countX + 50, rowY);
    rowY += 22;
    if (doc.strictSlots && !AnvilSlots.onStation(selectedSlot)) {
      Label warn = new Label(Names.tr("hint.anvil_only"), Theme.WARNING).wrap(panelW);
      form.view.add(warn).at(form.pad + panelX, rowY);
      rowY += warn.h + 4;
    }
    form.y = Math.max(top + anvil.h + 12, rowY + 4);
    if (!doc.hasInputs()) {
      form.full(new Label(Names.tr("warn.no_inputs"), Theme.WARNING).wrap(form.width()));
    }

    // rules
    form.header(Names.tr("section.upgrade_rules"));
    form.row(Names.tr("label.slot_type"), new Dropdown<>(UpgradeGenerator.SLOT_TYPES, t -> Names.tr("slot_type." + t), () -> doc.slotType, t -> {
      doc.slotType = t;
      Page.rebuild();
    }, 110)).tooltip(Names.tr("hint.slot_type"));
    if (!"none".equals(doc.slotType)) {
      form.row(Names.tr("label.slot_count"), new NumberField(doc.slotCount, true, 1, 10, 1, 44).onValue(v -> {
        doc.slotCount = (int) v;
        Page.changed();
      }));
    }
    form.row(Names.tr("label.max_level"), new NumberField(doc.maxLevel, true, 1, 100, 1, 44).onValue(v -> {
      doc.maxLevel = (int) v;
      Page.changed();
    })).tooltip(Names.tr("hint.max_level"));
    form.row(Names.tr("label.allow_crystal"), new Toggle(Component.empty(), () -> doc.allowCrystal, v -> {
      doc.allowCrystal = v;
      Page.changed();
    })).tooltip(Names.tr("hint.allow_crystal"));
    form.row(Names.tr("label.strict_slots"), new Toggle(Component.empty(), () -> doc.strictSlots, v -> {
      doc.strictSlots = v;
      Page.rebuild();
    })).tooltip(Names.tr("hint.strict_slots"));
    form.hint(Names.tr(doc.strictSlots ? "hint.strict_on" : "hint.strict_off"));
    if (doc.modifier.isBlank()) {
      form.full(new Label(Names.tr("warn.no_modifier"), Theme.WARNING).wrap(form.width()));
    }
    form.finish();
  }
}
