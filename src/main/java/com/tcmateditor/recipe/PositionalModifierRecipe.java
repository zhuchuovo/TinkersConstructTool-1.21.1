package com.tcmateditor.recipe;

import com.tcmateditor.model.AnvilSlots;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.ITinkerableContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.IMutableTinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.modifiers.adding.ModifierRecipe;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Tinkers' modifier recipe where every input must be placed in a specific station slot.
 * {@code positions[i]} is the station input index for {@code inputs[i]}.
 * Generated packs pair this recipe with a plain {@code tconstruct:modifier} fallback for when this mod is missing.
 */
public class PositionalModifierRecipe extends ModifierRecipe {
  public static final RecordLoadable<PositionalModifierRecipe> LOADER = RecordLoadable.create(
    ContextKey.ID.requiredField(), INPUTS_FIELD,
    IntLoadable.range(0, 15).list(1).requiredField("positions", r -> r.positions),
    TOOLS_FIELD, MAX_TOOL_SIZE_FIELD, RESULT_FIELD, LEVEL_FIELD, SLOTS_FIELD, ALLOW_CRYSTAL_FIELD, CHECK_TRAIT_LEVEL_FIELD,
    PositionalModifierRecipe::new);

  private final List<Integer> positions;

  public PositionalModifierRecipe(ResourceLocation id, List<SizedIngredient> inputs, List<Integer> positions, Ingredient toolRequirement, int maxToolSize,
                                  ModifierId result, IntRange level, @Nullable SlotCount slots, boolean allowCrystal, boolean checkTraitLevel) {
    super(id, inputs, toolRequirement, maxToolSize, result, level, slots, allowCrystal, checkTraitLevel);
    if (positions.size() != inputs.size()) {
      throw new IllegalArgumentException("Recipe " + id + " needs one position per input, got " + positions.size() + " for " + inputs.size() + " inputs");
    }
    this.positions = positions;
  }

  /** Checks every input is in its own slot and every other slot is empty */
  private boolean matchesPositions(ITinkerableContainer inv) {
    int count = inv.getInputCount();
    boolean[] used = new boolean[count];
    for (int i = 0; i < inputs.size(); i++) {
      int position = positions.get(i);
      if (position >= count || !inputs.get(i).test(inv.getInput(position))) {
        return false;
      }
      used[position] = true;
    }
    for (int i = 0; i < count; i++) {
      if (!used[i] && !inv.getInput(i).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean matches(ITinkerStationContainer inv, Level level) {
    if (!result.isBound() || !toolRequirement.test(inv.getTinkerableStack())) {
      return false;
    }
    return matchesCrystal(inv) || matchesPositions(inv);
  }

  @Override
  public void updateInputs(LazyToolStack result, IMutableTinkerStationContainer inv, boolean isServer) {
    if (matchesCrystal(inv)) {
      super.updateInputs(result, inv, isServer);
      return;
    }
    for (int i = 0; i < inputs.size(); i++) {
      inv.shrinkInput(positions.get(i), inputs.get(i).getAmountNeeded());
    }
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return EditorRecipes.POSITIONAL_MODIFIER.get();
  }

  /* JEI: show each input where it has to go */

  @Override
  public int getInputCount() {
    return 5;
  }

  @Override
  public List<ItemStack> getDisplayItems(int displaySlot) {
    for (int i = 0; i < inputs.size(); i++) {
      if (AnvilSlots.displaySlot(positions.get(i)) == displaySlot) {
        return inputs.get(i).getMatchingStacks();
      }
    }
    return Collections.emptyList();
  }
}
