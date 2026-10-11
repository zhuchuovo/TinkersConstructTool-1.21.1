package com.tcmateditor.client.gui.pages;

import com.tcmateditor.EditorPaths;
import com.tcmateditor.client.gui.Form;
import com.tcmateditor.client.gui.Names;
import com.tcmateditor.client.gui.PreviewCache;
import com.tcmateditor.client.gui.PreviewCache.Preview;
import com.tcmateditor.client.gui.Theme;
import com.tcmateditor.client.gui.ui.Button;
import com.tcmateditor.client.gui.ui.ColorSwatch;
import com.tcmateditor.client.gui.ui.Dropdown;
import com.tcmateditor.client.gui.ui.Label;
import com.tcmateditor.client.gui.ui.NumberField;
import com.tcmateditor.client.gui.ui.PaletteBar;
import com.tcmateditor.client.gui.ui.Slider;
import com.tcmateditor.client.gui.ui.SpritePreview;
import com.tcmateditor.client.gui.ui.Toggle;
import com.tcmateditor.client.gui.ui.UIElement;
import com.tcmateditor.client.texture.PartTextureLibrary;
import com.tcmateditor.client.texture.TextureEngine;
import com.tcmateditor.model.MaterialDoc;
import com.tcmateditor.model.TextureDoc;
import com.tcmateditor.model.TextureDoc.Effect;
import com.tcmateditor.model.TextureDoc.Stop;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Texture editor: palette, base style, effect layers and animation, with live previews */
public class TexturePage implements Page {
  public static final List<String> STYLES = List.of("", "metal", "rock", "wood", "bone", "primitive", "stick", "cloth", "crystal", "slime_metal", "slime_wood", "vine", "contrast", "metal_contrast");
  /** Color for quick palette generation, kept while editing */
  private static int quickColor = 0;

  private final MaterialDoc doc;
  private PaletteBar bar;

  public TexturePage(MaterialDoc doc) {
    this.doc = doc;
  }

  private TextureDoc tex() {
    return doc.texture;
  }

