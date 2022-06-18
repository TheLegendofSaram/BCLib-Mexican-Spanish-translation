package org.betterx.bclib.api.v3.levelgen.features;

import org.betterx.bclib.api.v2.levelgen.features.BCLFeature;
import org.betterx.bclib.api.v2.levelgen.features.config.PillarFeatureConfig;
import org.betterx.bclib.api.v2.levelgen.features.config.PlaceFacingBlockConfig;
import org.betterx.bclib.api.v2.levelgen.features.config.SequenceFeatureConfig;
import org.betterx.bclib.api.v2.levelgen.features.config.TemplateFeatureConfig;
import org.betterx.bclib.api.v2.levelgen.features.features.PillarFeature;
import org.betterx.bclib.api.v2.levelgen.features.features.PlaceBlockFeature;
import org.betterx.bclib.api.v2.levelgen.features.features.SequenceFeature;
import org.betterx.bclib.api.v2.levelgen.features.features.TemplateFeature;
import org.betterx.bclib.api.v2.levelgen.structures.StructurePlacementType;
import org.betterx.bclib.api.v2.levelgen.structures.StructureWorldNBT;
import org.betterx.bclib.api.v2.poi.BCLPoiType;
import org.betterx.bclib.blocks.BlockProperties;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;

public abstract class BCLFeatureBuilder<F extends Feature<FC>, FC extends FeatureConfiguration> {
    private final ResourceLocation featureID;
    private final F feature;

    private BCLFeatureBuilder(ResourceLocation featureID, F feature) {
        this.featureID = featureID;
        this.feature = feature;
    }

    /**
     * Starts a new {@link BCLFeature} builder.
     *
     * @param featureID {@link ResourceLocation} feature identifier.
     * @param feature   {@link Feature} to construct.
     * @return {@link org.betterx.bclib.api.v2.levelgen.features.BCLFeatureBuilder} instance.
     */
    public static <F extends Feature<FC>, FC extends FeatureConfiguration> WithConfiguration<F, FC> start(
            ResourceLocation featureID,
            F feature
    ) {
        return new WithConfiguration<>(featureID, feature);
    }

    public static ForSimpleBlock start(
            ResourceLocation featureID,
            Block block
    ) {
        return start(featureID, BlockStateProvider.simple(block));
    }

    public static ForSimpleBlock start(
            ResourceLocation featureID,
            BlockState state
    ) {
        return start(featureID, BlockStateProvider.simple(state));
    }

    public static ForSimpleBlock start(
            ResourceLocation featureID,
            BlockStateProvider provider
    ) {
        return new ForSimpleBlock(
                featureID,
                (SimpleBlockFeature) Feature.SIMPLE_BLOCK,
                provider
        );
    }

    public static WeightedBlock startWeighted(
            ResourceLocation featureID
    ) {
        return new WeightedBlock(
                featureID,
                (SimpleBlockFeature) Feature.SIMPLE_BLOCK
        );
    }

    public static RandomPatch startRandomPatch(
            ResourceLocation featureID,
            Holder<PlacedFeature> featureToPlace
    ) {
        return new RandomPatch(
                featureID,
                (RandomPatchFeature) Feature.RANDOM_PATCH,
                featureToPlace
        );
    }

    public static NetherForrestVegetation startNetherVegetation(
            ResourceLocation featureID
    ) {
        return new NetherForrestVegetation(
                featureID,
                (NetherForestVegetationFeature) Feature.NETHER_FOREST_VEGETATION
        );
    }


    public static WithTemplates startWithTemplates(
            ResourceLocation featureID
    ) {
        return new WithTemplates(
                featureID,
                (TemplateFeature<TemplateFeatureConfig>) BCLFeature.TEMPLATE
        );
    }

    public static AsBlockColumn<BlockColumnFeature> startColumn(
            ResourceLocation featureID
    ) {
        return new AsBlockColumn<>(
                featureID,
                (BlockColumnFeature) Feature.BLOCK_COLUMN
        );
    }

