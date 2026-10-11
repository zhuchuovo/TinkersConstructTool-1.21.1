package com.tcmateditor.net;

import com.tcmateditor.EditorPaths;
import com.tcmateditor.TCMatEditor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerTools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server side of applying: writes data files into the live pack and reloads data packs */
public final class ServerApply {
  private ServerApply() {}

  /** Chunks being received per player */
  private static final Map<UUID,Assembly> ASSEMBLIES = new HashMap<>();
  /** Request waiting for the current reload to finish */
  private static Request queued;
  private static boolean reloading = false;

  private record Assembly(int session, byte[][] parts) {}
  private record Request(ServerPlayer player, int session, Map<String,String> files) {}

  /** Singleplayer owner or an operator */
  public static boolean canEdit(ServerPlayer player) {
    MinecraftServer server = player.server;
    return server.isSingleplayerOwner(player.getGameProfile()) || player.hasPermissions(2);
  }

  /* Apply */

  public static void handleData(ApplyDataPayload payload, ServerPlayer player) {
    if (!canEdit(player)) {
      EditorNetwork.sendToPlayer(player, new ApplyResultPayload(payload.session(), false, "tcmateditor.status.no_permission", "", 0));
      return;
    }
    int total = payload.total();
    int index = payload.index();
    if (total <= 0 || total > 512 || index < 0 || index >= total) {
      return;
    }
    UUID uuid = player.getUUID();
    Assembly assembly = ASSEMBLIES.get(uuid);
    if (assembly == null || assembly.session != payload.session() || assembly.parts.length != total) {
      assembly = new Assembly(payload.session(), new byte[total][]);
      ASSEMBLIES.put(uuid, assembly);
    }
    assembly.parts[index] = payload.data();
    for (byte[] part : assembly.parts) {
      if (part == null) {
        return;
      }
    }
    ASSEMBLIES.remove(uuid);

    Map<String,String> files;
    try {
      ByteArrayOutputStream joined = new ByteArrayOutputStream();
      for (byte[] part : assembly.parts) {
        joined.write(part);
      }
      files = FilePacking.unpack(joined.toByteArray());
    } catch (IOException | RuntimeException e) {
      TCMatEditor.LOG.error("Received invalid editor data from {}", player.getName().getString(), e);
      EditorNetwork.sendToPlayer(player, new ApplyResultPayload(payload.session(), false, "tcmateditor.status.error", String.valueOf(e.getMessage()), 0));
      return;
    }

    Request request = new Request(player, payload.session(), files);
    if (reloading) {
      // only the latest state matters
      if (queued != null && queued.player != player) {
        sendResult(queued, false, "tcmateditor.status.superseded", "", 0);
      }
      queued = request;
    } else {
      apply(request);
    }
  }

  private static void apply(Request request) {
    MinecraftServer server = request.player.server;
    try {
      writeData(request.files);
    } catch (IOException e) {
      TCMatEditor.LOG.error("Failed to write editor data", e);
      sendResult(request, false, "tcmateditor.status.error", String.valueOf(e.getMessage()), 0);
      return;
    }
    reloading = true;
    long start = System.nanoTime();
    PackRepository repository = server.getPackRepository();
    repository.reload();
    Collection<String> selected = new ArrayList<>(repository.getSelectedIds());
    server.reloadResources(selected).whenComplete((result, error) -> server.execute(() -> {
      reloading = false;
      int millis = (int) ((System.nanoTime() - start) / 1_000_000);
      if (error != null) {
        TCMatEditor.LOG.error("Data pack reload failed", error);
        sendResult(request, false, "tcmateditor.status.reload_failed", String.valueOf(error.getMessage()), millis);
      } else {
        refreshTools(server);
        sendResult(request, true, "tcmateditor.status.applied", Integer.toString(request.files.size()), millis);
      }
      Request next = queued;
      queued = null;
      if (next != null) {
        apply(next);
      }
    }));
  }

  private static void sendResult(Request request, boolean success, String key, String detail, int millis) {
    EditorNetwork.sendToPlayer(request.player, new ApplyResultPayload(request.session, success, key, detail, millis));
  }

  /** Replaces the data folder of the live pack */
  private static void writeData(Map<String,String> files) throws IOException {
    Path pack = EditorPaths.livePack();
    Path data = pack.resolve("data");
    EditorPaths.deleteTree(data);
    Files.createDirectories(data);
    for (Map.Entry<String,String> entry : files.entrySet()) {
      String path = entry.getKey();
      if (!path.startsWith("data/") || !path.endsWith(".json")) {
        throw new IOException("Refusing to write " + path);
      }
      Path target = EditorPaths.safeResolve(pack, path);
      Files.createDirectories(target.getParent());
      Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
    }
  }

  /** Rebuilds tool stats in player inventories so stat changes show up immediately */
  private static void refreshTools(MinecraftServer server) {
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      Inventory inventory = player.getInventory();
      for (int i = 0; i < inventory.getContainerSize(); i++) {
        ItemStack stack = inventory.getItem(i);
        if (!stack.isEmpty() && stack.getItem() instanceof IModifiable) {
          try {
            ToolStack tool = ToolStack.copyFrom(stack);
            tool.rebuildStats();
            tool.updateStack(stack);
          } catch (RuntimeException e) {
            TCMatEditor.LOG.debug("Unable to refresh tool {}", stack, e);
          }
        }
      }
      player.inventoryMenu.broadcastChanges();
    }
  }

  /* Give test tools */

  public static void handleGive(GiveTestPayload payload, ServerPlayer player) {
    if (!player.isCreative() && !canEdit(player)) {
      return;
    }
    MaterialId id = MaterialId.tryParse(payload.material());
    if (id == null) {
      return;
    }
    IMaterial material = MaterialRegistry.getMaterial(id);
    if (material == IMaterial.UNKNOWN) {
      EditorNetwork.sendToPlayer(player, new ApplyResultPayload(-1, false, "tcmateditor.status.unknown_material", payload.material(), 0));
      return;
    }
    MaterialVariant variant = MaterialVariant.of(material);
    List<IModifiable> tools = List.of(TinkerTools.pickaxe.get(), TinkerTools.sword.get(), TinkerTools.longbow.get(), TinkerTools.plateArmor.get(ArmorItem.Type.CHESTPLATE));
    int given = 0;
    for (IModifiable tool : tools) {
      ItemStack stack = ToolBuildHandler.createSingleMaterial(tool, variant);
      if (!stack.isEmpty()) {
        given++;
        if (!player.getInventory().add(stack)) {
          player.drop(stack, false);
        }
      }
    }
    EditorNetwork.sendToPlayer(player, new ApplyResultPayload(-1, given > 0, given > 0 ? "tcmateditor.status.given" : "tcmateditor.status.nothing_given", Integer.toString(given), 0));
  }
}
