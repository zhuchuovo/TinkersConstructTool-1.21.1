package com.tcmateditor.model;

import java.util.List;
import java.util.Map;

/** Languages supported for editing material and entry texts */
public final class Languages {
  public static final String EN = "en_us";
  public static final List<String> CODES = List.of("zh_cn", "zh_tw", "en_us", "ja_jp");
  public static final Map<String,String> LABELS = Map.of(
    "zh_cn", "简体中文",
    "zh_tw", "繁體中文",
    "en_us", "English",
    "ja_jp", "日本語");

  private Languages() {}

  /** Picks the best language to show from the given game language */
  public static String bestFor(String gameLanguage) {
    if (CODES.contains(gameLanguage)) {
      return gameLanguage;
    }
    if (gameLanguage.startsWith("zh_")) {
      return gameLanguage.equals("zh_hk") ? "zh_tw" : "zh_cn";
    }
    return EN;
  }

  /** Fallback order used when a translation is missing */
  public static List<String> fallbackOrder(String code) {
    return switch (code) {
      case "zh_tw" -> List.of("zh_tw", "zh_cn", "en_us", "ja_jp");
      case "zh_cn" -> List.of("zh_cn", "zh_tw", "en_us", "ja_jp");
      case "ja_jp" -> List.of("ja_jp", "en_us", "zh_cn", "zh_tw");
      default -> List.of("en_us", "zh_cn", "zh_tw", "ja_jp");
    };
  }

  /** Resolves a single field with fallbacks */
  public static String resolve(Map<String,LangText> map, String code, java.util.function.Function<LangText,String> getter) {
    for (String c : fallbackOrder(code)) {
      LangText text = map.get(c);
      if (text != null) {
        String value = getter.apply(text);
        if (value != null && !value.isBlank()) {
          return value;
        }
      }
    }
    return "";
  }
}
