package com.tcmateditor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tcmateditor.EditorPaths;
import com.tcmateditor.TCMatEditor;
import com.tcmateditor.client.gui.Theme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Personal editor preferences, stored in {@code tcmateditor/client_settings.json} */
public final class ClientSettings {
  private ClientSettings() {}

  private static boolean loaded = false;
  private static boolean lightTheme = false;
  /** Editor UI language code, empty to follow the game */
  private static String uiLanguage = "";

  private static Path file() {
    return EditorPaths.root().resolve("client_settings.json");
  }

  /** Loads once and applies the theme */
  public static void load() {
    if (loaded) {
      return;
    }
    loaded = true;
    try {
      Path file = file();
      if (Files.exists(file)) {
        JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        lightTheme = json.has("light_theme") && json.get("light_theme").getAsBoolean();
        uiLanguage = json.has("ui_language") ? json.get("ui_language").getAsString() : "";
      }
    } catch (IOException | RuntimeException e) {
      TCMatEditor.LOG.warn("Failed to read client settings", e);
    }
    Theme.apply(lightTheme);
  }

  public static boolean isLightTheme() {
    return lightTheme;
  }

  public static void setLightTheme(boolean value) {
    lightTheme = value;
    Theme.apply(value);
    save();
  }

  /** Editor UI language code, empty to follow the game */
  public static String uiLanguage() {
    load();
    return uiLanguage;
  }

  public static void setUiLanguage(String code) {
    uiLanguage = code == null ? "" : code;
    save();
  }

  private static void save() {
    JsonObject json = new JsonObject();
    json.addProperty("light_theme", lightTheme);
    json.addProperty("ui_language", uiLanguage);
    try {
      Files.createDirectories(file().getParent());
      Files.writeString(file(), json.toString(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      TCMatEditor.LOG.warn("Failed to save client settings", e);
    }
  }
}
