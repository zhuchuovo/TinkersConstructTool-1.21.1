package com.tcmateditor.client.gui;

import com.tcmateditor.TCMatEditor;
import com.tcmateditor.client.ClientApply;
import com.tcmateditor.client.ClientSettings;
import com.tcmateditor.client.EditorSession;
import com.tcmateditor.client.EditorSession.Mode;
import com.tcmateditor.client.Exporter;
import com.tcmateditor.client.MaterialImporter;
import com.tcmateditor.client.gui.pages.EntryEffectsPage;
import com.tcmateditor.client.gui.pages.EntryInfoPage;
import com.tcmateditor.client.gui.pages.GeneralPage;
import com.tcmateditor.client.gui.pages.InfoPage;
import com.tcmateditor.client.gui.pages.Page;
import com.tcmateditor.client.gui.pages.StatsPage;
import com.tcmateditor.client.gui.pages.TexturePage;
import com.tcmateditor.client.gui.pages.TraitsPage;
import com.tcmateditor.client.gui.pages.UpgradeRecipePage;
import com.tcmateditor.client.gui.pages.UpgradeToolsPage;
import com.tcmateditor.client.gui.ui.Button;
import com.tcmateditor.client.gui.ui.Label;
import com.tcmateditor.client.gui.ui.Popup;
import com.tcmateditor.client.gui.ui.ScrollView;
import com.tcmateditor.client.gui.ui.SpritePreview;
import com.tcmateditor.client.gui.ui.TextField;
import com.tcmateditor.client.gui.ui.Toggle;
import com.tcmateditor.client.gui.ui.UI;
import com.tcmateditor.client.gui.ui.UIElement;
import com.tcmateditor.client.gui.ui.UIGroup;
import com.tcmateditor.client.texture.Colors;
import com.tcmateditor.client.texture.PartTextureLibrary;
import com.tcmateditor.model.EntryDoc;
import com.tcmateditor.model.LangText;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.TextureDoc;
import com.tcmateditor.model.UpgradeDoc;
import com.tcmateditor.model.Workspace;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Main editor screen, opened with Shift+Z */
public class EditorScreen extends Screen {
  private static final int TOP_H = 22;
  private static final int STATUS_H = 13;
  private static final Random RANDOM = new Random();

  private final UIGroup root = new UIGroup();
  private ScrollView content;
  private ScrollView list;
  private String contentKey = "";
  private int winX, winY, winW, winH, sideW;
  private boolean assetsDirty;
  private long assetsCheckTime;

  public EditorScreen() {
    super(Component.translatable("tcmateditor.title"));
  }

  @Override
  protected void init() {
    ClientSettings.load();
    UI.reset();
    PartTextureLibrary.clear();
    PreviewCache.invalidate();
    EditorSession.workspace();
    rebuild();
  }

  @Override
  public boolean isPauseScreen() {
    // the integrated server has to keep running to apply data
    return false;
  }

  @Override
  public void removed() {
    try {
      EditorSession.workspace().save();
    } catch (IOException e) {
      TCMatEditor.LOG.error("Failed to save workspace", e);
    }
    SpritePreview.releaseAll();
    UI.reset();
  }

  /* Layout */

  private void rebuild() {
    if (content != null) {
      EditorSession.SCROLL.put(contentKey, content.getScroll());
    }
    if (list != null) {
      EditorSession.SCROLL.put("list:" + EditorSession.mode, list.getScroll());
    }
    root.clear();
    int margin = width > 560 ? 10 : 3;
    winX = margin;
    winY = margin;
    winW = width - margin * 2;
    winH = height - margin * 2;
    sideW = Math.max(100, Math.min(150, winW / 4));
    root.size(width, height);
    buildTopBar();
    buildSidebar();
    buildMain();
    root.layout(0, 0);
    if (list != null) {
      list.setScroll(EditorSession.SCROLL.getOrDefault("list:" + EditorSession.mode, 0d));
    }
    if (content != null) {
      content.setScroll(EditorSession.SCROLL.getOrDefault(contentKey, 0d));
    }
  }

