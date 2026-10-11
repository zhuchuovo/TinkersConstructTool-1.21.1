package com.tcmateditor.recipe;

import com.tcmateditor.TCMatEditor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;

/** Recipe types added by the editor. Only used by upgrades that require exact anvil slots. */
public final class EditorRecipes {
  private EditorRecipes() {}

  private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TCMatEditor.MOD_ID);

  public static final RegistryObject<RecipeSerializer<PositionalModifierRecipe>> POSITIONAL_MODIFIER =
    SERIALIZERS.register("positional_modifier", () -> LoadableRecipeSerializer.of(PositionalModifierRecipe.LOADER));

  public static void register(IEventBus modBus) {
    SERIALIZERS.register(modBus);
  }
}
