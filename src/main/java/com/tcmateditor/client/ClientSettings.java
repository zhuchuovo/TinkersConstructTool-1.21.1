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

  private static void save() {
    JsonObject json = new JsonObject();
    json.addProperty("light_theme", lightTheme);
    try {
      Files.createDirectories(file().getParent());
      Files.writeString(file(), json.toString(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      TCMatEditor.LOG.warn("Failed to save client settings", e);
    }
  }
}