  @Override
  public void build(Form form) {
    TextureDoc texture = tex();
    form.hint(Names.tr("page.texture.hint"));

    // previews
    int x = 0;
    int rowHeight = 0;
    for (Preview preview : PreviewCache.PREVIEWS) {
      boolean composite = preview.layers().size() > 1;
      int size = composite ? 48 : 32;
      if (x + size > form.width()) {
        form.next(rowHeight);
        x = 0;
        rowHeight = 0;
      }
      SpritePreview sprite = new SpritePreview(preview.key(), () -> PreviewCache.get(tex(), preview), () -> tex().animation.frametime, size);
      sprite.tooltip(preview.name());
      form.place(sprite, x);
      x += size + 4;
      rowHeight = Math.max(rowHeight, size);
    }
    form.next(rowHeight);

    // palette
    form.header(Names.tr("section.palette"));
    form.hint(Names.tr("section.palette.hint"));
    bar = form.full(new PaletteBar(this::tex, form.width(), Page::changed, () -> {}));
    Stop selected = bar.selected();
    form.place(new Label(Names.tr("label.selected_stop"), Theme.TEXT_DIM), 0).at(form.pad, form.y + 4);
    int col = form.labelWidth;
    form.place(new ColorSwatch(() -> bar.selected() == null ? 0 : bar.selected().color, color -> {
      Stop stop = bar.selected();
      if (stop != null) {
        stop.color = color | 0xFF000000;
        Page.changed();
      }
    }).withHex(), col);
    form.place(new Label(Names.tr("label.grey"), Theme.TEXT_FAINT), col + 70).at(form.pad + col + 70, form.y + 4);
    if (selected != null) {
      form.place(new NumberField(selected.grey, true, 0, 255, 1, 40).onValue(v -> {
        Stop stop = bar.selected();
        if (stop != null && texture.palette.stream().noneMatch(s -> s != stop && s.grey == (int) v)) {
          stop.grey = (int) v;
          texture.sortPalette();
          Page.changed();
        }
      }), col + 70 + Theme.font().width(Names.tr("label.grey")) + 4).tooltip(Names.tr("hint.grey"));
    }
    form.next(16);

    if (quickColor == 0) {
      quickColor = texture.mainColor();
    }
    form.place(new Label(Names.tr("label.quick_palette"), Theme.TEXT_DIM), 0).at(form.pad, form.y + 4);
    form.place(new ColorSwatch(() -> quickColor, c -> quickColor = c), col);
    Button generate = form.place(new Button(Names.tr("button.generate_palette"), () -> {
      texture.palette = TextureDoc.paletteFromColor(quickColor);
      Page.rebuild();
    }), col + 20);
    generate.tooltip(Names.tr("hint.generate_palette"));
    form.place(new Button(Names.tr("button.invert_palette"), () -> {
      List<Integer> colors = new ArrayList<>(texture.palette.stream().map(s -> s.color).toList());
      Collections.reverse(colors);
      for (int i = 0; i < colors.size(); i++) {
        texture.palette.get(i).color = colors.get(i);
      }
      Page.changed();
    }).fit(), col + 24 + generate.w);
    form.next(16);

    // style
    form.header(Names.tr("section.style"));
    form.row(Names.tr("label.base_style"), new Dropdown<>(STYLES, s -> Names.tr("style." + (s.isEmpty() ? "none" : s)), () -> texture.baseStyle, s -> {
      texture.baseStyle = s;
      Page.changed();
    }, 110)).tooltip(Names.tr("hint.base_style"));
    form.row(Names.tr("label.luminosity"), new Slider(0, 15, true, () -> texture.luminosity, v -> {
      texture.luminosity = (int) v;
      Page.changed();
    }, 130)).tooltip(Names.tr("hint.luminosity"));

    // effects
    form.header(Names.tr("section.effects"));
    form.hint(Names.tr("section.effects.hint"));
    for (int i = 0; i < texture.effects.size(); i++) {
      buildEffect(form, texture, texture.effects.get(i), i);
    }
    Button add = form.place(new Button(Names.tr("button.add_effect"), () -> {
      Effect effect = new Effect();
      effect.seed = texture.effects.size();
      texture.effects.add(effect);
      Page.rebuild();
    }), 0);
    form.place(new Button(Names.tr("button.open_patterns"), () -> {
      try {
        Files.createDirectories(EditorPaths.patterns());
      } catch (IOException ignored) {}
      PartTextureLibrary.clear();
      Util.getPlatform().openFile(EditorPaths.patterns().toFile());
    }).fit(), add.w + 4).tooltip(Names.tr("hint.open_patterns"));
    form.next(16);

    // animation
    form.header(Names.tr("section.animation"));
    TextureDoc.Animation animation = texture.animation;
    form.row(Names.tr("label.animation"), new Dropdown<>(TextureEngine.ANIMATIONS, a -> Names.tr("anim." + a), () -> animation.type, a -> {
      animation.type = a;
      Page.rebuild();
    }, 110)).tooltip(Names.tr("hint.animation"));
    if (animation.isActive()) {
      form.row(Names.tr("label.frames"), new NumberField(animation.frames, true, 2, 32, 1, 50).onValue(v -> { animation.frames = (int) v; Page.changed(); }));
      form.row(Names.tr("label.frametime"), new NumberField(animation.frametime, true, 1, 40, 1, 50).onValue(v -> { animation.frametime = (int) v; Page.changed(); }))
        .tooltip(Names.tr("hint.frametime"));
      form.row(Names.tr("label.color"), new ColorSwatch(() -> animation.color, c -> { animation.color = c; Page.changed(); }).withHex());
      form.row(Names.tr("label.strength"), new Slider(0, 1, false, () -> animation.strength, v -> { animation.strength = (float) v; Page.changed(); }, 130));
      form.row(Names.tr("label.interpolate"), new Toggle(Component.empty(), () -> animation.interpolate, v -> { animation.interpolate = v; Page.changed(); }))
        .tooltip(Names.tr("hint.interpolate"));
      form.hint(Names.tr("hint.animation_armor"));
    }
    form.finish();
  }