    public static AsPillar startPillar(
            ResourceLocation featureID,
            PillarFeatureConfig.KnownTransformers transformer
    ) {
        return new AsPillar(
                featureID,
                (PillarFeature) BCLFeature.PILLAR,
                transformer
        );
    }

    public static AsSequence startSequence(
            ResourceLocation featureID
    ) {
        return new AsSequence(
                featureID,
                (SequenceFeature) BCLFeature.SEQUENCE
        );
    }

    public static AsOre startOre(
            ResourceLocation featureID
    ) {
        return new AsOre(
                featureID,
                (OreFeature) Feature.ORE
        );
    }

    public static FacingBlock startFacing(
            ResourceLocation featureID
    ) {
        return new FacingBlock(
                featureID,
                (PlaceBlockFeature<PlaceFacingBlockConfig>) BCLFeature.PLACE_BLOCK
        );
    }

    /**
     * Internally used by the builder. Normally you should not have to call this method directly as it is
     * handled by {@link #buildAndRegister()}
     *
     * @param id       The ID to register this feature with
     * @param cFeature The configured Feature
     * @param <F>      The Feature Class
     * @param <FC>     The FeatureConfiguration Class
     * @return The Holder for the new Feature
     */
    public static <F extends Feature<FC>, FC extends FeatureConfiguration> Holder<ConfiguredFeature<FC, F>> register(
            ResourceLocation id,
            ConfiguredFeature<FC, F> cFeature
    ) {
        return (Holder<ConfiguredFeature<FC, F>>) (Object) BuiltinRegistries.register(
                BuiltinRegistries.CONFIGURED_FEATURE,
                id,
                cFeature
        );
    }

    public abstract FC createConfiguration();

    protected BCLConfigureFeature<F, FC> buildAndRegister(BiFunction<ResourceLocation, ConfiguredFeature<FC, F>, Holder<ConfiguredFeature<FC, F>>> holderBuilder) {
        FC config = createConfiguration();
        if (config == null) {
            throw new IllegalStateException("Feature configuration for " + featureID + " can not be null!");
        }
        ConfiguredFeature<FC, F> cFeature = new ConfiguredFeature<>(feature, config);
        Holder<ConfiguredFeature<FC, F>> holder = holderBuilder.apply(featureID, cFeature);
        return new BCLConfigureFeature<>(featureID, holder, true);
    }

    public BCLConfigureFeature<F, FC> buildAndRegister() {
        return buildAndRegister(BCLFeatureBuilder::register);
    }

    public BCLConfigureFeature<F, FC> build() {
        return buildAndRegister((id, cFeature) -> Holder.direct(cFeature));
    }

    public BCLInlinePlacedBuilder<F, FC> inlinePlace() {
        BCLConfigureFeature<F, FC> f = build();
        return BCLInlinePlacedBuilder.place(f);
    }

    public Holder<PlacedFeature> inlinePlace(BCLInlinePlacedBuilder<F, FC> placer) {
        BCLConfigureFeature<F, FC> f = build();
        return placer.build(f);
    }

    public static class AsOre extends BCLFeatureBuilder<OreFeature, OreConfiguration> {
        private final List<OreConfiguration.TargetBlockState> targetStates = new LinkedList<>();
        private int size = 6;
        private float discardChanceOnAirExposure = 0;

        private AsOre(ResourceLocation featureID, OreFeature feature) {
            super(featureID, feature);
        }

        public AsOre add(Block containedIn, Block ore) {
            return this.add(containedIn, ore.defaultBlockState());
        }

        public AsOre add(Block containedIn, BlockState ore) {
            return this.add(new BlockMatchTest(containedIn), ore);
        }

        public AsOre add(RuleTest containedIn, Block ore) {
            return this.add(containedIn, ore.defaultBlockState());
        }

        public AsOre add(RuleTest containedIn, BlockState ore) {
            targetStates.add(OreConfiguration.target(
                    containedIn,
                    ore
            ));
            return this;
        }

