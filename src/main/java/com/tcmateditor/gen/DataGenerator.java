package com.tcmateditor.gen;

import com.tcmateditor.model.EntryDoc;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.UpgradeDoc;
import com.tcmateditor.model.Workspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Converts editor documents into plain Tinkers' Construct data pack files.
 * Nothing generated here references this mod, so the output works on its own.
 * @see MaterialGenerator
 * @see EntryGenerator
 * @see UpgradeGenerator
 */
public final class DataGenerator {
  private DataGenerator() {}

  /**
   * Something left out of the output because the game would reject it, such as an item that does not exist.
   * @param owner   ID of the material, entry or upgrade
   * @param key     Translation key suffix under {@code tcmateditor.problem.}
   * @param detail  The offending value
   */
  public record Problem(String owner, String key, String detail) {}

  /** Generates all data files, keyed by pack relative path */
  public static Map<String,String> generate(Workspace workspace) {
    return generate(workspace, new ArrayList<>());
  }

  /** Generates all data files, anything the game would fail to load is left out and added to the problems */
  public static Map<String,String> generate(Workspace workspace, List<Problem> problems) {
    Map<String,String> files = new TreeMap<>();
    for (MaterialDoc doc : workspace.materials) {
      if (Workspace.isValidId(doc.id)) {
        MaterialGenerator.generate(doc, workspace, files, problems);
      }
    }
    for (EntryDoc doc : workspace.entries) {
      if (Workspace.isValidId(doc.id)) {
        EntryGenerator.generate(doc, files, problems);
      }
    }
    for (UpgradeDoc doc : workspace.upgrades) {
      UpgradeGenerator.generate(doc, workspace, files, problems);
    }
    return files;
  }
}
