package com.tcmateditor.client.gui;

import com.tcmateditor.client.EditorSession;
import com.tcmateditor.model.EntryDoc;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;

/** Display names for IDs shown in the editor */
public final class Names {
  private Names() {}

  public static MutableComponent tr(String key, Object... args) {
    return Component.translatable("tcmateditor." + key, args);
  }

  private static ResourceLocation rl(String id) {
    ResourceLocation location = ResourceLocation.tryParse(id);
    return location == null ? new ResourceLocation("unknown") : location;
  }

  /** Material stat type name, e.g. Head */
  public static Component statType(String id) {
    ResourceLocation location = rl(id);
    return Component.translatableWithFallback("stat." + location.getNamespace() + "." + location.getPath(), location.getPath());
  }

  /** Material stat field name */
  public static Component field(String key) {
    return Component.translatableWithFallback("tcmateditor.field." + key, key);
  }

  /** Stat type group name */
  public static Component group(String group) {
    return Component.translatableWithFallback("tcmateditor.group." + group, group);
  }

  /** Trait group (melee_harvest etc.) or stat type name */
  public static Component traitGroup(String id) {
    ResourceLocation location = rl(id);
    String key = "tcmateditor.trait_group." + location.getPath();
    if ("tconstruct".equals(location.getNamespace()) && net.minecraft.locale.Language.getInstance().has(key)) {
      return Component.translatable(key);
    }
    return statType(id);
  }

  /** Tool stat name (durability, attack damage...) */
  public static Component toolStat(String id) {
    ResourceLocation location = rl(id);
    String text = Component.translatableWithFallback("tool_stat." + location.getNamespace() + "." + location.getPath(), location.getPath()).getString();
    // Tinkers' stat names are tooltip prefixes ending with a colon
    text = text.replaceAll("[\\s:：]+$", "");
    return Component.literal(text);
  }

  public static Component attribute(String id) {
    Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(rl(id));
    return attribute != null ? Component.translatable(attribute.getDescriptionId()) : Component.literal(id);
  }

  public static Component harvestTier(String id) {
    ResourceLocation location = rl(id);
    return Component.translatableWithFallback("stat.tconstruct.harvest_tier." + location.getNamespace() + "." + location.getPath(), location.getPath());
  }

  /** Mob effect name, or the ID if it does not exist */
  public static Component effect(String id) {
    ResourceLocation location = ResourceLocation.tryParse(id == null ? "" : id);
    MobEffect effect = location == null ? null : BuiltInRegistries.MOB_EFFECT.get(location);
    return effect != null ? Component.translatable(effect.getDescriptionId()) : Component.literal(String.valueOf(id));
  }

  /** Mob effect particle color */
  public static int effectColor(String id) {
    ResourceLocation location = ResourceLocation.tryParse(id == null ? "" : id);
    MobEffect effect = location == null ? null : BuiltInRegistries.MOB_EFFECT.get(location);
    return effect != null ? effect.getColor() | 0xFF000000 : Theme.TEXT_DIM;
  }

  /** Modifier name, custom entries first */
  public static Component modifier(String id) {
    EntryDoc entry = EditorSession.workspace().findEntry(id);
    if (entry != null) {
      return Component.literal(entry.displayName(EditorSession.uiLanguage()));
    }
    ModifierId modifierId = ModifierId.tryParse(id);
    if (modifierId != null && ModifierManager.INSTANCE.contains(modifierId)) {
      return modifierName(ModifierManager.getValue(modifierId));
    }
    return Component.literal(id);
  }

  /** Modifier display name, falling back to the ID for internal modifiers without a translation */
  public static Component modifierName(Modifier modifier) {
    if (!net.minecraft.locale.Language.getInstance().has(modifier.getTranslationKey())) {
      return Component.literal(modifier.getId().toString());
    }
    return modifier.getDisplayName();
  }

  public static int modifierColor(String id) {
    EntryDoc entry = EditorSession.workspace().findEntry(id);
    if (entry != null) {
      return entry.colorInt();
    }
    ModifierId modifierId = ModifierId.tryParse(id);
    if (modifierId != null && ModifierManager.INSTANCE.contains(modifierId)) {
      Modifier modifier = ModifierManager.getValue(modifierId);
      return modifier.getColor() | 0xFF000000;
    }
    return Theme.TEXT_DIM;
  }
}
