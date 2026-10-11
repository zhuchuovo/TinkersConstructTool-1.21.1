package com.tcmateditor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * An upgrade ("强化"): a Tinker Station / Tinkers' Anvil recipe that applies an entry (or any modifier) to equipment.
 * Inputs are stored per anvil slot, numbered 1-5 clockwise starting from the lapis slot.
 */
public class UpgradeDoc {
  public static final int SLOTS = 5;

  /** Recipe ID */
  public String id = "";
  /** Modifier (usually a custom entry) the upgrade applies */
  public String modifier = "";
  /** upgrades, abilities, defense, souls or none */
  public String slotType = "upgrades";
  public int slotCount = 1;
  public int maxLevel = 1;
  public boolean allowCrystal = true;
  /** Require each material in its own slot (needs this mod, falls back to any order without it) */
  public boolean strictSlots = true;
  /** Five anvil slots, index 0 = slot 1 */
  public List<SlotInput> inputs = new ArrayList<>();
  /** Item IDs or #tags that may receive the upgrade */
  public List<String> allowTools = new ArrayList<>(List.of("#tconstruct:modifiable"));
  /** Item IDs or #tags that may not receive the upgrade */
  public List<String> denyTools = new ArrayList<>();

  public static class SlotInput {
    /** Item ID or #tag, empty for an unused slot */
    public String ingredient = "";
    public int count = 1;

    public boolean isEmpty() {
      return ingredient == null || ingredient.isBlank();
    }

    public SlotInput copy() {
      SlotInput input = new SlotInput();
      input.ingredient = ingredient;
      input.count = count;
      return input;
    }
  }

  public UpgradeDoc() {
    ensureSlots();
  }

  public SlotInput slot(int index) {
    ensureSlots();
    return inputs.get(index);
  }

  private void ensureSlots() {
    if (inputs == null) {
      inputs = new ArrayList<>();
    }
    while (inputs.size() < SLOTS) {
      inputs.add(new SlotInput());
    }
    while (inputs.size() > SLOTS) {
      inputs.remove(inputs.size() - 1);
    }
  }

  public boolean hasInputs() {
    return inputs.stream().anyMatch(i -> !i.isEmpty());
  }

  public String namespace() {
    int i = id.indexOf(':');
    return i < 0 ? "minecraft" : id.substring(0, i);
  }

  public String path() {
    int i = id.indexOf(':');
    return i < 0 ? id : id.substring(i + 1);
  }

  public UpgradeDoc copy() {
    UpgradeDoc doc = new UpgradeDoc();
    doc.id = id;
    doc.modifier = modifier;
    doc.slotType = slotType;
    doc.slotCount = slotCount;
    doc.maxLevel = maxLevel;
    doc.allowCrystal = allowCrystal;
    doc.strictSlots = strictSlots;
    doc.inputs = new ArrayList<>(inputs.stream().map(SlotInput::copy).toList());
    doc.allowTools = new ArrayList<>(allowTools);
    doc.denyTools = new ArrayList<>(denyTools);
    return doc;
  }

  public void validate() {
    if (id == null) id = "";
    if (modifier == null) modifier = "";
    if (slotType == null) slotType = "upgrades";
    if (allowTools == null) allowTools = new ArrayList<>();
    if (denyTools == null) denyTools = new ArrayList<>();
    allowTools.removeIf(s -> s == null || s.isBlank());
    denyTools.removeIf(s -> s == null || s.isBlank());
    if (inputs != null) {
      inputs.replaceAll(i -> i == null ? new SlotInput() : i);
    }
    ensureSlots();
    for (SlotInput input : inputs) {
      if (input.ingredient == null) input.ingredient = "";
      input.count = Math.max(1, Math.min(64, input.count));
    }
    slotCount = Math.max(0, slotCount);
    maxLevel = Math.max(1, maxLevel);
  }
}