  private void buildTopBar() {
    int x = winX + 8;
    Component titleText = Component.literal("◆ ").withColor(Theme.ACCENT).append(Component.translatable("tcmateditor.title").withColor(Theme.TEXT));
    root.add(new Label(titleText, Theme.TEXT)).at(x, winY + 7);
    x += font.width(titleText) + 12;
    for (Mode mode : Mode.values()) {
      Button tab = new Button(Names.tr("mode." + mode.name().toLowerCase(Locale.ROOT)), () -> {
        EditorSession.mode = mode;
        UI.requestRebuild();
      }).style(Button.Style.TAB).selected(() -> EditorSession.mode == mode);
      tab.h = 18;
      root.add(tab).at(x, winY + 2);
      x += tab.w + 2;
    }

    int right = winX + winW - 6;
    Button apply = new Button(() -> ClientApply.isWaiting() ? Names.tr("button.applying") : Names.tr("button.apply"), () -> ClientApply.apply(true))
      .style(Button.Style.PRIMARY);
    apply.w = Math.max(apply.w, 46);
    apply.tooltip(Names.tr("hint.apply"));
    right -= apply.w;
    root.add(apply).at(right, winY + 3);
    Button export = new Button(Names.tr("button.export"), this::export);
    export.tooltip(Names.tr("hint.export"));
    right -= export.w + 4;
    root.add(export).at(right, winY + 3);
    Button help = new Button(Component.literal("?"), Dialogs::help).size(16, 16);
    help.tooltip(Names.tr("hint.help"));
    right -= 20;
    root.add(help).at(right, winY + 3);
    Button theme = new Button(() -> Component.literal(Theme.isLight() ? "☾" : "☀"), () -> {
      ClientSettings.setLightTheme(!ClientSettings.isLightTheme());
      UI.requestRebuild();
    }).size(16, 16);
    theme.tooltip(() -> Names.tr(Theme.isLight() ? "hint.theme_dark" : "hint.theme_light"));
    right -= 20;
    root.add(theme).at(right, winY + 3);
    Toggle auto = new Toggle(Names.tr("label.auto_apply"), () -> EditorSession.autoApply, v -> EditorSession.autoApply = v);
    auto.labelColor = Theme.TEXT_DIM;
    auto.tooltip(Names.tr("hint.auto_apply"));
    right -= auto.w + 8;
    if (right > x + 10) {
      root.add(auto).at(right, winY + 5);
    }
  }

  private void buildSidebar() {
    int top = winY + TOP_H;
    int bottom = winY + winH - STATUS_H;
    Mode mode = EditorSession.mode;
    TextField search = root.add(new TextField(EditorSession.listSearch, sideW - 12));
    search.at(winX + 6, top + 6);
    search.hint(Names.tr("hint.search"));
    search.onChange(text -> {
      EditorSession.listSearch = text;
      fillList();
    });
    int y = top + 26;
    Button create = root.add(new Button(Names.tr("button.new"), this::createNew));
    create.at(winX + 6, y);
    if (mode == Mode.MATERIALS) {
      Button imp = root.add(new Button(Names.tr("button.import"), this::importMaterial));
      imp.tooltip(Names.tr("hint.import"));
      imp.at(winX + 6 + create.w + 4, y);
      int remaining = sideW - 12 - create.w - 4;
      imp.w = Math.max(imp.w, remaining);
    } else {
      create.w = sideW - 12;
    }
    list = root.add(new ScrollView());
    list.at(winX + 2, y + 22);
    list.size(sideW - 4, bottom - (y + 22) - 2);
    fillList();
  }

  private void fillList() {
    list.clear();
    String query = EditorSession.listSearch.toLowerCase(Locale.ROOT).trim();
    String lang = EditorSession.uiLanguage();
    int y = 0;
    Workspace workspace = EditorSession.workspace();
    switch (EditorSession.mode) {
      case MATERIALS -> {
        for (MaterialDoc doc : workspace.materials) {
          if (!matches(query, doc.displayName(lang), doc.id)) continue;
          list.add(new ListRow(() -> Component.literal(doc.displayName(lang)), () -> doc.id, doc::nameColor, () -> EditorSession.material == doc, () -> {
            EditorSession.material = doc;
            UI.requestRebuild();
          })).at(2, y).size(list.w - 8, 22);
          y += 23;
        }
      }
      case ENTRIES -> {
        for (EntryDoc doc : workspace.entries) {
          if (!matches(query, doc.displayName(lang), doc.id)) continue;
          list.add(new ListRow(() -> Component.literal(doc.displayName(lang)), () -> doc.id, doc::colorInt, () -> EditorSession.entry == doc, () -> {
            EditorSession.entry = doc;
            UI.requestRebuild();
          })).at(2, y).size(list.w - 8, 22);
          y += 23;
        }
      }
      case UPGRADES -> {
        for (UpgradeDoc doc : workspace.upgrades) {
          if (!matches(query, upgradeName(doc).getString(), doc.id)) continue;
          list.add(new ListRow(() -> upgradeName(doc), () -> doc.id, () -> upgradeColor(doc), () -> EditorSession.upgrade == doc, () -> {
            EditorSession.upgrade = doc;
            UI.requestRebuild();
          })).at(2, y).size(list.w - 8, 22);
          y += 23;
        }
      }
    }
    list.contentHeight = y;
    list.layoutChildren();
  }

