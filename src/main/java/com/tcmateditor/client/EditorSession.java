package com.tcmateditor.client;

import com.tcmateditor.model.EntryDoc;
import com.tcmateditor.model.FluidDoc;
import com.tcmateditor.model.Languages;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.UpgradeDoc;
import com.tcmateditor.model.Workspace;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Editor state kept between screen openings */
public final class EditorSession {
  private EditorSession() {}

  public enum Mode { MATERIALS, ENTRIES, UPGRADES, FLUIDS }

  private static Workspace workspace;
  public static Mode mode = Mode.MATERIALS;
  @Nullable
  public static MaterialDoc material;
  @Nullable
  public static EntryDoc entry;
  @Nullable
  public static UpgradeDoc upgrade;
  @Nullable
  public static FluidDoc fluid;
  public static int materialTab = 0;
  public static int entryTab = 0;
  public static int upgradeTab = 0;
  public static int fluidTab = 0;
  /** Apply data changes automatically shortly after editing, off by default so only Apply / Ctrl+Shift+S reloads */
  public static boolean autoApply = false;
  /** Scroll positions per page key */
  public static final Map<String,Double> SCROLL = new HashMap<>();
  public static String listSearch = "";

  /** Status line */
  public static Component status = Component.empty();
  public static int statusColor = 0xFF9AA0AE;
  public static long statusTime = 0;

  public static Workspace workspace() {
    if (workspace == null) {
      workspace = Workspace.load();
      if (!workspace.materials.isEmpty()) {
        material = workspace.materials.get(0);
      }
      if (!workspace.entries.isEmpty()) {
        entry = workspace.entries.get(0);
      }
      if (!workspace.fluids.isEmpty()) fluid = workspace.fluids.get(0);
    }
    return workspace;
  }

  /** Language texts are edited in, the same as the UI language */
  public static String editLanguage() {
    return UiLanguage.code();
  }

  /** Language used for the UI and names in lists, chosen in the settings */
  public static String uiLanguage() {
    return UiLanguage.code();
  }

  public static void setStatus(Component text, int color) {
    status = text;
    statusColor = color;
    statusTime = System.currentTimeMillis();
  }
}
