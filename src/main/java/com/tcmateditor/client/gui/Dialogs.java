package com.tcmateditor.client.gui;

import com.tcmateditor.client.ClientSettings;
import com.tcmateditor.client.EditorSession;
import com.tcmateditor.client.Exporter;
import com.tcmateditor.client.UiLanguage;
import com.tcmateditor.client.gui.ui.Button;
import com.tcmateditor.client.gui.ui.Label;
import com.tcmateditor.client.gui.ui.Popup;
import com.tcmateditor.client.gui.ui.ScrollView;
import com.tcmateditor.client.gui.ui.TextField;
import com.tcmateditor.client.gui.ui.UI;
import com.tcmateditor.gen.DataGenerator.Problem;
import com.tcmateditor.model.Languages;
import com.tcmateditor.model.Workspace;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Modal dialogs */
public final class Dialogs {
  private Dialogs() {}

  private static int screenW() {
    return Minecraft.getInstance().getWindow().getGuiScaledWidth();
  }

  private static int screenH() {
    return Minecraft.getInstance().getWindow().getGuiScaledHeight();
  }

  /** Yes/no question */
  public static void confirm(Component title, Component message, Component confirmText, Runnable onConfirm) {
    Popup popup = new Popup();
    popup.modal = true;
    int width = Math.min(240, screenW() - 20);
    popup.w = width;
    popup.add(new Label(title, Theme.TEXT)).at(10, 10);
    Label body = popup.add(new Label(message, Theme.TEXT_DIM).wrap(width - 20));
    body.at(10, 26);
    int buttonsY = 26 + body.h + 10;
    Button ok = new Button(confirmText, () -> {
      popup.close();
      onConfirm.run();
    }).style(Button.Style.DANGER);
    ok.w = Math.max(ok.w, 50);
    popup.add(ok).at(width - 10 - ok.w, buttonsY);
    Button cancel = new Button(Names.tr("button.cancel"), popup::close);
    cancel.w = Math.max(cancel.w, 50);
    popup.add(cancel).at(width - 14 - ok.w - cancel.w, buttonsY);
    popup.h = buttonsY + 26;
    UI.openPopup(popup);
    popup.center(screenW(), screenH());
  }

  /** Where the exported files go, plus anything that was left out */
  public static void exported(Exporter.Result result) {
    Popup popup = new Popup();
    popup.modal = true;
    int width = Math.min(320, screenW() - 20);
    popup.w = width;
    popup.add(new Label(Names.tr("export.title"), Theme.ACCENT)).at(10, 10);
    ScrollView body = popup.add(new ScrollView());
    body.at(0, 24).size(width, 0);
    int y = 2;
    String jar = result.modJar().getFileName().toString();
    String data = result.dataPack().getFileName().toString();
    String resources = result.resourcePack().getFileName().toString();
    List<Component> exported = new java.util.ArrayList<>(List.of(Names.tr("export.jar", jar), Names.tr("export.packs", data, resources)));
    if (result.fluidScript() != null) {
      exported.add(Names.tr("export.fluid_script", result.fluidScript().getFileName().toString()));
    }
    if (result.kubeAssets() != null) {
      exported.add(Names.tr("export.fluid_assets", result.kubeAssets().getFileName().toString()));
    }
    for (Component text : exported) {
      Label line = body.add(new Label(Component.literal("• ").append(text), Theme.TEXT).wrap(width - 24));
      line.at(10, y);
      y += line.h + 4;
    }
    if (result.fluidScript() != null) {
      Label note = body.add(new Label(Names.tr("export.fluids_note"), Theme.WARNING).wrap(width - 24));
      note.at(10, y);
      y += note.h + 4;
    }
    List<Problem> problems = result.problems();
    if (!problems.isEmpty()) {
      y += 4;
      Label header = body.add(new Label(Names.tr("export.problems", problems.size()), Theme.WARNING).wrap(width - 24));
      header.at(10, y);
      y += header.h + 3;
      int shown = Math.min(problems.size(), 6);
      for (int i = 0; i < shown; i++) {
        Label line = body.add(new Label(problemText(problems.get(i)), Theme.TEXT_DIM).wrap(width - 34));
        line.at(18, y);
        y += line.h + 2;
      }
      if (problems.size() > shown) {
        body.add(new Label(Names.tr("export.more", problems.size() - shown), Theme.TEXT_FAINT)).at(18, y);
        y += 11;
      }
    }
    body.contentHeight = y;
    body.h = Math.min(y, Math.max(40, screenH() - 74));
    y = body.ly + body.h + 8;
    Button close = new Button(Names.tr("button.done"), popup::close).style(Button.Style.PRIMARY);
    close.w = Math.max(close.w, 50);
    popup.add(close).at(width - 10 - close.w, y);
    Button open = new Button(Names.tr("button.open_folder"), () -> Util.getPlatform().openFile(result.folder().toFile()));
    popup.add(open).at(width - 14 - close.w - open.w, y);
    popup.h = y + 26;
    UI.openPopup(popup);
    popup.center(screenW(), screenH());
  }