        public AsOre veinSize(int size) {
            this.size = size;
            return this;
        }

        public AsOre discardChanceOnAirExposure(float chance) {
            this.discardChanceOnAirExposure = chance;
            return this;
        }

        @Override
        public OreConfiguration createConfiguration() {
            return new OreConfiguration(targetStates, size, discardChanceOnAirExposure);
        }
    }

    public static class AsPillar extends BCLFeatureBuilder<PillarFeature, PillarFeatureConfig> {
        private IntProvider height;
        private BlockStateProvider stateProvider;

        private final PillarFeatureConfig.KnownTransformers transformer;
        private Direction direction = Direction.UP;
        private BlockPredicate allowedPlacement = BlockPredicate.ONLY_IN_AIR_PREDICATE;

        private AsPillar(
                ResourceLocation featureID,
                PillarFeature feature,
                PillarFeatureConfig.KnownTransformers transformer
        ) {
            super(featureID, feature);
            this.transformer = transformer;
        }

        public AsPillar allowedPlacement(BlockPredicate predicate) {
            this.allowedPlacement = predicate;
            return this;
        }

        public AsPillar direction(Direction v) {
            this.direction = v;
            return this;
        }

        public AsPillar blockState(Block v) {
            return blockState(BlockStateProvider.simple(v.defaultBlockState()));
        }

        public AsPillar blockState(BlockState v) {
            return blockState(BlockStateProvider.simple(v));
        }

        public AsPillar blockState(BlockStateProvider v) {
            this.stateProvider = v;
            return this;
        }

        public AsPillar height(int v) {
            this.height = ConstantInt.of(v);
            return this;
        }

        public AsPillar height(IntProvider v) {
            this.height = v;
            return this;
        }


        @Override
        public PillarFeatureConfig createConfiguration() {
            if (stateProvider == null) {
                throw new IllegalStateException("A Pillar Features need a stateProvider");
            }
            if (height == null) {
                throw new IllegalStateException("A Pillar Features need a height");
            }
            return new PillarFeatureConfig(height, direction, allowedPlacement, stateProvider, transformer);
        }
    }

    public static class AsSequence extends BCLFeatureBuilder<SequenceFeature, SequenceFeatureConfig> {
        private final List<Holder<PlacedFeature>> features = new LinkedList<>();

        private AsSequence(ResourceLocation featureID, SequenceFeature feature) {
            super(featureID, feature);
        }


        public AsSequence add(org.betterx.bclib.api.v3.levelgen.features.BCLFeature p) {
            return add(p.placedFeature);
        }

        public AsSequence add(Holder<PlacedFeature> p) {
            features.add(p);
            return this;
        }

        @Override
        public SequenceFeatureConfig createConfiguration() {
            return new SequenceFeatureConfig(features);
        }
    }

    public static class AsBlockColumn<FF extends Feature<BlockColumnConfiguration>> extends BCLFeatureBuilder<FF, BlockColumnConfiguration> {
        private final List<BlockColumnConfiguration.Layer> layers = new LinkedList<>();
        private Direction direction = Direction.UP;
        private BlockPredicate allowedPlacement = BlockPredicate.ONLY_IN_AIR_PREDICATE;
        private boolean prioritizeTip = false;

        private AsBlockColumn(ResourceLocation featureID, FF feature) {
            super(featureID, feature);
        }

        public AsBlockColumn<FF> add(int height, Block block) {
            return add(ConstantInt.of(height), BlockStateProvider.simple(block));
        }

        public AsBlockColumn<FF> add(int height, BlockState state) {
            return add(ConstantInt.of(height), BlockStateProvider.simple(state));
        }

        public AsBlockColumn<FF> add(int height, BlockStateProvider state) {
            return add(ConstantInt.of(height), state);
        }

        public AsBlockColumn<FF> add(IntProvider height, Block block) {
            return add(height, BlockStateProvider.simple(block));
        }

        public AsBlockColumn<FF> add(IntProvider height, BlockState state) {
            return add(height, BlockStateProvider.simple(state));
        }

