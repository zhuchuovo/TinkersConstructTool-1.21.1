package com.tcmateditor.client.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;

import java.util.List;

/** Item stacks to show for item and tag references, shared by pages and widgets */
public final class Icons {
  private Icons() {}

  /** Icon for an item ID or #tag, tags cycle through their items once a second */
  public static ItemStack forRef(String ref) {
    if (ref == null || ref.isBlank()) {
      return ItemStack.EMPTY;
    }
    if (ref.startsWith("#")) {
      List<ItemStack> tagged = Pickers.tagItems(ref.substring(1));
      if (tagged.isEmpty()) {
        return ItemStack.EMPTY;
      }
      return tagged.get((int) ((System.currentTimeMillis() / 1000) % tagged.size()));
    }
    ResourceLocation id = ResourceLocation.tryParse(ref);
    if (id == null) {
      return ItemStack.EMPTY;
    }
    Item item = BuiltInRegistries.ITEM.get(id);
    // Tinkers' tools need materials to render, use their display version
    return item == Items.AIR ? ItemStack.EMPTY : IModifiableDisplay.getDisplayStack(item);
  }
}
