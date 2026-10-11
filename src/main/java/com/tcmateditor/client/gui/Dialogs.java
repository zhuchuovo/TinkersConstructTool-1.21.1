package com.tcmateditor.client.gui;

import com.tcmateditor.client.ClientSettings;
import com.tcmateditor.client.EditorSession;
import com.tcmateditor.client.gui.ui.Button;
import com.tcmateditor.client.gui.ui.Label;
import com.tcmateditor.client.gui.ui.Popup;
import com.tcmateditor.client.gui.ui.TextField;
import com.tcmateditor.client.gui.ui.UI;
import com.tcmateditor.model.Workspace;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
    Button close = new Button(Names.tr("button.done"), popup::close).style(Button.Style.PRIMARY);
    close.w = Math.max(close.w, 50);
    popup.add(close).at(width - 10 - close.w, y);
    popup.h = y + 26;
    UI.openPopup(popup);
    popup.center(screenW(), screenH());
  }
}
