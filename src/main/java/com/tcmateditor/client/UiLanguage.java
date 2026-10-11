package com.tcmateditor.client;

import com.tcmateditor.TCMatEditor;
import com.tcmateditor.model.Languages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Lets the editor show its UI in a language other than the game's.
 * The chosen language (editor texts plus vanilla and mod names) is swapped in while the editor is open and the game language is put back when it closes.
 */
public final class UiLanguage {
  private UiLanguage() {}

  /** Language loaded for the chosen code */
  @Nullable
  private static Language override;
  private static String overrideCode = "";
  /** Language that was active before swapping */
  @Nullable
  private static Language gameLanguage;

  /** Language code the editor shows and edits texts in */
  public static String code() {
    String chosen = ClientSettings.uiLanguage();
    return Languages.CODES.contains(chosen) ? chosen : Languages.bestFor(gameCode());
  }

  private static String gameCode() {
    return Minecraft.getInstance().getLanguageManager().getSelected();
  }

  /**
   * Makes sure the chosen language is active, call while the editor is open
   * @return true if the active language changed and the UI should be rebuilt
   */
  public static boolean ensureActive() {
    String chosen = ClientSettings.uiLanguage();
    if (!Languages.CODES.contains(chosen) || chosen.equals(gameCode())) {
      return restore();
    }
    Language current = Language.getInstance();
    if (override != null && current == override && chosen.equals(overrideCode)) {
      return false;
    }
    // first use, a different choice, or a resource reload put the game language back
    if (current != override) {
      gameLanguage = current;
    }
    try {
      List<String> files = new ArrayList<>(List.of("en_us"));
      if (!chosen.equals("en_us")) {
        files.add(chosen);
      }
      override = ClientLanguage.loadFrom(Minecraft.getInstance().getResourceManager(), files, false);
      overrideCode = chosen;
      inject(override);
      return true;
    } catch (RuntimeException e) {
      TCMatEditor.LOG.error("Failed to load editor language {}", chosen, e);
      override = null;
      overrideCode = "";
      return false;
    }
  }

  /**
   * Puts the game language back
   * @return true if the active language changed
   */
  public static boolean restore() {
    boolean changed = false;
    if (override != null && Language.getInstance() == override && gameLanguage != null) {
      inject(gameLanguage);
      changed = true;
    }
    override = null;
    overrideCode = "";
    gameLanguage = null;
    return changed;
  }

  private static void inject(Language language) {
    Language.inject(language);
    // I18n keeps its own copy and has no public setter
    try {
      Field field = I18n.class.getDeclaredField("language");
      field.setAccessible(true);
      field.set(null, language);
    } catch (ReflectiveOperationException | RuntimeException e) {
      TCMatEditor.LOG.debug("Could not update I18n language", e);
    }
  }
}
