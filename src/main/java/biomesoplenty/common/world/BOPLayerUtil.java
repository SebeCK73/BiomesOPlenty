/*******************************************************************************
 * Copyright 2014-2019, the Biomes O' Plenty Team
 *
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/
package biomesoplenty.common.world;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.IContextExtended;
import net.minecraft.world.gen.LazyAreaLayerContext;
import net.minecraft.world.gen.OverworldGenSettings;
import net.minecraft.world.gen.area.IArea;
import net.minecraft.world.gen.area.IAreaFactory;
import net.minecraft.world.gen.area.LazyArea;
import net.minecraft.world.gen.layer.*;

import java.util.function.LongFunction;

public class BOPLayerUtil
{
    public static <T extends IArea, C extends IContextExtended<T>> IAreaFactory<T> createInitialLandAndSeaProcedure(LongFunction<C> contextFactory)
    {
        // NOTE: Normally AddSnow, CoolWarm, HeatIce and Special GenLayers occur here, but we handle those ourselves
        IAreaFactory<T> factory = GenLayerIsland.INSTANCE.apply(contextFactory.apply(1L));
        factory = GenLayerZoom.FUZZY.apply(contextFactory.apply(2000L), factory);
        factory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(1L), factory);
        factory = GenLayerZoom.NORMAL.apply(contextFactory.apply(2001L), factory);
        factory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(2L), factory);
        factory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(50L), factory);
        factory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(70L), factory);
        factory = GenLayerRemoveTooMuchOcean.INSTANCE.apply(contextFactory.apply(2L), factory);
        factory = GenLayerAddSnow.INSTANCE.apply(contextFactory.apply(2L), factory);
        factory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(3L), factory);
        factory = GenLayerEdge.CoolWarm.INSTANCE.apply(contextFactory.apply(2L), factory);
        factory = GenLayerEdge.HeatIce.INSTANCE.apply(contextFactory.apply(2L), factory);
        factory = GenLayerEdge.Special.INSTANCE.apply(contextFactory.apply(3L), factory);
        factory = GenLayerZoom.NORMAL.apply(contextFactory.apply(2002L), factory);
        factory = GenLayerZoom.NORMAL.apply(contextFactory.apply(2003L), factory);
        factory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(4L), factory);
        factory = GenLayerAddMushroomIsland.INSTANCE.apply(contextFactory.apply(5L), factory);
        factory = GenLayerDeepOcean.INSTANCE.apply(contextFactory.apply(4L), factory);
        return factory;
    }

    public static <T extends IArea, C extends IContextExtended<T>> ImmutableList<IAreaFactory<T>> createAreaFactories(WorldType worldTypeIn, OverworldGenSettings settings, LongFunction<C> contextFactory)
    {
        // Create the initial land and sea layer. Is also responsible for adding deep oceans
        // and mushroom islands
        IAreaFactory<T> landSeaFactory = createInitialLandAndSeaProcedure(contextFactory);

        // Determines positions for all of the new ocean subbiomes added in 1.13
        IAreaFactory<T> oceanBiomeFactory = OceanLayer.INSTANCE.apply(contextFactory.apply(2L));
        oceanBiomeFactory = LayerUtil.repeat(2001L, GenLayerZoom.NORMAL, oceanBiomeFactory, 6, contextFactory);

        int biomeSize = 4;
        int riverSize = biomeSize;
        if (settings != null) {
            biomeSize = settings.getBiomeSize();
            riverSize = settings.getRiverSize();
        }

        biomeSize = LayerUtil.getModdedBiomeSize(worldTypeIn, biomeSize);

        // Fork off a new branch as a seed for rivers and sub biomes
        IAreaFactory<T> riverAndSubBiomesInitFactory = GenLayerRiverInit.INSTANCE.apply(contextFactory.apply(100L), landSeaFactory);
        riverAndSubBiomesInitFactory = LayerUtil.repeat(1000L, GenLayerZoom.NORMAL, riverAndSubBiomesInitFactory, 2, contextFactory);

        // Allocate the biomes
        IAreaFactory<T> biomesFactory = worldTypeIn.getBiomeLayer(landSeaFactory, settings, contextFactory);
        biomesFactory = GenLayerHills.INSTANCE.apply(contextFactory.apply(1000L), biomesFactory, riverAndSubBiomesInitFactory);

        // Develop the rivers branch
        IAreaFactory<T> riversInitFactory = LayerUtil.repeat(1000L, GenLayerZoom.NORMAL, riverAndSubBiomesInitFactory, riverSize, contextFactory);
        riversInitFactory = GenLayerRiver.INSTANCE.apply(contextFactory.apply(1L), riversInitFactory);
        riversInitFactory = GenLayerSmooth.INSTANCE.apply(contextFactory.apply(1000L), riversInitFactory);

        // Mix in rare biomes into biomes branch
        biomesFactory = GenLayerRareBiome.INSTANCE.apply(contextFactory.apply(1001L), biomesFactory);

        // Zoom more based on the biome size
        for (int i = 0; i < biomeSize; ++i)
        {
            biomesFactory = GenLayerZoom.NORMAL.apply(contextFactory.apply((long)(1000 + i)), biomesFactory);
            if (i == 0) biomesFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(3L), biomesFactory);
            if (i == 1 || biomeSize == 1) biomesFactory = GenLayerShore.INSTANCE.apply(contextFactory.apply(1000L), biomesFactory);
        }

        biomesFactory = GenLayerSmooth.INSTANCE.apply(contextFactory.apply(1000L), biomesFactory);

        // Mix rivers into the biomes branch
        biomesFactory = GenLayerRiverMix.INSTANCE.apply(contextFactory.apply(100L), biomesFactory, riversInitFactory);
        biomesFactory = GenLayerMixOceans.INSTANCE.apply(contextFactory.apply(100L), biomesFactory, oceanBiomeFactory);

        // Finish biomes with Voroni zoom
        IAreaFactory<T> voroniZoomBiomesFactory = GenLayerVoronoiZoom.INSTANCE.apply(contextFactory.apply(10L), biomesFactory);

        return ImmutableList.of(biomesFactory, voroniZoomBiomesFactory, biomesFactory);
    }

    public static GenLayer[] createGenLayers(long seed, WorldType worldType, OverworldGenSettings settings)
    {
        int[] layerCount = new int[1]; // Do this as an array to enable incrementing it in the lambda
        ImmutableList<IAreaFactory<LazyArea>> factoryList = createAreaFactories(worldType, settings, (seedModifier) ->
        {
            ++layerCount[0];
            return new LazyAreaLayerContext(1, layerCount[0], seed, seedModifier);
        });
        GenLayer biomesLayer = new GenLayer(factoryList.get(0));
        GenLayer voroniZoomBiomesLayer = new GenLayer(factoryList.get(1));
        GenLayer biomesLayer2 = new GenLayer(factoryList.get(2));
        return new GenLayer[]{biomesLayer, voroniZoomBiomesLayer, biomesLayer2};
    }
}
