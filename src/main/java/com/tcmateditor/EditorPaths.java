package com.tcmateditor;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** File locations used by the editor, all relative to the game directory */
public final class EditorPaths {
  private EditorPaths() {}

  /** Root folder: {@code <game>/tcmateditor} */
  public static Path root() {
    return FMLPaths.GAMEDIR.get().resolve(TCMatEditor.MOD_ID);
  }

  /** Live pack injected as both data pack and resource pack while the mod is installed */
  public static Path livePack() {
    return root().resolve("live_pack");
  }

  /** Editor project files (the source of truth for the editor) */
  public static Path workspace() {
    return root().resolve("workspace");
  }

  /** Exported standalone packs */
  public static Path exports() {
    return root().resolve("exports");
  }

  /** User supplied pattern textures for the texture editor */
  public static Path patterns() {
    return root().resolve("patterns");
  }

  /** Deletes a folder recursively, ignoring if missing */
  public static void deleteTree(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    Files.walkFileTree(path, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Resolves a pack relative path safely below the given root.
   * @throws IOException if the path escapes the root
   */
  public static Path safeResolve(Path root, String relative) throws IOException {
    Path normalizedRoot = root.toAbsolutePath().normalize();
    Path resolved = normalizedRoot.resolve(relative).normalize();
    if (!resolved.startsWith(normalizedRoot) || relative.contains("..")) {
      throw new IOException("Illegal path " + relative);
    }
    return resolved;
  }
}