        public AsBlockColumn<FF> add(IntProvider height, BlockStateProvider state) {
            layers.add(new BlockColumnConfiguration.Layer(height, state));
            return this;
        }

        public AsBlockColumn<FF> addTripleShape(BlockState state, IntProvider midHeight) {
            return this
                    .add(1, state.setValue(BlockProperties.TRIPLE_SHAPE, BlockProperties.TripleShape.BOTTOM))
                    .add(midHeight, state.setValue(BlockProperties.TRIPLE_SHAPE, BlockProperties.TripleShape.MIDDLE))
                    .add(1, state.setValue(BlockProperties.TRIPLE_SHAPE, BlockProperties.TripleShape.TOP));
        }

        public AsBlockColumn<FF> addTripleShapeUpsideDown(BlockState state, IntProvider midHeight) {
            return this
                    .add(1, state.setValue(BlockProperties.TRIPLE_SHAPE, BlockProperties.TripleShape.TOP))
                    .add(midHeight, state.setValue(BlockProperties.TRIPLE_SHAPE, BlockProperties.TripleShape.MIDDLE))
                    .add(1, state.setValue(BlockProperties.TRIPLE_SHAPE, BlockProperties.TripleShape.BOTTOM));
        }

        public AsBlockColumn<FF> addBottomShapeUpsideDown(BlockState state, IntProvider midHeight) {
            return this
                    .add(midHeight, state.setValue(BlockProperties.BOTTOM, false))
                    .add(1, state.setValue(BlockProperties.BOTTOM, true));
        }

        public AsBlockColumn<FF> addBottomShape(BlockState state, IntProvider midHeight) {
            return this
                    .add(1, state.setValue(BlockProperties.BOTTOM, true))
                    .add(midHeight, state.setValue(BlockProperties.BOTTOM, false));
        }

        public AsBlockColumn<FF> direction(Direction dir) {
            direction = dir;
            return this;
        }

        public AsBlockColumn<FF> prioritizeTip() {
            return this.prioritizeTip(true);
        }

        public AsBlockColumn<FF> prioritizeTip(boolean v) {
            prioritizeTip = v;
            return this;
        }

        public AsBlockColumn<FF> allowedPlacement(BlockPredicate v) {
            allowedPlacement = v;
            return this;
        }

        @Override
        public BlockColumnConfiguration createConfiguration() {
            return new BlockColumnConfiguration(layers, direction, allowedPlacement, prioritizeTip);
        }
    }

    public static class WithTemplates extends BCLFeatureBuilder<TemplateFeature<TemplateFeatureConfig>, TemplateFeatureConfig> {
        private final List<StructureWorldNBT> templates = new LinkedList<>();

        private WithTemplates(ResourceLocation featureID, TemplateFeature<TemplateFeatureConfig> feature) {
            super(featureID, feature);
        }

        public WithTemplates add(
                ResourceLocation location,
                int offsetY,
                StructurePlacementType type,
                float chance
        ) {
            templates.add(TemplateFeatureConfig.cfg(location, offsetY, type, chance));
            return this;
        }

        @Override
        public TemplateFeatureConfig createConfiguration() {
            return new TemplateFeatureConfig(templates);
        }
    }

    public static class NetherForrestVegetation extends BCLFeatureBuilder<NetherForestVegetationFeature, NetherForestVegetationConfig> {
        private SimpleWeightedRandomList.Builder<BlockState> blocks;
        private WeightedStateProvider stateProvider;
        private int spreadWidth = 8;
        private int spreadHeight = 4;

        private NetherForrestVegetation(ResourceLocation featureID, NetherForestVegetationFeature feature) {
            super(featureID, feature);
        }

        public NetherForrestVegetation spreadWidth(int v) {
            spreadWidth = v;
            return this;
        }

        public NetherForrestVegetation spreadHeight(int v) {
            spreadHeight = v;
            return this;
        }

