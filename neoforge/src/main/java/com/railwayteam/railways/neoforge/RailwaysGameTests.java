/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2024 The Railways Team
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

package com.railwayteam.railways.neoforge;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.fuel.tank.FuelTankBlockEntity;
import com.railwayteam.railways.registry.fabric.CRBlocksImpl;
import com.zurrtum.create.Create;
import com.zurrtum.create.content.trains.graph.TrackEdge;
import com.zurrtum.create.content.trains.graph.TrackGraph;
import com.zurrtum.create.content.trains.graph.TrackNode;
import com.zurrtum.create.content.trains.graph.TrackNodeLocation;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackShape;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.function.Consumer;

/**
 * ZP5 port checks, run by {@code runGameTestServer} (namespace {@code railways}). They build their setup inside
 * Create's empty 9x7x5 test structure.
 */
public final class RailwaysGameTests {
	private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Railways.MOD_ID);
	private static final ResourceKey<Consumer<GameTestHelper>> FUEL_TANK = key("fuel_tank_takes_lava_not_water");
	private static final ResourceKey<Consumer<GameTestHelper>> TRACK_NETWORK = key("railways_track_joins_create_network");

	static {
		FUNCTIONS.register(FUEL_TANK.identifier().getPath(), () -> RailwaysGameTests::fuelTankTakesLavaNotWater);
		FUNCTIONS.register(TRACK_NETWORK.identifier().getPath(), () -> RailwaysGameTests::trackJoinsCreateNetwork);
	}

	private RailwaysGameTests() {
	}

	private static ResourceKey<Consumer<GameTestHelper>> key(String path) {
		return ResourceKey.create(Registries.TEST_FUNCTION, Railways.asResource(path));
	}

	public static void register(IEventBus modBus) {
		FUNCTIONS.register(modBus);
		modBus.addListener(RailwaysGameTests::registerTests);
	}

	private static void registerTests(RegisterGameTestsEvent event) {
		Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Railways.asResource("default"));
		Identifier structure = Identifier.fromNamespaceAndPath(Create.MOD_ID, "gametest/empty_9x7x5");
		for (ResourceKey<Consumer<GameTestHelper>> test : new ResourceKey[] {FUEL_TANK, TRACK_NETWORK})
			event.registerTest(test.identifier(), new FunctionGameTestInstance(test, new TestData<>(environment, structure, 400, 0, true)));
	}

	/**
	 * A fuel tank exposes NeoForge's fluid capability (through Create's capability bridge): it takes a bucket of lava,
	 * a liquid fuel, and refuses water. Create stores the lava in droplets (81 per mB).
	 */
	private static void fuelTankTakesLavaNotWater(GameTestHelper helper) {
		BlockPos tank = new BlockPos(4, 1, 2);
		helper.setBlock(tank, CRBlocksImpl.FUEL_TANK.get());
		helper.runAfterDelay(5, () -> {
			ResourceHandler<FluidResource> handler = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(tank), Direction.UP);
			helper.assertTrue(handler != null, "fuel tank exposes no NeoForge fluid capability");
			int water;
			int lava;
			try (Transaction transaction = Transaction.openRoot()) {
				water = handler.insert(FluidResource.of(Fluids.WATER), 1000, transaction);
				lava = handler.insert(FluidResource.of(Fluids.LAVA), 1000, transaction);
				transaction.commit();
			}
			FluidStack stored = helper.getBlockEntity(tank, FuelTankBlockEntity.class).getTankInventory().getFluid();
			String state = "water inserted " + water + " mB, lava inserted " + lava + " mB, tank holds " + stored.getAmount() + " droplets of " + BuiltInRegistries.FLUID.getKey(stored.getFluid());
			helper.assertTrue(water == 0 && lava == 1000 && stored.isOf(Fluids.LAVA) && stored.getAmount() == 81_000, state);
			Railways.LOGGER.info("fuel_tank_takes_lava_not_water passed: {}", state);
			helper.succeed();
		});
	}

	/**
	 * A straight line of Railways acacia track, placed in the world, becomes part of Create's train network: a track
	 * graph gets nodes in the test area whose edges carry the Railways track material.
	 */
	private static void trackJoinsCreateNetwork(GameTestHelper helper) {
		Block track = BuiltInRegistries.BLOCK.getValue(Railways.asResource("track_acacia"));
		helper.assertTrue(track instanceof TrackBlock, "railways:track_acacia is not a Create track block: " + track);
		for (int x = 0; x < 9; x++) {
			helper.setBlock(new BlockPos(x, 0, 2), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 1, 2), track.defaultBlockState().setValue(TrackBlock.SHAPE, TrackShape.XO));
		}
		AABB area = helper.getBounds().inflate(1);
		helper.succeedWhen(() -> {
			int nodes = 0;
			String material = null;
			for (TrackGraph graph : Create.RAILWAYS.trackNetworks.values()) {
				for (TrackNodeLocation location : graph.getNodes()) {
					Vec3 position = location.getLocation();
					if (!area.contains(position))
						continue;
					nodes++;
					TrackNode node = graph.locateNode(location);
					for (TrackEdge edge : graph.getConnectionsFrom(node).values())
						material = edge.getTrackMaterial().getId().toString();
				}
			}
			String state = nodes + " graph nodes in the test area, edge material " + material;
			helper.assertTrue(nodes >= 2 && material != null && material.startsWith(Railways.MOD_ID + ":"), state);
			Railways.LOGGER.info("railways_track_joins_create_network passed at tick {}: {}", helper.getTick(), state);
		});
	}
}
