package com.tcmateditor.model;

/** Localized texts for one language */
public class LangText {
  /** Display name */
  public String name = "";
  /** Short one line flavor text */
  public String flavor = "";
  /** Longer description (encyclopedia for materials, description for entries) */
  public String desc = "";

  public LangText() {}

  public LangText(String name, String flavor, String desc) {
    this.name = name == null ? "" : name;
    this.flavor = flavor == null ? "" : flavor;
    this.desc = desc == null ? "" : desc;
  }

  public boolean isEmpty() {
    return name.isBlank() && flavor.isBlank() && desc.isBlank();
  }

  public LangText copy() {
    return new LangText(name, flavor, desc);
  }
}