        public NetherForrestVegetation addAllStates(Block block, int weight) {
            Set<BlockState> states = BCLPoiType.getBlockStates(block);
            states.forEach(s -> add(block.defaultBlockState(), Math.max(1, weight / states.size())));
            return this;
        }

        public NetherForrestVegetation addAllStatesFor(IntegerProperty prop, Block block, int weight) {
            Collection<Integer> values = prop.getPossibleValues();
            values.forEach(s -> add(block.defaultBlockState().setValue(prop, s), Math.max(1, weight / values.size())));
            return this;
        }

        public NetherForrestVegetation add(Block block, int weight) {
            return add(block.defaultBlockState(), weight);
        }

        public NetherForrestVegetation add(BlockState state, int weight) {
            if (stateProvider != null) {
                throw new IllegalStateException("You can not add new state once a WeightedStateProvider was built. (" + state + ", " + weight + ")");
            }
            if (blocks == null) {
                blocks = SimpleWeightedRandomList.builder();
            }
            blocks.add(state, weight);
            return this;
        }

        public NetherForrestVegetation provider(WeightedStateProvider provider) {
            if (blocks != null) {
                throw new IllegalStateException(
                        "You can not set a WeightedStateProvider after states were added manually.");
            }
            stateProvider = provider;
            return this;
        }

        @Override
        public NetherForestVegetationConfig createConfiguration() {
            if (stateProvider == null && blocks == null) {
                throw new IllegalStateException("NetherForestVegetationConfig needs at least one BlockState");
            }
            if (stateProvider == null) stateProvider = new WeightedStateProvider(blocks.build());
            return new NetherForestVegetationConfig(stateProvider, spreadWidth, spreadHeight);
        }
    }

    public static class RandomPatch extends BCLFeatureBuilder<RandomPatchFeature, RandomPatchConfiguration> {
        private final Holder<PlacedFeature> featureToPlace;
        private int tries = 96;
        private int xzSpread = 7;
        private int ySpread = 3;

        private RandomPatch(
                @NotNull ResourceLocation featureID,
                @NotNull RandomPatchFeature feature,
                @NotNull Holder<PlacedFeature> featureToPlace
        ) {
            super(featureID, feature);
            this.featureToPlace = featureToPlace;
        }

        public RandomPatch likeDefaultNetherVegetation() {
            return likeDefaultNetherVegetation(8, 4);
        }

        public RandomPatch likeDefaultNetherVegetation(int xzSpread, int ySpread) {
            this.xzSpread = xzSpread;
            this.ySpread = ySpread;
            tries = xzSpread * xzSpread;
            return this;
        }

        public RandomPatch tries(int v) {
            tries = v;
            return this;
        }

        public RandomPatch spreadXZ(int v) {
            xzSpread = v;
            return this;
        }

        public RandomPatch spreadY(int v) {
            ySpread = v;
            return this;
        }


        @Override
        public RandomPatchConfiguration createConfiguration() {
            return new RandomPatchConfiguration(tries, xzSpread, ySpread, featureToPlace);
        }
    }

    public static class WithConfiguration<F extends Feature<FC>, FC extends FeatureConfiguration> extends BCLFeatureBuilder<F, FC> {
        private FC configuration;

        private WithConfiguration(@NotNull ResourceLocation featureID, @NotNull F feature) {
            super(featureID, feature);
        }

        public WithConfiguration configuration(FC config) {
            this.configuration = config;
            return this;
        }


        @Override
        public FC createConfiguration() {
            if (configuration == null) return (FC) NoneFeatureConfiguration.NONE;
            return configuration;
        }
    }

    public static class FacingBlock extends BCLFeatureBuilder<PlaceBlockFeature<PlaceFacingBlockConfig>, PlaceFacingBlockConfig> {
        private SimpleWeightedRandomList.Builder<BlockState> stateBuilder = SimpleWeightedRandomList.builder();
        BlockState firstState;
        private int count = 0;
        private List<Direction> directions = PlaceFacingBlockConfig.HORIZONTAL;