  private static boolean matches(String query, String name, String id) {
    return query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query) || id.contains(query);
  }

  /** Upgrades are named after the modifier they apply */
  public static Component upgradeName(UpgradeDoc doc) {
    return doc.modifier.isBlank() ? Component.literal(doc.path()) : Names.modifier(doc.modifier);
  }

  public static int upgradeColor(UpgradeDoc doc) {
    return doc.modifier.isBlank() ? Theme.TEXT_DIM : Names.modifierColor(doc.modifier);
  }

  /** Sidebar entry */
  private static class ListRow extends UIElement {
    private final java.util.function.Supplier<Component> name;
    private final java.util.function.Supplier<String> id;
    private final java.util.function.IntSupplier color;
    private final java.util.function.BooleanSupplier selected;
    private final Runnable onClick;

    ListRow(java.util.function.Supplier<Component> name, java.util.function.Supplier<String> id, java.util.function.IntSupplier color,
            java.util.function.BooleanSupplier selected, Runnable onClick) {
      this.name = name;
      this.id = id;
      this.color = color;
      this.selected = selected;
      this.onClick = onClick;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
      boolean sel = selected.getAsBoolean();
      boolean hover = isHovered(mx, my);
      if (sel || hover) {
        Theme.box(g, x, y, w, h, sel ? Theme.ACCENT_DIM : Theme.CARD_HOVER);
      }
      if (sel) {
        g.fill(x, y + 3, x + 2, y + h - 3, Theme.ACCENT);
      }
      int c = color.getAsInt() | 0xFF000000;
      Theme.box(g, x + 6, y + 6, 10, 10, c);
      Theme.ellipsis(g, name.get(), x + 21, y + 2, w - 24, Theme.TEXT);
      Theme.ellipsis(g, Component.literal(id.get()), x + 21, y + 12, w - 24, Theme.TEXT_FAINT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
      onClick.run();
      return true;
    }
  }

  /* Mode helpers */

  private static List<String> tabsFor(Mode mode) {
    return switch (mode) {
      case MATERIALS -> List.of("general", "stats", "traits", "texture", "info");
      case ENTRIES -> List.of("effects", "info");
      case UPGRADES -> List.of("recipe", "tools");
    };
  }

  private static int tabFor(Mode mode) {
    return switch (mode) {
      case MATERIALS -> EditorSession.materialTab;
      case ENTRIES -> EditorSession.entryTab;
      case UPGRADES -> EditorSession.upgradeTab;
    };
  }

  private static void setTab(Mode mode, int tab) {
    switch (mode) {
      case MATERIALS -> EditorSession.materialTab = tab;
      case ENTRIES -> EditorSession.entryTab = tab;
      case UPGRADES -> EditorSession.upgradeTab = tab;
    }
  }

  /** Keeps the selection pointing at an existing document */
  private static void fixSelection(Workspace workspace) {
    if (EditorSession.material != null && !workspace.materials.contains(EditorSession.material)) {
      EditorSession.material = workspace.materials.isEmpty() ? null : workspace.materials.get(0);
    }
    if (EditorSession.entry != null && !workspace.entries.contains(EditorSession.entry)) {
      EditorSession.entry = workspace.entries.isEmpty() ? null : workspace.entries.get(0);
    }
    if (EditorSession.upgrade != null && !workspace.upgrades.contains(EditorSession.upgrade)) {
      EditorSession.upgrade = workspace.upgrades.isEmpty() ? null : workspace.upgrades.get(0);
    }
    if (EditorSession.upgrade == null && !workspace.upgrades.isEmpty()) {
      EditorSession.upgrade = workspace.upgrades.get(0);
    }
  }

  private void buildMain() {
    int mainX = winX + sideW;
    int mainY = winY + TOP_H;
    int mainW = winW - sideW;
    int mainH = winH - TOP_H - STATUS_H;
    Mode mode = EditorSession.mode;
    Workspace workspace = EditorSession.workspace();
    fixSelection(workspace);
    MaterialDoc material = EditorSession.material;
    EntryDoc entry = EditorSession.entry;
    UpgradeDoc upgrade = EditorSession.upgrade;
    boolean empty = switch (mode) {
      case MATERIALS -> material == null;
      case ENTRIES -> entry == null;
      case UPGRADES -> upgrade == null;
    };

    if (empty) {
      content = null;
      String modeKey = mode.name().toLowerCase(Locale.ROOT);
      Label emptyLabel = root.add(new Label(Names.tr("empty." + modeKey), Theme.TEXT_DIM).wrap(Math.min(260, mainW - 40)));
      emptyLabel.at(mainX + (mainW - emptyLabel.w) / 2, mainY + mainH / 2 - 20);
      Button create = root.add(new Button(Names.tr("button.new"), this::createNew).style(Button.Style.PRIMARY));
      if (mode == Mode.MATERIALS) {
        create.at(mainX + mainW / 2 - create.w - 2, mainY + mainH / 2 + 4);
        root.add(new Button(Names.tr("button.import"), this::importMaterial)).at(mainX + mainW / 2 + 2, mainY + mainH / 2 + 4);
      } else {
        create.at(mainX + (mainW - create.w) / 2, mainY + mainH / 2 + 4);
      }
      return;
    }

    // header: name, id and actions
    int x = mainX + 12;
    java.util.function.Supplier<Component> nameText = switch (mode) {
      case MATERIALS -> () -> Component.literal(material.displayName(EditorSession.uiLanguage()));
      case ENTRIES -> () -> Component.literal(entry.displayName(EditorSession.uiLanguage()));
      case UPGRADES -> () -> upgradeName(upgrade);
    };
    java.util.function.Supplier<Integer> nameColor = switch (mode) {
      case MATERIALS -> material::nameColor;
      case ENTRIES -> entry::colorInt;
      case UPGRADES -> () -> upgradeColor(upgrade);
    };
    java.util.function.Supplier<String> idText = switch (mode) {
      case MATERIALS -> () -> material.id;
      case ENTRIES -> () -> entry.id;
      case UPGRADES -> () -> upgrade.id;
    };
    root.add(new Label(nameText, Theme.TEXT).color(nameColor)).at(x, mainY + 7).size(mainW - 150, 9);
    root.add(new Label(() -> Component.literal(idText.get()), Theme.TEXT_FAINT)).at(x, mainY + 18).size(mainW - 150, 9);
    int right = mainX + mainW - 8;
    Button delete = new Button(Names.tr("button.delete"), () -> confirmDelete(nameText.get().getString())).style(Button.Style.GHOST);
    right -= delete.w;
    root.add(delete).at(right, mainY + 6);
    Button duplicate = new Button(Names.tr("button.duplicate"), this::duplicate).style(Button.Style.GHOST);
    right -= duplicate.w + 2;
    root.add(duplicate).at(right, mainY + 6);

    // tabs
    List<String> tabs = tabsFor(mode);
    int tabY = mainY + 30;
    int tx = mainX + 8;
    for (int i = 0; i < tabs.size(); i++) {
      int index = i;
      Button tab = new Button(Names.tr("tab." + tabs.get(i)), () -> {
        setTab(mode, index);
        UI.requestRebuild();
      }).style(Button.Style.TAB).selected(() -> tabFor(mode) == index);
      tab.h = 18;
      tab.w += 4;
      root.add(tab).at(tx, tabY);
      tx += tab.w + 2;
    }

    // page content
    int tab = Math.min(tabFor(mode), tabs.size() - 1);
    content = root.add(new ScrollView());
    content.at(mainX + 4, tabY + 20);
    content.size(mainW - 6, mainY + mainH - (tabY + 20) - 2);
    Page page = switch (mode) {
      case MATERIALS -> switch (tab) {
        case 1 -> new StatsPage(material);
        case 2 -> new TraitsPage(material);
        case 3 -> new TexturePage(material);
        case 4 -> new InfoPage(material);
        default -> new GeneralPage(material);
      };
      case ENTRIES -> tab == 1 ? new EntryInfoPage(entry) : new EntryEffectsPage(entry);
      case UPGRADES -> tab == 1 ? new UpgradeToolsPage(upgrade) : new UpgradeRecipePage(upgrade);
    };
    contentKey = mode.name() + ":" + tab;
    try {
      page.build(new Form(content));
    } catch (RuntimeException e) {
      TCMatEditor.LOG.error("Failed to build editor page", e);
      content.clear();
      content.add(new Label(Component.literal("Error: " + e), Theme.DANGER).wrap(content.w - 20)).at(8, 8);
    }
  }

  /* Actions */

  private void createNew() {
    switch (EditorSession.mode) {
      case MATERIALS -> newMaterial();
      case ENTRIES -> newEntry();
      case UPGRADES -> newUpgrade(null);
    }
  }

  private void newMaterial() {
    Workspace workspace = EditorSession.workspace();
    MaterialDoc doc = new MaterialDoc();
    doc.id = workspace.freeMaterialId(workspace.namespace, "new_material");
    int color = Colors.fromHsv(RANDOM.nextFloat(), 0.45f + RANDOM.nextFloat() * 0.3f, 0.8f, 255);
    doc.texture = TextureDoc.createDefault(color);
    for (String stat : List.of("tconstruct:head", "tconstruct:handle", "tconstruct:binding")) {
      doc.stats.put(stat, StatSchema.defaults(StatSchema.get(stat)));
    }
    doc.lang.put(EditorSession.uiLanguage(), new LangText(Names.tr("default.material_name").getString(), "", ""));
    workspace.materials.add(doc);
    EditorSession.material = doc;
    EditorSession.materialTab = 0;
    EditorSession.SCROLL.remove(Mode.MATERIALS.name() + ":0");
    Page.rebuild();
  }

  private void newEntry() {
    Workspace workspace = EditorSession.workspace();
    EntryDoc doc = new EntryDoc();
    doc.id = workspace.freeEntryId(workspace.namespace, "new_entry");
    doc.color = Colors.hex(Colors.fromHsv(RANDOM.nextFloat(), 0.6f, 0.95f, 255));
    EntryDoc.Line line = new EntryDoc.Line();
    doc.lines.add(line);
    doc.lang.put(EditorSession.uiLanguage(), new LangText(Names.tr("default.entry_name").getString(), "", ""));
    workspace.entries.add(doc);
    EditorSession.entry = doc;
    EditorSession.entryTab = 0;
    Page.rebuild();
  }

  /**
   * Creates an upgrade and switches to it
   * @param modifier  Modifier to apply, null to use the selected entry
   */
  public static void newUpgrade(@javax.annotation.Nullable String modifier) {
    Workspace workspace = EditorSession.workspace();
    UpgradeDoc doc = new UpgradeDoc();
    if (modifier == null) {
      EntryDoc entry = EditorSession.entry != null ? EditorSession.entry : workspace.entries.isEmpty() ? null : workspace.entries.get(0);
      modifier = entry != null ? entry.id : "";
    }
    doc.modifier = modifier;
    String base = modifier.isBlank() ? "new_upgrade" : modifier.substring(modifier.indexOf(':') + 1);
    doc.id = workspace.freeUpgradeId(workspace.namespace, base);
    doc.slot(0).ingredient = "minecraft:diamond";
    workspace.upgrades.add(doc);
    EditorSession.mode = Mode.UPGRADES;
    EditorSession.upgrade = doc;
    EditorSession.upgradeTab = 0;
    Page.rebuild();
  }

  private void importMaterial() {
    Pickers.material(Names.tr("picker.import"), source -> {
      Workspace workspace = EditorSession.workspace();
      String id = workspace.freeMaterialId(workspace.namespace, source.getIdentifier().getPath());
      try {
        MaterialDoc doc = MaterialImporter.importMaterial(source, id);
        workspace.materials.add(doc);
        EditorSession.material = doc;
        EditorSession.materialTab = 0;
        EditorSession.setStatus(Names.tr("status.imported", source.getIdentifier().toString()), Theme.SUCCESS);
        Page.rebuild();
      } catch (RuntimeException e) {
        TCMatEditor.LOG.error("Import failed", e);
        EditorSession.setStatus(Names.tr("status.error", e.getMessage()), Theme.DANGER);
      }
    });
  }

  private void duplicate() {
    Workspace workspace = EditorSession.workspace();
    switch (EditorSession.mode) {
      case MATERIALS -> {
        if (EditorSession.material != null) {
          MaterialDoc copy = EditorSession.material.copy();
          copy.id = workspace.freeMaterialId(copy.namespace(), copy.path() + "_copy");
          workspace.materials.add(copy);
          EditorSession.material = copy;
        }
      }
      case ENTRIES -> {
        if (EditorSession.entry != null) {
          EntryDoc copy = EditorSession.entry.copy();
          copy.id = workspace.freeEntryId(copy.namespace(), copy.path() + "_copy");
          workspace.entries.add(copy);
          EditorSession.entry = copy;
        }
      }
      case UPGRADES -> {
        if (EditorSession.upgrade != null) {
          UpgradeDoc copy = EditorSession.upgrade.copy();
          copy.id = workspace.freeUpgradeId(copy.namespace(), copy.path() + "_copy");
          workspace.upgrades.add(copy);
          EditorSession.upgrade = copy;
        }
      }
    }
    Page.rebuild();
  }

  private void confirmDelete(String name) {
    Dialogs.confirm(Names.tr("dialog.delete.title", name), Names.tr("dialog.delete.message"), Names.tr("button.delete"), () -> {
      Workspace workspace = EditorSession.workspace();
      switch (EditorSession.mode) {
        case MATERIALS -> EditorSession.material = removeAndNext(workspace.materials, EditorSession.material);
        case ENTRIES -> EditorSession.entry = removeAndNext(workspace.entries, EditorSession.entry);
        case UPGRADES -> EditorSession.upgrade = removeAndNext(workspace.upgrades, EditorSession.upgrade);
      }
      Page.rebuild();
    });
  }

  /** Removes the document and returns the one to select next */
  private static <T> T removeAndNext(List<T> docs, T doc) {
    int index = docs.indexOf(doc);
    docs.remove(doc);
    return docs.isEmpty() ? null : docs.get(Math.max(0, Math.min(index, docs.size() - 1)));
  }

  private void export() {
    try {
      Exporter.Result result = Exporter.export(EditorSession.workspace());
      EditorSession.setStatus(Names.tr("status.exported", result.folder().getFileName().toString()), Theme.SUCCESS);
      Util.getPlatform().openFile(result.folder().toFile());
    } catch (IOException | RuntimeException e) {
      TCMatEditor.LOG.error("Export failed", e);
      EditorSession.setStatus(Names.tr("status.error", e.getMessage()), Theme.DANGER);
    }
  }

  /* Rendering */

  @Override
  public void tick() {
    root.tick();
    if (UI.consumeRebuild()) {
      rebuild();
    }
  }

  @Override
  public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    g.fill(0, 0, width, height, Theme.DIM);
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    if (UI.consumeRebuild()) {
      rebuild();
    }
    renderBackground(g, mouseX, mouseY, partialTick);
    // window
    Theme.box(g, winX, winY, winW, winH, Theme.WINDOW);
    g.fill(winX, winY + TOP_H - 1, winX + winW, winY + TOP_H, Theme.BORDER);
    g.fill(winX + 1, winY + TOP_H, winX + sideW, winY + winH - STATUS_H, Theme.SIDEBAR);
    g.fill(winX + sideW, winY + TOP_H, winX + sideW + 1, winY + winH - STATUS_H, Theme.BORDER);
    g.fill(winX, winY + winH - STATUS_H, winX + winW, winY + winH - STATUS_H + 1, Theme.BORDER);
    if (content != null) {
      g.fill(winX + sideW + 1, content.y - 1, winX + winW - 1, content.y, Theme.BORDER);
    }

    UI.overlay = null;
    boolean popupOpen = !UI.POPUPS.isEmpty();
    int mx = popupOpen ? -10000 : mouseX;
    int my = popupOpen ? -10000 : mouseY;
    root.render(g, mx, my, partialTick);
    renderStatus(g);

    // popups above everything
    g.pose().pushPose();
    g.pose().translate(0, 0, 300);
    for (int i = 0; i < UI.POPUPS.size(); i++) {
      Popup popup = UI.POPUPS.get(i);
      boolean top = i == UI.POPUPS.size() - 1;
      popup.render(g, top ? mouseX : -10000, top ? mouseY : -10000, partialTick);
    }
    g.pose().popPose();

    // tooltip
    Component tooltip = popupOpen ? UI.POPUPS.get(UI.POPUPS.size() - 1).findTooltip(mouseX, mouseY) : root.findTooltip(mouseX, mouseY);
    if (UI.overlay != null) {
      g.pose().pushPose();
      g.pose().translate(0, 0, 400);
      UI.overlay.accept(g);
      g.pose().popPose();
    } else if (tooltip != null && !tooltip.getString().isEmpty()) {
      g.renderTooltip(font, font.split(tooltip, 220), mouseX, mouseY);
    }
  }

  private void renderStatus(GuiGraphics g) {
    int y = winY + winH - STATUS_H + 3;
    long age = System.currentTimeMillis() - EditorSession.statusTime;
    int color = age > 10000 ? Theme.TEXT_FAINT : EditorSession.statusColor;
    Theme.ellipsis(g, EditorSession.status, winX + 8, y, winW / 2, color);
    long now = System.currentTimeMillis();
    if (now - assetsCheckTime > 600) {
      assetsCheckTime = now;
      try {
        assetsDirty = ClientApply.assetsDirty();
      } catch (RuntimeException e) {
        assetsDirty = false;
      }
    }
    Component right = assetsDirty
      ? Names.tr("status.assets_pending").withColor(Theme.WARNING)
      : Names.tr("status.shortcuts").withColor(Theme.TEXT_FAINT);
    int rw = font.width(right);
    Theme.text(g, right, winX + winW - rw - 8, y, Theme.TEXT_FAINT);
  }

  /* Input */

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (!UI.POPUPS.isEmpty()) {
      Popup top = UI.POPUPS.get(UI.POPUPS.size() - 1);
      UI.beginClick();
      if (top.isHovered(mx, my)) {
        top.mouseClicked(mx, my, button);
      } else {
        top.close();
      }
      UI.endClick();
      return true;
    }
    UI.beginClick();
    boolean handled = root.mouseClicked(mx, my, button);
    UI.endClick();
    return handled || super.mouseClicked(mx, my, button);
  }

  @Override
  public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
    UIElement captured = UI.captured();
    if (captured != null) {
      return captured.mouseDragged(mx, my, button, dx, dy);
    }
    return false;
  }

  @Override
  public boolean mouseReleased(double mx, double my, int button) {
    UIElement captured = UI.captured();
    if (captured != null) {
      UI.release();
      return captured.mouseReleased(mx, my, button);
    }
    return super.mouseReleased(mx, my, button);
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    if (!UI.POPUPS.isEmpty()) {
      Popup top = UI.POPUPS.get(UI.POPUPS.size() - 1);
      return top.isHovered(mx, my) && top.mouseScrolled(mx, my, sx, sy);
    }
    return root.mouseScrolled(mx, my, sx, sy);
  }

  @Override
  public boolean keyPressed(int key, int scan, int modifiers) {
    if (key == GLFW.GLFW_KEY_ESCAPE) {
      if (!UI.POPUPS.isEmpty()) {
        UI.POPUPS.get(UI.POPUPS.size() - 1).close();
        return true;
      }
      if (UI.focused() != null) {
        UI.focus(null);
        return true;
      }
      onClose();
      return true;
    }
    if (key == GLFW.GLFW_KEY_S && hasControlDown()) {
      ClientApply.apply(true);
      return true;
    }
    UIElement focused = UI.focused();
    if (focused != null) {
      focused.keyPressed(key, scan, modifiers);
      // never let typing trigger other key bindings
      return true;
    }
    return super.keyPressed(key, scan, modifiers);
  }

  @Override
  public boolean charTyped(char c, int modifiers) {
    UIElement focused = UI.focused();
    if (focused != null) {
      return focused.charTyped(c, modifiers);
    }
    return false;
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return true;
  }
}
