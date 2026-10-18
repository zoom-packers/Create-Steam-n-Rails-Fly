/*
 * Steam 'n' Rails
 * Copyright (c) 2025 The Railways Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.railwayteam.railways.content.palettes.painting.fabric;

import com.railwayteam.railways.content.palettes.PalettesColor;
import com.railwayteam.railways.content.palettes.painting.PaintFluid;
import com.railwayteam.railways.content.palettes.painting.PaintPitcherItem;
import com.railwayteam.railways.content.palettes.painting.PitcherColor;
import com.railwayteam.railways.registry.CRFluids;
import com.railwayteam.railways.registry.fabric.CRFluidsImpl;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import static com.railwayteam.railways.content.palettes.painting.PaintPitcherItem.FLUID_PER_LEVEL;
import static com.railwayteam.railways.content.palettes.painting.PaintPitcherItem.MAX_LEVELS;

/**
 * NeoForge (ZP5 port): the paint pitcher's fluid handler as a NeoForge {@code Capabilities.Fluid.ITEM}, replacing
 * Fabric's item {@code FluidStorage}. NeoForge counts millibuckets, Fabric droplets (81 per mB); a level is still 1/8
 * bucket, and only whole levels are inserted or extracted.
 */
class PaintPitcherFluidStorage extends ItemAccessResourceHandler<FluidResource> {
    static final int MB_PER_LEVEL = (int) (FLUID_PER_LEVEL / 81);

    public PaintPitcherFluidStorage(ItemAccess access) {
        super(access, 1);
    }

    private static @Nullable PitcherColor getColor(ItemResource item) {
        if (item.getItem() instanceof PaintPitcherItem pitcher) {
            return new PitcherColor(pitcher.getColor());
        }
        return null;
    }

    private static int getLevels(ItemResource item) {
        ItemStack stack = item.toStack();
        if (!(stack.getItem() instanceof PaintPitcherItem pitcher)) return 0;
        return pitcher.getLevels(stack);
    }

    private static @Nullable PitcherColor getResourceColor(FluidResource resource) {
        if (resource.getFluid().isSame(Fluids.WATER))
            return PitcherColor.SANDY_WATER;

        if (!CRFluids.PAINT.get().isSame(resource.getFluid()))
            return null;

        PalettesColor fluidColor = CRFluidsImpl.getPaintColor(resource).orElse(null);
        if (fluidColor == null)
            return null;

        return new PitcherColor(fluidColor);
    }

    @Nullable
    private PitcherColor getColorIfValid(FluidResource resource) {
        PitcherColor color = getColor(itemAccess.getResource());
        PitcherColor fluidColor = getResourceColor(resource);
        if (fluidColor == null) return null;

        // Color mismatches can never be inserted or extracted
        if (color != null && !color.equals(fluidColor)) {
            return null;
        }

        return color == null ? fluidColor : color;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return getColorIfValid(resource) != null;
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        ItemStack stack = accessResource.toStack();
        if (!(stack.getItem() instanceof PaintPitcherItem item) || item.getLevels(stack) == 0)
            return FluidResource.EMPTY;

        PalettesColor color = item.getColor();
        return color == null ? FluidResource.of(Fluids.WATER) : FluidResource.of(
            CRFluids.PAINT.get(),
            DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_DATA, CustomData.of(PaintFluid.setColor(new CompoundTag(), color)))
                .build()
        );
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        return getLevels(accessResource) * MB_PER_LEVEL;
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return MAX_LEVELS * MB_PER_LEVEL;
    }

    @Override
    protected @Nullable ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        if (newAmount % MB_PER_LEVEL != 0)
            return ItemResource.EMPTY;
        PitcherColor color = getColor(accessResource);
        if (color == null)
            color = getResourceColor(newResource);
        if (color == null)
            return ItemResource.EMPTY;
        return ItemResource.of(color.getItemEntry().get().copyAsFilledStack(accessResource.toStack(), newAmount / MB_PER_LEVEL));
    }

    // Only whole levels move: round the requested amount down to a multiple of a level
    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        int levels = amount - amount % MB_PER_LEVEL;
        return levels <= 0 ? 0 : super.insert(index, resource, levels, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        int levels = amount - amount % MB_PER_LEVEL;
        return levels <= 0 ? 0 : super.extract(index, resource, levels, transaction);
    }

    @Override
    public String toString() {
        return "PaintPitcherFluidStorage[" + itemAccess + "]";
    }
}
