package com.tterrag.registrarrp.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tterrag.registrarrp.fabric.RegistrARRP;
import com.tterrag.registrarrp.mixin.AbstractBlock$SettingsAccessor;
import com.tterrag.registrarrp.util.CommonLootTableTypes;
import com.tterrag.registrarrp.util.CookingRecipeTypes;
import com.tterrag.registrarrp.util.nullness.*;
import net.devtech.arrp.json.blockstate.JState;
import net.devtech.arrp.json.blockstate.JVariant;
import net.devtech.arrp.json.loot.JEntry;
import net.devtech.arrp.json.loot.JLootTable;
import net.devtech.arrp.json.loot.JPool;
import net.devtech.arrp.json.models.JFaces;
import net.devtech.arrp.json.models.JModel;
import net.devtech.arrp.json.recipe.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.BlockItem;
import net.minecraft.tag.Tag.Identified;
import net.minecraft.util.DyeColor;

import com.tterrag.registrarrp.AbstractRegistrate;
import com.tterrag.registrarrp.fabric.EnvExecutor;
import com.tterrag.registrarrp.fabric.RegistryObject;
import com.tterrag.registrarrp.util.entry.BlockEntry;
import com.tterrag.registrarrp.util.entry.RegistryEntry;

import static net.devtech.arrp.json.loot.JLootTable.condition;
import static net.devtech.arrp.json.loot.JLootTable.predicate;
import static net.devtech.arrp.json.models.JModel.face;

/**
 * A builder for blocks, allows for customization of the {@link FabricBlockSettings}, creation of block items, and configuration of data associated with blocks (loot tables, recipes, etc.).
 * 
 * @param <T>
 *            The type of block being built
 * @param <P>
 *            Parent object type
 */
public class BlockBuilder<T extends Block, P> extends AbstractBuilder<Block, T, P, BlockBuilder<T, P>> {

