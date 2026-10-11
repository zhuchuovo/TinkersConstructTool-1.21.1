package com.tcmateditor.client;

import com.tcmateditor.EditorPaths;
import com.tcmateditor.TCMatEditor;
import com.tcmateditor.client.gen.AssetGenerator;
import com.tcmateditor.client.gui.Dialogs;
import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.gen.DataGenerator;
import com.tcmateditor.gen.DataGenerator.Problem;
import com.tcmateditor.model.Workspace;
import com.tcmateditor.net.ApplyDataPayload;
import com.tcmateditor.net.ApplyResultPayload;
import com.tcmateditor.net.FilePacking;
import com.tcmateditor.net.GiveTestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client side of applying changes.
 * Data files go to the server which reloads data packs, assets are written locally and trigger a resource reload when they changed.
 */
public final class ClientApply {
  private ClientApply() {}

  /** Delay after the last edit before auto apply */
  private static final long AUTO_DELAY = 1200;
  private static final String SIGNATURE_FILE = ".assets_signature";

  private static int session = 0;
  private static boolean waiting = false;
  private static boolean resourceReloadPending = false;
  private static boolean dataDirty = false;
  private static long lastEdit = 0;
  private static String lastSentData = null;
  private static String appliedAssets = null;
  /** Content left out of the last apply because the game would reject it */
  private static List<Problem> lastProblems = List.of();

  /** Call after any edit */
  public static void markEdited() {
    dataDirty = true;
    lastEdit = System.currentTimeMillis();
  }

  public static boolean isWaiting() {
    return waiting;
  }

  /** True if there are edits that were not applied yet */
  public static boolean hasUnappliedEdits() {
    return dataDirty;
  }

  /** True if visual changes are waiting for a full apply */
  public static boolean assetsDirty() {
    return !AssetGenerator.signature(EditorSession.workspace()).equals(appliedAssetSignature());
  }

  private static String appliedAssetSignature() {
    if (appliedAssets == null) {
      try {
        Path file = EditorPaths.livePack().resolve(SIGNATURE_FILE);
        appliedAssets = Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
      } catch (IOException e) {
        appliedAssets = "";
      }
    }
    return appliedAssets;
  }

  public static void reset() {
    waiting = false;
    lastSentData = null;
  }

  /** Client tick: handles auto apply */
  public static void tick() {
    if (dataDirty && EditorSession.autoApply && !waiting && System.currentTimeMillis() - lastEdit > AUTO_DELAY && canSendToServer()) {
      apply(false);
    }
  }

  public static boolean canSendToServer() {
    ClientPacketListener connection = Minecraft.getInstance().getConnection();
    return connection != null && connection.hasChannel(ApplyDataPayload.TYPE);
  }

  /** Saves the workspace without reloading anything */
  public static void save() {
    try {
      EditorSession.workspace().save();
      EditorSession.setStatus(Component.translatable("tcmateditor.status.saved_only"), Theme.SUCCESS);
    } catch (IOException e) {
      TCMatEditor.LOG.error("Failed to save workspace", e);
      EditorSession.setStatus(Component.translatable("tcmateditor.status.error", e.getMessage()), Theme.DANGER);
    }
  }

