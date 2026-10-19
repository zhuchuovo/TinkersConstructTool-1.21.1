package com.tcmateditor.exporttemplate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Standalone export runtime. Deliberately has no editor dependencies or @Mod annotation:
 * the exporter relocates these classes and adds @Mod only to the exported copy.
 */
public final class FluidBootstrap {
  public FluidBootstrap(IEventBus bus) {
    Map<String,DeferredRegister<FluidType>> types = new LinkedHashMap<>();
    Map<String,DeferredRegister<Fluid>> fluids = new LinkedHashMap<>();
    Map<String,DeferredRegister<Block>> blocks = new LinkedHashMap<>();
    Map<String,DeferredRegister<Item>> items = new LinkedHashMap<>();
    List<Registration> registrations = new ArrayList<>();
    try (InputStream stream = FluidBootstrap.class.getResourceAsStream("fluids.json")) {
      if (stream == null) throw new IllegalStateException("Missing exported fluid definitions");
      for (var element : JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray()) {
        JsonObject doc = element.getAsJsonObject();
        ResourceLocation id = ResourceLocation.parse(doc.get("id").getAsString());
        String ns = id.getNamespace();
        String path = id.getPath();
        String translation = "fluid." + ns + "." + path.replace('/', '.');
        DeferredHolder<FluidType,FluidType> type = types.computeIfAbsent(ns, key -> DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, key))
          .register(path, () -> new FluidType(FluidType.Properties.create()
            .descriptionId(translation)
            .temperature(doc.get("temperature").getAsInt())
            .density(doc.get("density").getAsInt())
            .viscosity(doc.get("viscosity").getAsInt())
            .lightLevel(doc.get("lightLevel").getAsInt())
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
        DeferredRegister<Fluid> fluidRegistry = fluids.computeIfAbsent(ns, key -> DeferredRegister.create(Registries.FLUID, key));
        Registration registration = new Registration(doc, type);
        BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(type, () -> registration.source.get(), () -> registration.flowing.get());
        registration.source = fluidRegistry.register(path, () -> new BaseFlowingFluid.Source(properties));
        registration.flowing = fluidRegistry.register("flowing_" + path, () -> new BaseFlowingFluid.Flowing(properties));
        if (doc.get("block").getAsBoolean()) {
          var block = blocks.computeIfAbsent(ns, key -> DeferredRegister.create(Registries.BLOCK, key))
            .register(path, () -> new LiquidBlock(registration.source.get(), BlockBehaviour.Properties.of().noCollission().strength(100).noLootTable().liquid()));
          properties.block(block);
        }
        if (doc.get("bucket").getAsBoolean()) {
          var bucket = items.computeIfAbsent(ns, key -> DeferredRegister.create(Registries.ITEM, key))
            .register(path + "_bucket", () -> new BucketItem(registration.source.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
          properties.bucket(bucket);
        }
        registrations.add(registration);
      }
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Cannot read exported fluid definitions", e);
    }
    types.values().forEach(registry -> registry.register(bus));
    fluids.values().forEach(registry -> registry.register(bus));
    blocks.values().forEach(registry -> registry.register(bus));
    items.values().forEach(registry -> registry.register(bus));
    if (FMLEnvironment.dist == Dist.CLIENT) Client.register(bus, registrations);
  }

  private static final class Registration {
    final JsonObject doc;
    final DeferredHolder<FluidType,FluidType> type;
    DeferredHolder<Fluid,BaseFlowingFluid.Source> source;
    DeferredHolder<Fluid,BaseFlowingFluid.Flowing> flowing;

    Registration(JsonObject doc, DeferredHolder<FluidType,FluidType> type) {
      this.doc = doc;
      this.type = type;
    }
  }

  /** Client classes are referenced only after checking the physical side. */
  private static final class Client {
    static void register(IEventBus bus, List<Registration> registrations) {
      bus.addListener((net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) -> {
        for (Registration registration : registrations) {
          JsonObject doc = registration.doc;
          ResourceLocation still = ResourceLocation.parse(doc.get("stillTexture").getAsString());
          ResourceLocation flowing = ResourceLocation.parse(doc.get("flowingTexture").getAsString());
          int tint = 0xFF000000 | Integer.parseInt(doc.get("color").getAsString().substring(1), 16);
          event.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return still; }
            @Override public ResourceLocation getFlowingTexture() { return flowing; }
            @Override public int getTintColor() { return tint; }
          }, registration.type.get());
        }
      });
      bus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) -> event.enqueueWork(() -> {
        for (Registration registration : registrations) {
          net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(registration.source.get(), net.minecraft.client.renderer.RenderType.translucent());
          net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(registration.flowing.get(), net.minecraft.client.renderer.RenderType.translucent());
        }
      }));
    }
  }
}
