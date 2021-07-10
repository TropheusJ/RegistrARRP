//package com.tterrag.registrarrp.providers;
//
//import java.util.Optional;
//
//import com.tterrag.registrarrp.AbstractRegistrate;
//
//import net.fabricmc.api.EnvType;
//import net.minecraft.block.Block;
//import net.minecraft.data.DataGenerator;
//import net.minecraft.world.gen.stateprovider.BlockStateProvider;
//
//public class RegistrateBlockstateProvider extends BlockStateProvider implements RegistrateProvider {
//
//    private final AbstractRegistrate<?> parent;
//
//    public RegistrateBlockstateProvider(AbstractRegistrate<?> parent, DataGenerator gen, ExistingFileHelper exFileHelper) {
//        super(gen, parent.getModid(), exFileHelper);
//        this.parent = parent;
//    }
//
//    @Override
//    public EnvType getSide() {
//        return EnvType.CLIENT;
//    }
//
//    @Override
//    protected void registerStatesAndModels() {
//        parent.genData(ProviderType.BLOCKSTATE, this);
//    }
//
//    @Override
//    public String getName() {
//        return "Blockstates";
//    }
//
//    ExistingFileHelper getExistingFileHelper() {
//        return this.models().existingFileHelper;
//    }
//
//    @SuppressWarnings("null")
//    public Optional<VariantBlockStateBuilder> getExistingVariantBuilder(Block block) {
//        return Optional.ofNullable(registeredBlocks.get(block))
//                .filter(b -> b instanceof VariantBlockStateBuilder)
//                .map(b -> (VariantBlockStateBuilder) b);
//    }
//
//    @SuppressWarnings("null")
//    public Optional<MultiPartBlockStateBuilder> getExistingMultipartBuilder(Block block) {
//        return Optional.ofNullable(registeredBlocks.get(block))
//                .filter(b -> b instanceof MultiPartBlockStateBuilder)
//                .map(b -> (MultiPartBlockStateBuilder) b);
//    }
//}
