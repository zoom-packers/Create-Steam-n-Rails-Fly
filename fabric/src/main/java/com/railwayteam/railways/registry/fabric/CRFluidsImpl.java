package com.railwayteam.railways.registry.fabric;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.palettes.PalettesColor;
import com.railwayteam.railways.content.palettes.painting.PaintFluid;
import com.railwayteam.railways.internal.compat.create.content.fluids.VirtualFluid;
import com.railwayteam.railways.internal.compat.registrate.util.entry.FluidEntry;
import com.railwayteam.railways.neoforge.RailwaysNeoForgeInit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;

public class CRFluidsImpl {
    // NeoForge: every fluid needs a FluidType. Paint's name follows the color in its custom data, which upstream
    // did with Fabric's FluidVariantAttributeHandler.
    private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Railways.MOD_ID);
    public static final DeferredHolder<FluidType, FluidType> PAINT_TYPE = FLUID_TYPES.register("paint", () -> new FluidType(
        FluidType.Properties.create().descriptionId("fluid.railways.paint").canSwim(false).canDrown(false).supportsBoating(false)
    ) {
        @Override
        public Component getDescription(FluidStack stack) {
            return Component.translatable(getPaintColor(stack)
                .map(PalettesColor::getPaintNameId)
                .orElse("fluid.railways.paint"));
        }
    });

    static {
        RailwaysNeoForgeInit.onModBus(FLUID_TYPES::register);
    }

    public static FluidEntry<VirtualFluid> registerPaint() {
        VirtualFluid paint = Registry.register(BuiltInRegistries.FLUID, Railways.asResource("paint"), new VirtualFluid());
        return new FluidEntry<>(Railways.asResource("paint"), paint);
    }

    @Environment(EnvType.CLIENT)
    public static void initRendering() {
        com.railwayteam.railways.registry.fabric.client.CRPaintFluidRendering.register();
    }

    public static Optional<PalettesColor> getPaintColor(DataComponentGetter components) {
        CustomData data = components.get(DataComponents.CUSTOM_DATA);
        return data == null ? Optional.empty() : PaintFluid.getColor(data.copyTag());
    }
}