  /**
   * Saves and applies the workspace
   * @param withAssets  If true, also regenerates textures/texts and reloads resources when they changed
   */
  public static void apply(boolean withAssets) {
    Workspace workspace = EditorSession.workspace();
    dataDirty = false;
    try {
      workspace.save();
    } catch (IOException e) {
      TCMatEditor.LOG.error("Failed to save workspace", e);
      EditorSession.setStatus(Component.translatable("tcmateditor.status.error", e.getMessage()), Theme.DANGER);
      return;
    }

    if (withAssets) {
      String signature = AssetGenerator.signature(workspace);
      if (!signature.equals(appliedAssetSignature())) {
        try {
          writeAssets(AssetGenerator.generate(workspace));
          Files.writeString(EditorPaths.livePack().resolve(SIGNATURE_FILE), signature, StandardCharsets.UTF_8);
          appliedAssets = signature;
          resourceReloadPending = true;
        } catch (IOException | RuntimeException e) {
          TCMatEditor.LOG.error("Failed to write assets", e);
          EditorSession.setStatus(Component.translatable("tcmateditor.status.error", e.getMessage()), Theme.DANGER);
          return;
        }
      }
    }

    List<Problem> problems = new ArrayList<>();
    Map<String,String> data = DataGenerator.generate(workspace, problems);
    lastProblems = problems;
    String dataText = data.toString();
    boolean sendData = withAssets || !dataText.equals(lastSentData);
    if (sendData && canSendToServer()) {
      try {
        byte[] packed = FilePacking.pack(data);
        List<byte[]> chunks = FilePacking.split(packed, ApplyDataPayload.CHUNK_SIZE);
        session++;
        for (int i = 0; i < chunks.size(); i++) {
          PacketDistributor.sendToServer(new ApplyDataPayload(session, i, chunks.size(), chunks.get(i)));
        }
        waiting = true;
        lastSentData = dataText;
        EditorSession.setStatus(Component.translatable("tcmateditor.status.applying"), Theme.TEXT_DIM);
      } catch (IOException e) {
        EditorSession.setStatus(Component.translatable("tcmateditor.status.error", e.getMessage()), Theme.DANGER);
      }
    } else if (sendData) {
      EditorSession.setStatus(Component.translatable(Minecraft.getInstance().getConnection() == null ? "tcmateditor.status.saved" : "tcmateditor.status.no_server"), Theme.WARNING);
    } else if (!resourceReloadPending) {
      EditorSession.setStatus(Component.translatable("tcmateditor.status.up_to_date"), Theme.TEXT_DIM);
    }

    if (!waiting && resourceReloadPending) {
      reloadResources();
    }
  }

  /** Server reported the result of a data apply */
  public static void onResult(ApplyResultPayload payload) {
    if (payload.session() == -1) {
      // give test tools result
      EditorSession.setStatus(Component.translatable(payload.messageKey(), payload.detail()), payload.success() ? Theme.SUCCESS : Theme.WARNING);
      return;
    }
    if (payload.session() != session) {
      return;
    }
    waiting = false;
    if (payload.success() && !lastProblems.isEmpty()) {
      EditorSession.setStatus(Component.translatable("tcmateditor.status.applied_with_problems", payload.detail(), lastProblems.size(), Dialogs.problemText(lastProblems.get(0))), Theme.WARNING);
    } else if (payload.success()) {
      EditorSession.setStatus(Component.translatable(payload.messageKey(), payload.detail(), String.format("%.1f", payload.millis() / 1000f)), Theme.SUCCESS);
    } else {
      lastSentData = null;
      EditorSession.setStatus(Component.translatable(payload.messageKey(), payload.detail()), Theme.DANGER);
    }
    if (resourceReloadPending) {
      reloadResources();
    }
  }

  private static void reloadResources() {
    resourceReloadPending = false;
    long start = System.nanoTime();
    EditorSession.setStatus(Component.translatable("tcmateditor.status.reloading_resources"), Theme.TEXT_DIM);
    Minecraft.getInstance().reloadResourcePacks().thenRun(() -> Minecraft.getInstance().execute(() ->
      EditorSession.setStatus(Component.translatable("tcmateditor.status.resources_done", String.format("%.1f", (System.nanoTime() - start) / 1e9)), Theme.SUCCESS)));
  }

  /** Replaces the assets folder of the live pack */
  private static void writeAssets(Map<String,byte[]> files) throws IOException {
    Path pack = EditorPaths.livePack();
    EditorPaths.deleteTree(pack.resolve("assets"));
    for (Map.Entry<String,byte[]> entry : files.entrySet()) {
      Path target = EditorPaths.safeResolve(pack, entry.getKey());
      Files.createDirectories(target.getParent());
      Files.write(target, entry.getValue());
    }
  }

  /** Asks the server for test tools */
  public static void giveTestTools(String materialId) {
    if (canSendToServer()) {
      PacketDistributor.sendToServer(new GiveTestPayload(materialId));
    } else {
      EditorSession.setStatus(Component.translatable("tcmateditor.status.no_server"), Theme.WARNING);
    }
  }
}
