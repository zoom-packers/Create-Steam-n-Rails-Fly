package com.railwayteam.railways.content.palettes.painting.fabric;

import com.railwayteam.railways.content.palettes.painting.EmptyPaintPitcherItem;
import com.railwayteam.railways.neoforge.RailwaysNeoForgeInit;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.world.item.Item;

public class EmptyPaintPitcherItemImpl extends EmptyPaintPitcherItem {
    public EmptyPaintPitcherItemImpl(Properties properties) {
        super(properties);

        // NeoForge: item fluid capability instead of Fabric's FluidStorage.ITEM
        RailwaysNeoForgeInit.onModBus(bus -> bus.addListener(RegisterCapabilitiesEvent.class, event ->
            event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> new PaintPitcherFluidStorage(access), this)));
    }

    public static EmptyPaintPitcherItem create(Item.Properties properties) {
        return new EmptyPaintPitcherItemImpl(properties);
    }
}