    /**
     * Create a new {@link BlockBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
     * <p>
     * The block will be assigned the following data:
     * <ul>
     * <li>A default blockstate file mapping all states to one model (via {@link #defaultBlockstate()})</li>
     * <li>A simple cube_all model (used in the blockstate) with one texture (via {@link #defaultBlockstate()})</li>
     * <li>A self-dropping loot table (via {@link #defaultLoot()})</li>
     * <li>The default translation (via {@link #defaultLang()})</li>
     * </ul>
     * 
     * @param <T>
     *            The type of the builder
     * @param <P>
     *            Parent object type
     * @param owner
     *            The owning {@link AbstractRegistrate} object
     * @param parent
     *            The parent object
     * @param name
     *            Name of the entry being built
     * @param callback
     *            A callback used to actually register the built entry
     * @param factory
     *            Factory to create the block
     * @param material
     *            The {@link Material} to use for the initial {@link FabricBlockSettings} object
     * @return A new {@link BlockBuilder} with reasonable default data generators.
     */
    public static <T extends Block, P> BlockBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<FabricBlockSettings, T> factory, Material material) {
        return new BlockBuilder<>(owner, parent, name, callback, factory, () -> FabricBlockSettings.of(material))
                /*.defaultBlockstate().defaultLoot().defaultLang()*/;
    }

    private final NonNullFunction<FabricBlockSettings, T> factory;
    
    private NonNullSupplier<FabricBlockSettings> initialProperties;
    private NonNullFunction<FabricBlockSettings, FabricBlockSettings> propertiesCallback = NonNullUnaryOperator.identity();
    private List<Supplier<Supplier<RenderLayer>>> renderLayers = new ArrayList<>(1);
    
    @Nullable
    private NonNullSupplier<Supplier<BlockColorProvider>> colorHandler;

    protected BlockBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<FabricBlockSettings, T> factory, NonNullSupplier<FabricBlockSettings> initialProperties) {
        super(owner, parent, name, callback, Block.class);
        this.factory = factory;
        this.initialProperties = initialProperties;
    }

    /**
     * Modify the properties of the block. Modifications are done lazily, but the passed function is composed with the current one, and as such this method can be called multiple times to perform
     * different operations.
     * <p>
     * If a different properties instance is returned, it will replace the existing one entirely.
     * 
     * @param func
     *            The action to perform on the properties
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> properties(NonNullUnaryOperator<FabricBlockSettings> func) {
        propertiesCallback = propertiesCallback.andThen(func);
        return this;
    }

    /**
     * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
     *
     * @param material
     *            The material of the initial properties
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> initialProperties(Material material) {
        initialProperties = () -> FabricBlockSettings.of(material);
        return this;
    }

    /**
     * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
     * 
     * @param material
     *            The material of the initial properties
     * @param color
     *            The color of the intial properties
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> initialProperties(Material material, DyeColor color) {
        initialProperties = () -> FabricBlockSettings.of(material, color);
        return this;
    }

    /**
     * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
     * 
     * @param material
     *            The material of the initial properties
     * @param color
     *            The color of the intial properties
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> initialProperties(Material material, MaterialColor color) {
        initialProperties = () -> FabricBlockSettings.of(material, color);
        return this;
    }

    /**
     * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
     * 
     * @param block
     *            The block to create the initial properties from (via {@link FabricBlockSettings#copy(AbstractBlock)})
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> initialProperties(NonNullSupplier<? extends Block> block) {
        initialProperties = () -> FabricBlockSettings.copyOf(block.get());
        return this;
    }

    public BlockBuilder<T, P> addLayer(Supplier<Supplier<RenderLayer>> layer) {
        EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
            Preconditions.checkArgument(RenderLayer.getBlockLayers().contains(layer.get().get()), "Invalid block layer: " + layer);
        });
        if (this.renderLayers.isEmpty()) {
            onRegister(this::registerLayers);
        }
        this.renderLayers.add(layer);
        return this;
    }

    protected void registerLayers(T entry) {
        EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
//            if (renderLayers.size() == 1) {
                final RenderLayer layer = renderLayers.get(0).get().get();
                BlockRenderLayerMap.INSTANCE.putBlock(entry, layer);
//            } else if (renderLayers.size() > 1) {
//                final Set<RenderLayer> layers = renderLayers.stream()
//                        .map(s -> s.get().get())
//                        .collect(Collectors.toSet());
//                RenderLayers.setRenderLayer(entry, layers::contains);
//            }
        });
    }

    /**
     * Create a standard {@link BlockItem} for this block, building it immediately, and not allowing for further configuration.
     * <p>
     * The item will have no lang entry (since it would duplicate the block's) and a simple block item model (via {@link RegistrateItemModelProvider#blockItem(NonNullSupplier)}).
     *
     * @return this {@link BlockBuilder}
     * @see #item()
     */
    public BlockBuilder<T, P> simpleItem() {
        return item().build();
    }

    /**
     * Create a standard {@link BlockItem} for this block, and return the builder for it so that further customization can be done.
     * <p>
     * The item will have no lang entry (since it would duplicate the block's) and a simple block item model (via {@link RegistrateItemModelProvider#blockItem(NonNullSupplier)}).
     * 
     * @return the {@link ItemBuilder} for the {@link BlockItem}
     */
    public ItemBuilder<BlockItem, BlockBuilder<T, P>> item() {
        return item(BlockItem::new);
    }

    /**
     * Create a {@link BlockItem} for this block, which is created by the given factory, and return the builder for it so that further customization can be done.
     * <p>
     * By default, the item will have no lang entry (since it would duplicate the block's) and a simple block item model (via {@link RegistrateItemModelProvider#blockItem(NonNullSupplier)}).
     * 
     * @param <I>
     *            The type of the item
     * @param factory
     *            A factory for the item, which accepts the block object and properties and returns a new item
     * @return the {@link ItemBuilder} for the {@link BlockItem}
     */
    public <I extends BlockItem> ItemBuilder<I, BlockBuilder<T, P>> item(NonNullBiFunction<? super T, FabricItemSettings, ? extends I> factory) {
        return getOwner().<I, BlockBuilder<T, P>> item(this, getName(), p -> factory.apply(getEntry(), p))
                /*.setData(ProviderType.LANG, NonNullBiConsumer.noop()) // FIXME Need a better API for "unsetting" providers
                .model((ctx, prov) -> {
                    Optional<String> model = getOwner().getDataProvider(ProviderType.BLOCKSTATE)
                            .flatMap(p -> p.getExistingVariantBuilder(getEntry()))
                            .map(b -> b.getModels().get(b.partialState()))
                            .map(ConfiguredModelList::toJSON)
                            .filter(JsonElement::isJsonObject)
                            .map(j -> j.getAsJsonObject().get("model"))
                            .map(JsonElement::getAsString);
                    if (model.isPresent()) {
                        prov.withExistingParent(ctx.getName(), model.get());
                    } else {
                        prov.blockItem(asSupplier());
                    }
                })*/;
    }
    
    /**
     * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
     * 
     * @param <TE>
     *            The type of the tile entity
     * @param factory
     *            A factory for the tile entity
     * @return this {@link BlockBuilder}
     * @deprecated Use {@link #simpleTileEntity(NonNullFunction)}
     */
    @Deprecated
    public <TE extends BlockEntity> BlockBuilder<T, P> simpleTileEntity(NonNullSupplier<? extends TE> factory) {
        return tileEntity(factory).build();
    }

    /**
     * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
     * 
     * @param <TE>
     *            The type of the tile entity
     * @param factory
     *            A factory for the tile entity
     * @return this {@link BlockBuilder}
     */
    public <TE extends BlockEntity> BlockBuilder<T, P> simpleTileEntity(NonNullFunction<BlockEntityType<TE>, ? extends TE> factory) {
        return tileEntity(factory).build();
    }

    /**
     * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
     * <p>
     * The created {@link TileEntityBuilder} is returned for further configuration.
     * 
     * @param <TE>
     *            The type of the tile entity
     * @param factory
     *            A factory for the tile entity
     * @return the {@link TileEntityBuilder}
     * @deprecated Use {@link #tileEntity(NonNullFunction)}
     */
    @Deprecated
    public <TE extends BlockEntity> TileEntityBuilder<TE, BlockBuilder<T, P>> tileEntity(NonNullSupplier<? extends TE> factory) {
        return getOwner().<TE, BlockBuilder<T, P>> tileEntity(this, getName(), factory).validBlock(asSupplier());
    }

    /**
     * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
     * <p>
     * The created {@link TileEntityBuilder} is returned for further configuration.
     * 
     * @param <TE>
     *            The type of the tile entity
     * @param factory
     *            A factory for the tile entity
     * @return the {@link TileEntityBuilder}
     */
    public <TE extends BlockEntity> TileEntityBuilder<TE, BlockBuilder<T, P>> tileEntity(NonNullFunction<BlockEntityType<TE>, ? extends TE> factory) {
        return getOwner().<TE, BlockBuilder<T, P>> tileEntity(this, getName(), factory).validBlock(asSupplier());
    }
    
    /**
     * Register a block color handler for this block. The {@link BlockColorProvider} instance can be shared across many blocks.
     * 
     * @param colorHandler
     *            The color handler to register for this block
     * @return this {@link BlockBuilder}
     */
    // TODO it might be worthwhile to abstract this more and add the capability to automatically copy to the item
    public BlockBuilder<T, P> color(NonNullSupplier<Supplier<BlockColorProvider>> colorHandler) {
        if (this.colorHandler == null) {
            EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::registerBlockColor);
        }
        this.colorHandler = colorHandler;
        return this;
    }
    
    protected void registerBlockColor() {
        onRegister(entry -> {
            ColorProviderRegistry.BLOCK.register(colorHandler.get().get(), entry);
        });
    }

    /**
     * Assign the default blockstate, which maps all states to a single model.
     * 
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> defaultBlockstate() {
        JModel model = JModel.model().textures(JModel.textures().var("all", "block/" + getName()).particle("block/" + getName()))
                .element(JModel.element().from(0, 0, 0).to(16, 16, 16)
                        .faces(JModel.faces()
                                .up(JModel.face("all").uv(0, 0, 16, 16))
                                .down(JModel.face("all").uv(0, 0, 16, 16))
                                .north(JModel.face("all").uv(0, 0, 16, 16))
                                .south(JModel.face("all").uv(0, 0, 16, 16))
                                .east(JModel.face("all").uv(0, 0, 16, 16))
                                .west(JModel.face("all").uv(0, 0, 16, 16))));
        getOwner().getResourcePack().addModel(model, new Identifier(getOwner().getModid(), getName()));
        return blockstate(new Identifier(getOwner().getModid(), getName()),
                JState.state().add(JState.variant(JState.model(new Identifier(getOwner().getModid(), getName())))));
    }

    /**
     * Configure the blockstate/models for this block.
     * 
     * @param stateID The Identifier for the BlockState, should typically be equal to {@code new Identifier(getOwner().getModid(), getName())}, but may be changed to match the state.
     * @param state The BlockState to give this block, in the form of a raw {@link JState} object.
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> blockstate(Identifier stateID, JState state) {
        getOwner().getResourcePack().addBlockState(state, stateID);
        return this;
    }

    /**
     * Assign the default loot table. Block will drop itself.
     * 
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> defaultLoot() {
        return loot(JLootTable.loot("minecraft:block")
                .pool(JLootTable.pool()
                        .rolls(1)
                        .entry(JLootTable.entry()
                                .type("minecraft:item")
                                .name(getIdentifierString()))
                        .condition(JLootTable.predicate("minecraft:survives_explosion"))));
    }
    
    // credit to https://github.com/Azagwen/ATBYW/ for these 2 helper methods
    private static JsonObject silkTouchPredicate() {
        JsonObject level = new JsonObject();
        level.addProperty("min", 1);
    
        JsonObject silkTouch = new JsonObject();
        silkTouch.addProperty("enchantment", "minecraft:silk_touch");
        silkTouch.add("levels", level);
        
        JsonArray enchantments = new JsonArray();
        enchantments.add(silkTouch);
    
        JsonObject predicate = new JsonObject();
        predicate.add("enchantments", enchantments);
        
        return predicate;
    }
    
    private static JsonObject blockStringProperty(String name, String value) {
        JsonObject property = new JsonObject();
        property.addProperty(name, value);
        
        return property;
    }
    
    /**
     * Assign a common loot table to this block, determined by the {@link CommonLootTableTypes} fed in.
     *
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> simpleLoot(CommonLootTableTypes type) {
        if (type == CommonLootTableTypes.NEVER) {
            properties(FabricBlockSettings::dropsNothing);
            return this;
        }
    
        // credit to https://github.com/Azagwen/ATBYW/ for these 2
        if (type == CommonLootTableTypes.SLAB) {
            return loot(JLootTable.loot("minecraft:block")
                    .pool(JLootTable.pool()
                            .rolls(1)
                            .entry(JLootTable.entry()
                                    .type("minecraft:item")
                                    .function(JLootTable.function("minecraft:set_count")
                                            .condition(predicate("minecraft:block_state_property")
                                                    .parameter("block", getIdentifierString())
                                                    .parameter("properties", blockStringProperty("type", "double")))
                                            .parameter("count", 2))
                                    .function("minecraft:explosion_decay")
                                    .name(getIdentifierString()))
                            .condition(predicate("minecraft:survives_explosion"))));
        }
        
        if (type == CommonLootTableTypes.SILK_TOUCH_REQUIRED) {
            return loot(JLootTable.loot("minecraft:block")
                    .pool(JLootTable.pool()
                            .rolls(1)
                            .entry(JLootTable.entry()
                                    .type("minecraft:item")
                                    .name(getIdentifierString()))
                            .condition(JLootTable.predicate("minecraft:match_tool")
                                    .parameter("predicate", silkTouchPredicate()))));
        }
    
        throw new RuntimeException("Attempted to use a CommonLootTableTypes with no behavior, report this as an issue!");
    }

    /**
     * Configure the loot table for this block.
     * <p>
     * If the block does not have a loot table (i.e. {@link FabricBlockSettings#dropsNothing()} is called) this action will be <em>skipped</em>.
     *
     * @param table The loot table for this block, in the form of a raw {@link JLootTable} object.
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> loot(JLootTable table) {
        if (((AbstractBlock$SettingsAccessor) initialProperties.get()).getLootTableId() != LootTables.EMPTY) {
            getOwner().getResourcePack().addLootTable(new Identifier(getOwner().getModid(), "blocks/" + getName()), table);
        }
        return this;
    }
    
//    /**
//     * Assign {@link Identified}{@code s} to this block. Multiple calls will add additional tags.
//     *
//     * @param tags
//     *            The tags to assign
//     * @return this {@link BlockBuilder}
//     */
//    @SafeVarargs
//    public final BlockBuilder<T, P> tag(Identified<Block>... tags) {
//        return tag(ProviderType.BLOCK_TAGS, tags);
//    }
    
    // ------- RECIPES -------
    
    /**
     * Create a simple stonecutting recipe.
     *
     * @param input The input item
     * @param outputCount The number of items in the output stack
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> quickStonecutting(Item input, int outputCount) {
        return recipe("stonecutting", JRecipe.stonecutting(JIngredient.ingredient().item(input), JResult.stackedResult(getIdentifierString(), outputCount)));
    }
    
    /**
     * Create a simple smithing recipe.
     *
     * @param base The base item, such as a diamond tool.
     * @param addition The addition item, such as a Netherite ingot.
     * @param outputCount The number of items in the output stack.
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> quickSmithing(Item base, Item addition, int outputCount) {
        return recipe("smithing", JRecipe.smithing(JIngredient.ingredient().item(base), JIngredient.ingredient().item(addition), JResult.stackedResult(getIdentifierString(), outputCount)));
    }
    
    /**
     * Create a simple cooking recipe.
     *
     * @param input The input for this cooking recipe
     * @param outputCount The number of items in the output stack
     * @param type The type of cooking recipe, such as smelting or blasting.
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> quickCooking(Item input, int outputCount, CookingRecipeTypes type) {
        if (type == CookingRecipeTypes.SMELTING) return recipe("smelting", JRecipe.smelting(JIngredient.ingredient().item(input), JResult.stackedResult(getIdentifierString(), outputCount)));
        if (type == CookingRecipeTypes.BLASTING) return recipe("blasting", JRecipe.blasting(JIngredient.ingredient().item(input), JResult.stackedResult(getIdentifierString(), outputCount)));
        if (type == CookingRecipeTypes.SMOKING) return recipe("smoking", JRecipe.smoking(JIngredient.ingredient().item(input), JResult.stackedResult(getIdentifierString(), outputCount)));
        if (type == CookingRecipeTypes.CAMPFIRE) return recipe("campfire", JRecipe.campfire(JIngredient.ingredient().item(input), JResult.stackedResult(getIdentifierString(), outputCount)));
        throw new RuntimeException("Unknown cooking recipe type. Report this!");
    }
    
    /**
     * Create a simple shapeless recipe.
     *
     * @param outputCount The number of items in the output stack
     * @param ingredients Pairs of items and the number of times they appear in the recipe.
     * @return this {@link BlockBuilder}
     */
    @SafeVarargs
    public final BlockBuilder<T, P> quickShapeless(int outputCount, Pair<Item, Integer>... ingredients) {
        JIngredients jIngredients = JIngredients.ingredients();
        for (Pair<Item, Integer> pair : ingredients) {
            for (int i = 0; i < pair.getRight(); i++) {
                jIngredients.add(JIngredient.ingredient().item(pair.getLeft()));
            }
        }
        return recipe("shapeless", JRecipe.shapeless(jIngredients, JResult.stackedResult(getIdentifierString(), outputCount)));
    }
    
    /**
     * Create a simple shaped recipe.
     * <br>
     * --- Example usage --- <br>
     * goal recipe:<br>
     * "XXX"<br>
     * "XYX"<br>
     * "XXX"<br>
     * X: Netherite scrap<br>
     * Y: Mossy cobblestone<br>
     * output: Iron shovel<br>
     * <br>
     * correct usage:<br>
     * {@code quickShaped(1, "XXX", "XYX", "XXX", new Pair("X", Items.NETHERITE_SCRAP), new Pair("Y", Items.MOSSY_COBBLESTONE))}
     *
     * @param outputCount The number of items in the output stack
     * @param row1 The first row of the shaped recipe
     * @param row2 The second row of the shaped recipe
     * @param row3 The third row of the shaped recipe
     * @param keys Pairs containing the keys to the items in the recipe.
     * @return this {@link BlockBuilder}
     */
    @SafeVarargs
    public final BlockBuilder<T, P> quickShaped(int outputCount, String row1, String row2, String row3, Pair<String, Item>... keys) {
        JKeys jKeys = JKeys.keys();
        for (Pair<String, Item> pair : keys) {
            jKeys.key(pair.getLeft(), JIngredient.ingredient().item(pair.getRight()));
        }
        return recipe("shaped", JRecipe.shaped(JPattern.pattern(row1, row2, row3), jKeys, JResult.stackedResult(getIdentifierString(), outputCount)));
    }
    
    /**
     * Configure the recipe(s) for this block.
     * If the recipeID is null, one will be generated.
     *
     * @param recipeType The name of the added recipe, such as "stonecutting" or "shaped"
     * @param recipe The recipe to add, in the form of a raw {@link JRecipe} object.
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> recipe(@Nullable String recipeType, JRecipe recipe) {
        getOwner().addRecipe(recipeType, recipe);
        return this;
    }
    
    @Override
    protected T createEntry() {
        @NotNull FabricBlockSettings properties = this.initialProperties.get();
        properties = propertiesCallback.apply(properties);
        return factory.apply(properties);
    }
    
    @Override
    protected RegistryEntry<T> createEntryWrapper(RegistryObject<T> delegate) {
        return new BlockEntry<>(getOwner(), delegate);
    }
    
    @Override
    public BlockEntry<T> register() {
        return (BlockEntry<T>) super.register();
    }
}