  private void buildEffect(Form form, TextureDoc texture, Effect effect, int index) {
    int width = form.width();
    int top = form.y;
    boolean pattern = "pattern".equals(effect.type);
    int cardH = pattern ? 58 : 40;
    form.view.add(new UIElement() {
      @Override
      public void render(GuiGraphics g, int mx, int my, float partialTick) {
        Theme.box(g, x, y, w, h, Theme.PANEL);
      }
    }.at(form.pad, top).size(width, cardH));
    form.y += 3;
    // row 1
    form.place(new Toggle(Component.empty(), () -> effect.enabled, v -> { effect.enabled = v; Page.changed(); }), 4).at(form.pad + 4, form.y + 2);
    form.place(new Dropdown<>(TextureEngine.EFFECT_TYPES, t -> Names.tr("effect." + t), () -> effect.type, t -> {
      effect.type = t;
      Page.rebuild();
    }, 72), 26).tooltip(() -> Names.tr("effect." + effect.type + ".hint"));
    if (!"outline".equals(effect.type)) {
      form.place(new Dropdown<>(TextureEngine.REGIONS, r -> Names.tr("region." + r), () -> effect.region, r -> {
        effect.region = r;
        Page.changed();
      }, 64), 102).tooltip(Names.tr("hint.region"));
    }
    if (!"hue".equals(effect.type) && !"contrast".equals(effect.type) && !"noise".equals(effect.type)) {
      form.place(new ColorSwatch(() -> effect.color, c -> { effect.color = c; Page.changed(); }), 170);
    }
    int buttons = width - 52;
    form.place(new Button(Component.literal("▲"), () -> {
      if (index > 0) {
        Collections.swap(texture.effects, index, index - 1);
        Page.rebuild();
      }
    }).style(Button.Style.GHOST).size(16, 16), buttons).tooltip(Names.tr("button.move_up"));
    form.place(new Button(Component.literal("▼"), () -> {
      if (index < texture.effects.size() - 1) {
        Collections.swap(texture.effects, index, index + 1);
        Page.rebuild();
      }
    }).style(Button.Style.GHOST).size(16, 16), buttons + 17).tooltip(Names.tr("button.move_down"));
    form.place(new Button(Component.literal("×"), () -> {
      texture.effects.remove(effect);
      Page.rebuild();
    }).style(Button.Style.GHOST).size(16, 16), buttons + 34).tooltip(Names.tr("button.remove"));
    form.next(16);
    // row 2
    form.place(new Label(Names.tr("label.strength"), Theme.TEXT_FAINT), 6).at(form.pad + 6, form.y + 3);
    int sx = 8 + Math.max(36, Theme.font().width(Names.tr("label.strength")) + 4);
    form.place(new Slider(0, 1, false, () -> effect.strength, v -> { effect.strength = (float) v; Page.changed(); }, 96), sx);
    if (List.of("noise", "stripes", "checker", "pattern").contains(effect.type)) {
      form.place(new Label(Names.tr("label.scale"), Theme.TEXT_FAINT), sx + 102).at(form.pad + sx + 102, form.y + 3);
      int nx = sx + 104 + Theme.font().width(Names.tr("label.scale"));
      form.place(new NumberField(effect.scale, true, 1, 16, 1, 30).onValue(v -> { effect.scale = (int) v; Page.changed(); }), nx).tooltip(Names.tr("hint.scale"));
    }
    if (List.of("noise", "sparkle", "pattern").contains(effect.type)) {
      int seedX = width - 72;
      form.place(new Label(Names.tr("label.seed"), Theme.TEXT_FAINT), seedX).at(form.pad + seedX, form.y + 3);
      form.place(new NumberField(effect.seed, true, 0, 9999, 1, 34).onValue(v -> { effect.seed = (int) v; Page.changed(); }), width - 38)
        .tooltip(Names.tr("hint.seed"));
    }
    form.next(14);
    if (pattern) {
      List<String> patterns = PartTextureLibrary.listPatterns();
      if (!effect.pattern.isEmpty() && !patterns.contains(effect.pattern)) {
        patterns = new ArrayList<>(patterns);
        patterns.add(0, effect.pattern);
      }
      if (effect.pattern.isEmpty() && !patterns.isEmpty()) {
        effect.pattern = patterns.get(0);
      }
      form.place(new Label(Names.tr("label.pattern"), Theme.TEXT_FAINT), 6).at(form.pad + 6, form.y + 4);
      form.place(new Dropdown<>(patterns, TexturePage::patternName, () -> effect.pattern, p -> {
        effect.pattern = p;
        Page.changed();
      }, Math.min(170, width - sx - 8)), sx).tooltip(Names.tr("hint.pattern"));
      form.next(16);
    }
    form.y = top + cardH + 4;
  }

  private static Component patternName(String key) {
    if (key.startsWith("file:")) {
      return Component.literal("★ " + key.substring(5));
    }
    int slash = key.lastIndexOf('/');
    return Component.literal(slash >= 0 ? key.substring(slash + 1) : key).append(Component.literal("  " + key.substring(0, key.indexOf(':'))).withColor(Theme.TEXT_FAINT));
  }
}