  /** "owner: message" for a generation problem */
  public static Component problemText(Problem problem) {
    return Names.tr("problem.line", problem.owner(), Names.tr("problem." + problem.key(), problem.detail()));
  }

  /** Short help and workspace settings */
  public static void help() {
    Popup popup = new Popup();
    popup.modal = true;
    int width = Math.min(300, screenW() - 20);
    popup.w = width;
    popup.add(new Label(Names.tr("help.title"), Theme.ACCENT)).at(10, 10);
    int y = 26;
    for (int i = 1; i <= 8; i++) {
      Label line = popup.add(new Label(Component.literal("• ").append(Names.tr("help." + i)), Theme.TEXT_DIM).wrap(width - 20));
      line.at(10, y);
      y += line.h + 4;
    }
    y += 4;
    popup.add(new Label(Names.tr("label.namespace"), Theme.TEXT)).at(10, y + 4);
    Workspace workspace = EditorSession.workspace();
    TextField namespace = popup.add(new TextField(workspace.namespace, 110));
    namespace.at(90, y);
    namespace.validator(Workspace::isValidNamespace);
    namespace.onChange(text -> {
      if (Workspace.isValidNamespace(text)) {
        workspace.namespace = text;
      }
    });
    namespace.tooltip(Names.tr("hint.namespace"));
    y += 22;
    popup.add(new Label(Names.tr("label.theme"), Theme.TEXT)).at(10, y + 4);
    int chipX = 90;
    for (boolean light : new boolean[] { false, true }) {
      Button chip = new Button(Names.tr(light ? "theme.light" : "theme.dark"), () -> {
        if (ClientSettings.isLightTheme() != light) {
          ClientSettings.setLightTheme(light);
          // reopen so the dialog picks up the new colors
          popup.close();
          UI.requestRebuild();
          help();
        }
      }).style(Button.Style.CHIP).selected(() -> ClientSettings.isLightTheme() == light);
      chip.w = Math.max(chip.w, 44);
      popup.add(chip).at(chipX, y);
      chipX += chip.w + 4;
    }
    y += 22;
    // UI language, also the language texts are written in
    popup.add(new Label(Names.tr("label.ui_language"), Theme.TEXT)).at(10, y + 4).tooltip(Names.tr("hint.ui_language"));
    chipX = 90;
    List<String> choices = new java.util.ArrayList<>(List.of(""));
    choices.addAll(Languages.CODES);
    for (String code : choices) {
      Component label = code.isEmpty() ? Names.tr("ui_language.auto") : Component.literal(Languages.LABELS.get(code));
      Button chip = new Button(label, () -> {
        if (!ClientSettings.uiLanguage().equals(code)) {
          ClientSettings.setUiLanguage(code);
          UiLanguage.ensureActive();
          // reopen so everything picks up the new language
          popup.close();
          UI.requestRebuild();
          help();
        }
      }).style(Button.Style.CHIP).selected(() -> ClientSettings.uiLanguage().equals(code));
      chip.w = Math.max(chip.w, 44);
      if (chipX + chip.w > width - 10) {
        y += 18;
        chipX = 90;
      }
      popup.add(chip).at(chipX, y);
      chipX += chip.w + 4;
    }
    y += 22;
    Button close = new Button(Names.tr("button.done"), popup::close).style(Button.Style.PRIMARY);
    close.w = Math.max(close.w, 50);
    popup.add(close).at(width - 10 - close.w, y);
    popup.h = y + 26;
    UI.openPopup(popup);
    popup.center(screenW(), screenH());
  }
}
