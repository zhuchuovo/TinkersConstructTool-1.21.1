package com.tcmateditor.client.gui.pages;

import com.tcmateditor.client.EditorSession;
import com.tcmateditor.client.gui.Form;
import com.tcmateditor.client.gui.Names;
import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.client.gui.ui.TextArea;
import com.tcmateditor.client.gui.ui.TextField;
import com.tcmateditor.client.gui.ui.UIElement;
import com.tcmateditor.model.LangText;
import com.tcmateditor.model.Languages;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

/** Name, flavor and description editing, in the editor's UI language (chosen in the settings) */
public final class LangEditor {
  private LangEditor() {}

  /**
   * @param descLabel  Label of the long text field
   * @param color      Name color for the preview
   */
  public static void build(Form form, Map<String,LangText> lang, Component descLabel, IntSupplier color) {
    String code = EditorSession.editLanguage();
    form.hint(Names.tr("hint.editing_language", Languages.LABELS.get(code)));

    LangText text = lang.computeIfAbsent(code, c -> new LangText());
    form.row(Names.tr("label.name"), new TextField(text.name, form.controlWidth()).maxLength(64)
      .hint(Component.literal(Languages.resolve(lang, code, t -> t.name)).withStyle(style -> style.withColor(Theme.TEXT_FAINT)))
      .onChange(v -> { text.name = v; Page.changed(); }));
    form.row(Names.tr("label.flavor"), new TextField(text.flavor, form.controlWidth()).maxLength(200)
      .hint(Names.tr("hint.flavor"))
      .onChange(v -> { text.flavor = v; Page.changed(); }));
    form.row(descLabel, new TextArea(text.desc, form.controlWidth(), 64, Names.tr("hint.desc"))
      .onChange(v -> { text.desc = v.replace('\n', ' '); Page.changed(); }));

    // preview, looks like an in game tooltip
    form.header(Names.tr("section.preview"));
    form.full(new UIElement() {
      @Override
      public void render(GuiGraphics g, int mx, int my, float partialTick) {
        Font font = Theme.font();
        g.fill(x, y, x + w, y + h, 0xF0100010);
        g.renderOutline(x, y, w, h, 0xFF2D0A63);
        int yy = y + 5;
        String name = Languages.resolve(lang, code, t -> t.name);
        Theme.text(g, name.isBlank() ? "?" : name, x + 6, yy, color.getAsInt());
        yy += 12;
        String flavor = Languages.resolve(lang, code, t -> t.flavor);
        if (!flavor.isBlank()) {
          for (FormattedCharSequence line : font.split(Component.literal(flavor).withStyle(s -> s.withItalic(true)), w - 12)) {
            g.drawString(font, line, x + 6, yy, 0xFFAAAAAA, false);
            yy += 10;
          }
        }
        String desc = Languages.resolve(lang, code, t -> t.desc);
        if (!desc.isBlank()) {
          yy += 2;
          List<FormattedCharSequence> lines = font.split(Component.literal(desc), w - 12);
          for (FormattedCharSequence line : lines) {
            if (yy > y + h - 10) break;
            g.drawString(font, line, x + 6, yy, 0xFFDDDDDD, false);
            yy += 10;
          }
        }
      }
    }.size(form.width(), 80));
  }
}
