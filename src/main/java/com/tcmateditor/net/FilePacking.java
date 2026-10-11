package com.tcmateditor.net;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Packs a path to text map into compressed chunks */
public final class FilePacking {
  private static final Gson GSON = new Gson();
  /** Hard limit on decompressed data to protect servers */
  private static final int MAX_BYTES = 16 * 1024 * 1024;

  private FilePacking() {}

  public static byte[] pack(Map<String,String> files) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(bytes), StandardCharsets.UTF_8)) {
      GSON.toJson(files, writer);
    }
    return bytes.toByteArray();
  }

  public static Map<String,String> unpack(byte[] data) throws IOException {
    byte[] raw;
    try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(data))) {
      raw = input.readNBytes(MAX_BYTES + 1);
    }
    if (raw.length > MAX_BYTES) {
      throw new IOException("Data too large");
    }
    try (Reader reader = new InputStreamReader(new ByteArrayInputStream(raw), StandardCharsets.UTF_8)) {
      Map<String,String> map = GSON.fromJson(reader, new TypeToken<Map<String,String>>() {}.getType());
      return map == null ? new TreeMap<>() : map;
    }
  }

  public static List<byte[]> split(byte[] data, int size) {
    List<byte[]> chunks = new ArrayList<>();
    for (int i = 0; i < data.length; i += size) {
      chunks.add(Arrays.copyOfRange(data, i, Math.min(data.length, i + size)));
    }
    if (chunks.isEmpty()) {
      chunks.add(new byte[0]);
    }
    return chunks;
  }
}