        private FacingBlock(ResourceLocation featureID, PlaceBlockFeature<PlaceFacingBlockConfig> feature) {
            super(featureID, feature);
        }


        public FacingBlock allHorizontal() {
            directions = PlaceFacingBlockConfig.HORIZONTAL;
            return this;
        }

        public FacingBlock allVertical() {
            directions = PlaceFacingBlockConfig.VERTICAL;
            return this;
        }

        public FacingBlock allDirections() {
            directions = PlaceFacingBlockConfig.ALL;
            return this;
        }

        public FacingBlock add(Block block) {
            return add(block, 1);
        }

        public FacingBlock add(BlockState state) {
            return this.add(state, 1);
        }

        public FacingBlock add(Block block, int weight) {
            return add(block.defaultBlockState(), weight);
        }

        public FacingBlock add(BlockState state, int weight) {
            if (firstState == null) firstState = state;
            count++;
            stateBuilder.add(state, weight);
            return this;
        }

        public FacingBlock addAllStates(Block block, int weight) {
            Set<BlockState> states = BCLPoiType.getBlockStates(block);
            states.forEach(s -> add(block.defaultBlockState(), Math.max(1, weight / states.size())));
            return this;
        }

        public FacingBlock addAllStatesFor(IntegerProperty prop, Block block, int weight) {
            Collection<Integer> values = prop.getPossibleValues();
            values.forEach(s -> add(block.defaultBlockState().setValue(prop, s), Math.max(1, weight / values.size())));
            return this;
        }


        @Override
        public PlaceFacingBlockConfig createConfiguration() {
            BlockStateProvider provider = null;
            if (count == 1) {
                provider = SimpleStateProvider.simple(firstState);
            } else {
                SimpleWeightedRandomList<BlockState> list = stateBuilder.build();
                if (!list.isEmpty()) {
                    provider = new WeightedStateProvider(list);
                }
            }

            if (provider == null) {
                throw new IllegalStateException("Facing Blocks need a State Provider.");
            }
            return new PlaceFacingBlockConfig(provider, directions);
        }
    }

    public static class ForSimpleBlock extends BCLFeatureBuilder<SimpleBlockFeature, SimpleBlockConfiguration> {
        private final BlockStateProvider provider;

        private ForSimpleBlock(
                @NotNull ResourceLocation featureID,
                @NotNull SimpleBlockFeature feature,
                @NotNull BlockStateProvider provider
        ) {
            super(featureID, feature);
            this.provider = provider;
        }

        @Override
        public SimpleBlockConfiguration createConfiguration() {
            return new SimpleBlockConfiguration(provider);
        }
    }

    public static class WeightedBlock extends BCLFeatureBuilder<SimpleBlockFeature, SimpleBlockConfiguration> {
        SimpleWeightedRandomList.Builder<BlockState> stateBuilder = SimpleWeightedRandomList.builder();

        private WeightedBlock(
                @NotNull ResourceLocation featureID,
                @NotNull SimpleBlockFeature feature
        ) {
            super(featureID, feature);
        }

        public WeightedBlock add(Block block, int weight) {
            return add(block.defaultBlockState(), weight);
        }

        public WeightedBlock add(BlockState state, int weight) {
            stateBuilder.add(state, weight);
            return this;
        }

        public WeightedBlock addAllStates(Block block, int weight) {
            Set<BlockState> states = BCLPoiType.getBlockStates(block);
            states.forEach(s -> add(block.defaultBlockState(), Math.max(1, weight / states.size())));
            return this;
        }

        public WeightedBlock addAllStatesFor(IntegerProperty prop, Block block, int weight) {
            Collection<Integer> values = prop.getPossibleValues();
            values.forEach(s -> add(block.defaultBlockState().setValue(prop, s), Math.max(1, weight / values.size())));
            return this;
        }

        @Override
        public SimpleBlockConfiguration createConfiguration() {
            return new SimpleBlockConfiguration(new WeightedStateProvider(stateBuilder.build()));
        }
    }
}


