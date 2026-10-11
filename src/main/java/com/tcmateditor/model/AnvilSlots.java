package com.tcmateditor.model;

/**
 * Slot numbering of the Tinkers' Anvil used by the editor.
 * Slots are numbered 1-5 clockwise starting at the top (lapis) slot; Tinkers' stores them in a different order.
 */
public final class AnvilSlots {
  private AnvilSlots() {}

  /** Editor slot (0 = slot 1) to the Tinkers' station input index (layout order quartz, dust, lapis, ingot, gem) */
  public static final int[] INPUT_INDEX = { 2, 3, 4, 0, 1 };
  /** Slot icon names, see {@code tconstruct:textures/gui/tinker_pattern} */
  public static final String[] ICONS = { "lapis", "ingot", "gem", "quartz", "dust" };
  /** Position of each editor slot inside the Tinkers' Anvil layout (16px slots) */
  public static final int[][] POSITION = { { 33, 19 }, { 55, 37 }, { 51, 62 }, { 15, 62 }, { 11, 37 } };
  /** Tool slot position inside the layout */
  public static final int[] TOOL_POSITION = { 33, 41 };
  /** Number of inputs of the plain Tinker Station; its inputs are the first three input indices */
  public static final int STATION_INPUTS = 3;

  /** Station input index to the JEI display slot of Tinkers' modifier category (left, top, right, bottom right, bottom left) */
  public static int displaySlot(int inputIndex) {
    return switch (inputIndex) {
      case 1 -> 0;
      case 2 -> 1;
      case 3 -> 2;
      case 4 -> 3;
      default -> 4;
    };
  }

  /** True if the editor slot exists on the plain Tinker Station */
  public static boolean onStation(int slot) {
    return INPUT_INDEX[slot] < STATION_INPUTS;
  }
}
