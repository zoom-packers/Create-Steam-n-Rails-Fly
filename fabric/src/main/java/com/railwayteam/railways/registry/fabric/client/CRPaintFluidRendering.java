/*
 * Steam 'n' Rails
 * Copyright (c) 2026 The Railways Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.railwayteam.railways.registry.fabric.client;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.palettes.PalettesColor;
import com.railwayteam.railways.content.palettes.painting.PaintFluid;
import com.railwayteam.railways.neoforge.RailwaysNeoForgeInit;
import com.railwayteam.railways.registry.CRFluids;
import com.railwayteam.railways.registry.fabric.CRFluidsImpl;
import com.railwayteam.railways.util.Utils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * NeoForge (ZP5 port): the paint fluid model and its component-aware tint, through RegisterFluidModelsEvent and a
 * {@link FluidTintSource} instead of Fabric's FluidRenderingRegistry and FluidVariantRendering. Create Fly's NeoForge
 * port reads the tint source for the fluid stacks it renders.
 */
@Environment(EnvType.CLIENT)
public final class CRPaintFluidRendering {
    private static final PaintTint TINT = new PaintTint();
    private static boolean tintChecked;

    private CRPaintFluidRendering() {
    }

    public static void register() {
        RailwaysNeoForgeInit.onModBus(bus -> bus.addListener(RegisterFluidModelsEvent.class, event -> event.register(
            new FluidModel.Unbaked(
                new Material(Railways.asResource("fluid/paint_still/white")),
                new Material(Railways.asResource("fluid/paint_flow/white")),
                null,
                TINT
            ),
            CRFluids.PAINT.get()
        )));

        // A FluidStack needs the fluid's data components. On 26.3 the client binds them from the server's registry
        // data when it joins a world, so the check can't run while the game loads
        if (Utils.isDevEnv())
            NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> checkTint());
    }

    private static void checkTint() {
        if (tintChecked)
            return;
        tintChecked = true;
        FluidStack redPaint = new FluidStack(CRFluids.PAINT.get(), 1000);
        redPaint.set(DataComponents.CUSTOM_DATA, CustomData.of(PaintFluid.setColor(new CompoundTag(), PalettesColor.RED)));
        int expectedColor = 0xff000000 | PalettesColor.RED.getDiffuseColor();
        if (TINT.colorAsStack(redPaint) != expectedColor) {
            throw new IllegalStateException("Paint fluid component tint runtime check failed");
        }
        Railways.LOGGER.info("Paint fluid model and component-aware tint runtime check passed");
    }

    private static class PaintTint implements FluidTintSource {
        @Override
        public int color(FluidState state) {
            return 0xff000000 | PalettesColor.NETHERITE.getDiffuseColor();
        }

        @Override
        public int colorAsStack(FluidStack stack) {
            return 0xff000000 | CRFluidsImpl.getPaintColor(stack)
                .orElse(PalettesColor.NETHERITE)
                .getDiffuseColor();
        }
    }
}
