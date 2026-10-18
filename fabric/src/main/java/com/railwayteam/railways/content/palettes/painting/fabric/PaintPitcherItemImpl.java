package com.railwayteam.railways.content.palettes.painting.fabric;

import com.railwayteam.railways.content.palettes.PalettesColor;
import com.railwayteam.railways.content.palettes.painting.PaintPitcherItem;
import com.railwayteam.railways.neoforge.RailwaysNeoForgeInit;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public class PaintPitcherItemImpl extends PaintPitcherItem {
    public PaintPitcherItemImpl(Properties properties, @Nullable PalettesColor color) {
        super(properties, color);

        // NeoForge: item fluid capability instead of Fabric's FluidStorage.ITEM
        RailwaysNeoForgeInit.onModBus(bus -> bus.addListener(RegisterCapabilitiesEvent.class, event ->
            event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> new PaintPitcherFluidStorage(access), this)));
    }

    public static PaintPitcherItem create(Item.Properties properties, @Nullable PalettesColor color) {
        return new PaintPitcherItemImpl(properties, color);
    }
}
