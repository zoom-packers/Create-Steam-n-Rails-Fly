package com.railwayteam.railways.content.switches.fabric;

import com.mojang.serialization.MapCodec;
import com.railwayteam.railways.content.switches.TrackSwitchBlock;

public class TrackSwitchBlockImpl extends TrackSwitchBlock {

    protected TrackSwitchBlockImpl(Properties properties, boolean isAutomatic) {
        super(properties, isAutomatic);
    }

    // 26.3: blocks no longer have codecs

    public static TrackSwitchBlock manual(Properties properties) {
        return new TrackSwitchBlockImpl(properties, false);
    }

    public static TrackSwitchBlock automatic(Properties properties) {
        return new TrackSwitchBlockImpl(properties, true);
    }
}
