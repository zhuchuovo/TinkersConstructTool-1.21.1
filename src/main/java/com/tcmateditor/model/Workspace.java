package com.tcmateditor.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.tcmateditor.EditorPaths;
import com.tcmateditor.TCMatEditor;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/** All editor documents, stored as JSON files in {@link EditorPaths#workspace()} */
public class Workspace {
  public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  /** Default namespace for new materials and entries */
  public String namespace = "custommats";
  public final List<MaterialDoc> materials = new ArrayList<>();
  public final List<EntryDoc> entries = new ArrayList<>();
  public final List<UpgradeDoc> upgrades = new ArrayList<>();

  /** Settings stored in workspace.json */
  private static class Settings {
    String namespace = "custommats";
  }

  public static Workspace load() {
    Workspace workspace = new Workspace();
    Path root = EditorPaths.workspace();
    try {
      Files.createDirectories(root);
      Path settingsFile = root.resolve("workspace.json");
      if (Files.exists(settingsFile)) {
        try (Reader reader = Files.newBufferedReader(settingsFile, StandardCharsets.UTF_8)) {
          Settings settings = GSON.fromJson(reader, Settings.class);
          if (settings != null && isValidNamespace(settings.namespace)) {
            workspace.namespace = settings.namespace;
          }
        }
      }
      for (Path file : list(root.resolve("materials"))) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
          MaterialDoc doc = GSON.fromJson(reader, MaterialDoc.class);
          if (doc != null) {
            doc.validate();
            if (!doc.id.isBlank()) {
              workspace.materials.add(doc);
            }
          }
        } catch (IOException | JsonParseException e) {
          TCMatEditor.LOG.error("Failed to read material {}", file, e);
        }
      }
      for (Path file : list(root.resolve("entries"))) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
          EntryDoc doc = GSON.fromJson(reader, EntryDoc.class);
          if (doc != null) {
            doc.validate();
            if (!doc.id.isBlank()) {
              workspace.entries.add(doc);
            }
          }
        } catch (IOException | JsonParseException e) {
          TCMatEditor.LOG.error("Failed to read entry {}", file, e);
        }
      }
      for (Path file : list(root.resolve("upgrades"))) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
          UpgradeDoc doc = GSON.fromJson(reader, UpgradeDoc.class);
          if (doc != null) {
            doc.validate();
            if (!doc.id.isBlank()) {
              workspace.upgrades.add(doc);
            }
          }
        } catch (IOException | JsonParseException e) {
          TCMatEditor.LOG.error("Failed to read upgrade {}", file, e);
        }
      }
    } catch (IOException | JsonParseException e) {
      TCMatEditor.LOG.error("Failed to load editor workspace", e);
    }
    workspace.materials.sort((a, b) -> a.id.compareTo(b.id));
    workspace.entries.sort((a, b) -> a.id.compareTo(b.id));
    workspace.upgrades.sort((a, b) -> a.id.compareTo(b.id));
    return workspace;
  }

  private static List<Path> list(Path folder) throws IOException {
    if (!Files.isDirectory(folder)) {
      return List.of();
    }
    try (Stream<Path> stream = Files.list(folder)) {
      return stream.filter(p -> p.toString().endsWith(".json")).sorted().toList();
    }
  }

  /** Saves every document, removing files of deleted documents */
  public void save() throws IOException {
    Path root = EditorPaths.workspace();
    Files.createDirectories(root);
    Settings settings = new Settings();
    settings.namespace = namespace;
    Files.writeString(root.resolve("workspace.json"), GSON.toJson(settings), StandardCharsets.UTF_8);

    Path materialFolder = root.resolve("materials");
    Files.createDirectories(materialFolder);
    Set<Path> written = new HashSet<>();
    for (MaterialDoc doc : materials) {
      Path file = materialFolder.resolve(fileName(doc.id));
      Files.writeString(file, GSON.toJson(doc), StandardCharsets.UTF_8);
      written.add(file);
    }
    for (Path file : list(materialFolder)) {
      if (!written.contains(file)) {
        Files.deleteIfExists(file);
      }
    }

    Path entryFolder = root.resolve("entries");
    Files.createDirectories(entryFolder);
    written.clear();
    for (EntryDoc doc : entries) {
      Path file = entryFolder.resolve(fileName(doc.id));
      Files.writeString(file, GSON.toJson(doc), StandardCharsets.UTF_8);
      written.add(file);
    }
    for (Path file : list(entryFolder)) {
      if (!written.contains(file)) {
        Files.deleteIfExists(file);
      }
    }

    Path upgradeFolder = root.resolve("upgrades");
    Files.createDirectories(upgradeFolder);
    written.clear();
    for (UpgradeDoc doc : upgrades) {
      Path file = upgradeFolder.resolve(fileName(doc.id));
      Files.writeString(file, GSON.toJson(doc), StandardCharsets.UTF_8);
      written.add(file);
    }
    for (Path file : list(upgradeFolder)) {
      if (!written.contains(file)) {
        Files.deleteIfExists(file);
      }
    }
  }

  private static String fileName(String id) {
    return id.replace(':', '@').replace('/', '#') + ".json";
  }

  /* Lookup */

  public MaterialDoc findMaterial(String id) {
    for (MaterialDoc doc : materials) {
      if (doc.id.equals(id)) {
        return doc;
      }
    }
    return null;
  }

  public EntryDoc findEntry(String id) {
    for (EntryDoc doc : entries) {
      if (doc.id.equals(id)) {
        return doc;
      }
    }
    return null;
  }

  public UpgradeDoc findUpgrade(String id) {
    for (UpgradeDoc doc : upgrades) {
      if (doc.id.equals(id)) {
        return doc;
      }
    }
    return null;
  }

  public String freeUpgradeId(String namespace, String path) {
    String base = sanitizePath(path);
    if (base.isEmpty()) base = "upgrade";
    String id = namespace + ":" + base;
    int i = 2;
    while (findUpgrade(id) != null) {
      id = namespace + ":" + base + "_" + i++;
    }
    return id;
  }

  /** Finds a free ID in the given namespace starting from the given path */
  public String freeMaterialId(String namespace, String path) {
    String base = sanitizePath(path);
    if (base.isEmpty()) base = "material";
    String id = namespace + ":" + base;
    int i = 2;
    while (findMaterial(id) != null) {
      id = namespace + ":" + base + "_" + i++;
    }
    return id;
  }

  public String freeEntryId(String namespace, String path) {
    String base = sanitizePath(path);
    if (base.isEmpty()) base = "entry";
    String id = namespace + ":" + base;
    int i = 2;
    while (findEntry(id) != null) {
      id = namespace + ":" + base + "_" + i++;
    }
    return id;
  }

  /* Validation */

  public static boolean isValidNamespace(String namespace) {
    return namespace != null && !namespace.isEmpty() && namespace.chars().allMatch(c -> c == '_' || c == '-' || c == '.' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'));
  }

  public static boolean isValidPath(String path) {
    return path != null && !path.isEmpty() && path.chars().allMatch(c -> c == '_' || c == '-' || c == '.' || c == '/' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))
      && !path.startsWith("/") && !path.endsWith("/") && !path.contains("//") && !path.contains("..");
  }

  public static boolean isValidId(String id) {
    int i = id.indexOf(':');
    return i > 0 && isValidNamespace(id.substring(0, i)) && isValidPath(id.substring(i + 1));
  }

  /** Converts free text into a valid path */
  public static String sanitizePath(String text) {
    StringBuilder builder = new StringBuilder();
    for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
      if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
        builder.append(c);
      } else if (c == ' ' || c == '/') {
        builder.append('_');
      }
    }
    return builder.toString();
  }
}
