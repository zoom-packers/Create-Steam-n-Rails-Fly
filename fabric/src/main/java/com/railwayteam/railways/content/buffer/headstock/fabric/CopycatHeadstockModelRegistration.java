/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2026 The Railways Team
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

package com.railwayteam.railways.content.buffer.headstock.fabric;

import com.railwayteam.railways.content.buffer.TrackBufferBlock;
import com.railwayteam.railways.content.buffer.fabric.BufferModel;
import com.railwayteam.railways.content.buffer.headstock.CopycatHeadstockBarsBlock;
import com.railwayteam.railways.content.buffer.headstock.CopycatHeadstockBlock;
import com.railwayteam.railways.content.buffer.headstock.HeadstockBlock;
import com.railwayteam.railways.content.buffer.single_deco.GenericDyeableSingleBufferBlock;
import com.railwayteam.railways.content.buffer.single_deco.LinkPinBlock;
import com.railwayteam.railways.content.custom_tracks.generic_crossing.GenericCrossingBlock;
import com.railwayteam.railways.content.custom_tracks.generic_crossing.fabric.GenericCrossingModel;
import com.zurrtum.create.client.AllModels;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/**
 * NeoForge (ZP5 port): upstream wrapped these block models with Fabric API's ModelLoadingPlugin. Create Fly's NeoForge
 * port applies {@link AllModels} resolvers at the same point (before baking), so the wrappers are registered there.
 * TODO(neoforge 26.3): the item-model wrappers (CopycatHeadstockBlockStateModel/BufferModel.UnbakedItemModel) have
 * no NeoForge hook yet; those items render with their plain JSON model.
 */
public class CopycatHeadstockModelRegistration {
	private static boolean registered;

	public static void register() {
		if (registered)
			return;
		registered = true;
		for (Block block : BuiltInRegistries.BLOCK) {
			if (block instanceof CopycatHeadstockBlock)
				AllModels.register(block, CopycatHeadstockBlockStateModel::new);
			else if (block instanceof CopycatHeadstockBarsBlock)
				AllModels.register(block, CopycatHeadstockBarsModel::new);
			else if (block instanceof GenericCrossingBlock)
				AllModels.register(block, GenericCrossingModel::new);
			else if (isBufferBlock(block))
				AllModels.register(block, BufferModel::new);
		}
	}

	private static boolean isBufferBlock(Block block) {
		return block instanceof TrackBufferBlock<?>
			|| block instanceof GenericDyeableSingleBufferBlock
			|| block instanceof LinkPinBlock
			|| block instanceof HeadstockBlock;
	}
}
