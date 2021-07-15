package com.tterrag.registrarrp.builders;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tterrag.registrarrp.AbstractRegistrate;
import com.tterrag.registrarrp.fabric.EnvExecutor;
import com.tterrag.registrarrp.fabric.RegistryObject;
import com.tterrag.registrarrp.mixin.AbstractBlock$SettingsAccessor;
import com.tterrag.registrarrp.util.CommonLootTableTypes;
import com.tterrag.registrarrp.util.RecipeTypes;
import com.tterrag.registrarrp.util.entry.BlockEntry;
import com.tterrag.registrarrp.util.entry.RegistryEntry;
import com.tterrag.registrarrp.util.nullness.NonNullBiFunction;
import com.tterrag.registrarrp.util.nullness.NonNullFunction;
import com.tterrag.registrarrp.util.nullness.NonNullSupplier;
import com.tterrag.registrarrp.util.nullness.NonNullUnaryOperator;
import net.devtech.arrp.json.blockstate.JState;
import net.devtech.arrp.json.loot.JLootTable;
import net.devtech.arrp.json.models.JModel;
import net.devtech.arrp.json.models.JTextures;
import net.devtech.arrp.json.recipe.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.loot.LootTables;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static net.devtech.arrp.json.loot.JLootTable.predicate;

/**
 * A builder for blocks, allows for customization of the {@link FabricBlockSettings}, creation of block items, and configuration of data associated with blocks (loot tables, recipes, etc.).
 *
 * @param <T> The type of block being built
 * @param <P> Parent object type
 */
public class BlockBuilder<T extends Block, P> extends AbstractBuilder<Block, T, P, BlockBuilder<T, P>> {
	
	private final NonNullFunction<FabricBlockSettings, T> factory;
	private final List<Supplier<Supplier<RenderLayer>>> renderLayers = new ArrayList<>(1);
	private NonNullSupplier<FabricBlockSettings> initialProperties;
	private NonNullFunction<FabricBlockSettings, FabricBlockSettings> propertiesCallback = NonNullUnaryOperator.identity();
	@Nullable
	private NonNullSupplier<Supplier<BlockColorProvider>> colorHandler;
	@Nullable
	private JLootTable lootTable;
	private Pair<Identifier, JState> blockState;
	private final Map<Identifier, JModel> models = new HashMap<>();
	private final boolean wall = false;
	
	protected BlockBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<FabricBlockSettings, T> factory, NonNullSupplier<FabricBlockSettings> initialProperties) {
		super(owner, parent, name, callback, Block.class);
		this.factory = factory;
		this.initialProperties = initialProperties;
	}
	
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
	 * @param <T>      The type of the builder
	 * @param <P>      Parent object type
	 * @param owner    The owning {@link AbstractRegistrate} object
	 * @param parent   The parent object
	 * @param name     Name of the entry being built
	 * @param callback A callback used to actually register the built entry
	 * @param factory  Factory to create the block
	 * @param material The {@link Material} to use for the initial {@link FabricBlockSettings} object
	 * @return A new {@link BlockBuilder} with reasonable default data generators.
	 */
	public static <T extends Block, P> BlockBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<FabricBlockSettings, T> factory, Material material) {
		return new BlockBuilder<>(owner, parent, name, callback, factory, () -> FabricBlockSettings.of(material))
				.defaultBlockstate().defaultLoot().defaultLang();
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
	 * Modify the properties of the block. Modifications are done lazily, but the passed function is composed with the current one, and as such this method can be called multiple times to perform
	 * different operations.
	 * <p>
	 * If a different properties instance is returned, it will replace the existing one entirely.
	 *
	 * @param func The action to perform on the properties
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> properties(NonNullUnaryOperator<FabricBlockSettings> func) {
		propertiesCallback = propertiesCallback.andThen(func);
		return this;
	}
	
	/**
	 * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
	 *
	 * @param material The material of the initial properties
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> initialProperties(Material material) {
		initialProperties = () -> FabricBlockSettings.of(material);
		return this;
	}
	
	/**
	 * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
	 *
	 * @param material The material of the initial properties
	 * @param color    The color of the intial properties
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> initialProperties(Material material, DyeColor color) {
		initialProperties = () -> FabricBlockSettings.of(material, color);
		return this;
	}
	
	/**
	 * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
	 *
	 * @param material The material of the initial properties
	 * @param color    The color of the intial properties
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> initialProperties(Material material, MaterialColor color) {
		initialProperties = () -> FabricBlockSettings.of(material, color);
		return this;
	}
	
	/**
	 * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
	 *
	 * @param block The block to create the initial properties from (via {@link FabricBlockSettings#copy(AbstractBlock)})
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> initialProperties(NonNullSupplier<? extends Block> block) {
		initialProperties = () -> FabricBlockSettings.copyOf(block.get());
		return this;
	}
	
	public BlockBuilder<T, P> addLayer(Supplier<Supplier<RenderLayer>> layer) {
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () ->
				Preconditions.checkArgument(RenderLayer.getBlockLayers().contains(layer.get().get()), "Invalid block layer: " + layer));
		if (this.renderLayers.isEmpty()) {
			onRegister(this::registerLayers);
		}
		this.renderLayers.add(layer);
		return this;
	}
	
	protected void registerLayers(T entry) {
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
			final RenderLayer layer = renderLayers.get(0).get().get();
			BlockRenderLayerMap.INSTANCE.putBlock(entry, layer);
		});
	}
	
	/**
	 * Create a standard {@link BlockItem} for this block, building it immediately, and not allowing for further configuration.
	 * <p>
	 * The item will have no lang entry (since it would duplicate the block's) and a simple block item model. <br>
	 * If the block is a fence or a wall, {@link #simpleFenceItem()} will be used.
	 *
	 * @return this {@link BlockBuilder}
	 * @see #item()
	 */
	public BlockBuilder<T, P> simpleItem() {
		if (wall) {
			return simpleFenceItem();
		}
		return item().build();
	}
	
	/**
	 * Create a standard {@link BlockItem} for this block, building it immediately, and not allowing for further configuration.
	 * <p>
	 * The item will have no lang entry (since it would duplicate the block's) and a simple fence or wall block item model.
	 * Used for {@link #fenceModel(Identifier)} and {@link #wallModel(Identifier)}
	 *
	 * @return this {@link BlockBuilder}
	 * @see #item()
	 */
	protected BlockBuilder<T, P> simpleFenceItem() {
		return item().model(new Identifier(getOwner().getModid(), "item/" + getName()), JModel.model().parent(getOwner().getModid() + ":block/" + getName() + "_inventory")).build();
	}
	
	/**
	 * Create a standard {@link BlockItem} for this block, and return the builder for it so that further customization can be done.
	 * <p>
	 * The item will have no lang entry (since it would duplicate the block's) and a simple block item model.
	 *
	 * @return the {@link ItemBuilder} for the {@link BlockItem}
	 */
	public ItemBuilder<BlockItem, BlockBuilder<T, P>> item() {
		return item(BlockItem::new);
	}
	
	/**
	 * Create a {@link BlockItem} for this block, which is created by the given factory, and return the builder for it so that further customization can be done.
	 * <p>
	 * By default, the item will have no lang entry (since it would duplicate the block's) and a simple block item model.
	 *
	 * @param <I>     The type of the item
	 * @param factory A factory for the item, which accepts the block object and properties and returns a new item
	 * @return the {@link ItemBuilder} for the {@link BlockItem}
	 */
	public <I extends BlockItem> ItemBuilder<I, BlockBuilder<T, P>> item(NonNullBiFunction<? super T, FabricItemSettings, ? extends I> factory) {
		return getOwner().item(this, getName(), p -> factory.apply(getEntry(), p));
	}
	
	/**
	 * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
	 *
	 * @param <TE>    The type of the tile entity
	 * @param factory A factory for the tile entity
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
	 * @param <TE>    The type of the tile entity
	 * @param factory A factory for the tile entity
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
	 * @param <TE>    The type of the tile entity
	 * @param factory A factory for the tile entity
	 * @return the {@link TileEntityBuilder}
	 * @deprecated Use {@link #tileEntity(NonNullFunction)}
	 */
	@Deprecated
	public <TE extends BlockEntity> TileEntityBuilder<TE, BlockBuilder<T, P>> tileEntity(NonNullSupplier<? extends TE> factory) {
		return getOwner().<TE, BlockBuilder<T, P>>tileEntity(this, getName(), factory).validBlock(asSupplier());
	}
	
	/**
	 * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
	 * <p>
	 * The created {@link TileEntityBuilder} is returned for further configuration.
	 *
	 * @param <TE>    The type of the tile entity
	 * @param factory A factory for the tile entity
	 * @return the {@link TileEntityBuilder}
	 */
	public <TE extends BlockEntity> TileEntityBuilder<TE, BlockBuilder<T, P>> tileEntity(NonNullFunction<BlockEntityType<TE>, ? extends TE> factory) {
		return getOwner().tileEntity(this, getName(), factory).validBlock(asSupplier());
	}
	
	/**
	 * Register a block color handler for this block. The {@link BlockColorProvider} instance can be shared across many blocks.
	 *
	 * @param colorHandler The color handler to register for this block
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
		onRegister(entry -> ColorProviderRegistry.BLOCK.register(colorHandler.get().get(), entry));
	}
	
	/**
	 * Assign the default blockstate, which maps all states to a single model.  Is applied by default, calling manually should not be necessary.
	 *
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> defaultBlockstate() {
		return cubeModel(new Identifier(getOwner().getModid(), "block/" + getName()));
	}
	
	/**
	 * Give this block a simple cube model with one texture.
	 *
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> cubeModel(Identifier texture) {
		models.clear();
		JModel model = JModel.model()
				.parent("minecraft:block/cube_all")
				.textures(JModel.textures()
						.var("all", texture.toString()));
		models.put(new Identifier(getOwner().getModid(), "block/" + getName()), model);
		return blockstate(getIdentifier(),
				JState.state().add(JState.variant(JState.model(new Identifier(getOwner().getModid(), "block/" + getName())))));
	}
	
	/**
	 * Give this block a simple pillar model with two textures.
	 *
	 * @param sideTexture The Identifier for the texture for the sides of the pillar
	 * @param endsTexture The Identifier for the texture for the top and the bottom of the pillar
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> pillarModel(Identifier sideTexture, Identifier endsTexture) {
		models.clear();
		JModel model = JModel.model()
				.parent("minecraft:block/cube_column")
				.textures(JModel.textures()
						.var("end", endsTexture.toString())
						.var("side", sideTexture.toString()));
		models.put(new Identifier(getOwner().getModid(), "block/" + getName()), model);
		models.put(new Identifier(getOwner().getModid(), "block/" + getName() + "_horizontal"), model);
		return blockstate(getIdentifier(),
				JState.state().add(JState.variant()
						.put("axis", "x", JState.model(new Identifier(getOwner().getModid(), "block/" + getName() + "_horizontal")).x(90).y(90))
						.put("axis", "y", JState.model(new Identifier(getOwner().getModid(), "block/" + getName())))
						.put("axis", "z", JState.model(new Identifier(getOwner().getModid(), "block/" + getName() + "_horizontal")).x(90))));
	}
	
	/**
	 * Give this block a simple stairs model with one texture.
	 *
	 * @param texture The Identifier for the texture for this block
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> stairsModel(Identifier texture) {
		return stairsModel(texture, texture, texture);
	}
	
	/**
	 * Give this block a simple stairs model with three textures.
	 *
	 * @param topTexture    The Identifier for the texture for the top of the block
	 * @param bottomTexture The Identifier for the texture for the bottom of the block
	 * @param sideTexture   The Identifier for the texture for the sides of the block
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> stairsModel(Identifier topTexture, Identifier bottomTexture, Identifier sideTexture) {
		models.clear();
		JTextures textures = JModel.textures()
				.var("top", topTexture.toString())
				.var("bottom", bottomTexture.toString())
				.var("side", sideTexture.toString());
		JModel straight = JModel.model().parent("minecraft:block/stairs").textures(textures);
		JModel inner = JModel.model().parent("minecraft:block/inner_stairs").textures(textures);
		JModel outer = JModel.model().parent("minecraft:block/outer_stairs").textures(textures);
		Identifier straightID = new Identifier(getOwner().getModid(), "block/" + getName());
		Identifier innerID = new Identifier(getOwner().getModid(), "block/" + getName() + "_inner");
		Identifier outerID = new Identifier(getOwner().getModid(), "block/" + getName() + "_outer");
		models.put(straightID, straight);
		models.put(innerID, inner);
		models.put(outerID, outer);
		return blockstate(getIdentifier(),
				JState.state().add(JState.variant()
						// just put me out of my misery already
						.put("facing=east,half=bottom,shape=inner_left", JState.model(innerID).y(270).uvlock())
						.put("facing=east,half=bottom,shape=inner_right", JState.model(innerID))
						.put("facing=east,half=bottom,shape=outer_left", JState.model(outerID).y(270).uvlock())
						.put("facing=east,half=bottom,shape=outer_right", JState.model(outerID))
						.put("facing=east,half=bottom,shape=straight", JState.model(straightID))
						.put("facing=east,half=top,shape=inner_left", JState.model(innerID).x(180).uvlock())
						.put("facing=east,half=top,shape=inner_right", JState.model(innerID).x(180).y(90).uvlock())
						.put("facing=east,half=top,shape=outer_left", JState.model(outerID).x(180).uvlock())
						.put("facing=east,half=top,shape=outer_right", JState.model(outerID).x(180).y(90).uvlock())
						.put("facing=east,half=top,shape=straight", JState.model(straightID).x(180).uvlock())
						.put("facing=north,half=bottom,shape=inner_left", JState.model(innerID).y(180).uvlock())
						.put("facing=north,half=bottom,shape=inner_right", JState.model(innerID).y(270).uvlock())
						.put("facing=north,half=bottom,shape=outer_left", JState.model(outerID).y(180).uvlock())
						.put("facing=north,half=bottom,shape=outer_right", JState.model(outerID).y(270).uvlock())
						.put("facing=north,half=bottom,shape=straight", JState.model(straightID).y(270).uvlock())
						.put("facing=north,half=top,shape=inner_left", JState.model(innerID).x(180).y(270).uvlock())
						.put("facing=north,half=top,shape=inner_right", JState.model(innerID).x(180).uvlock())
						.put("facing=north,half=top,shape=outer_left", JState.model(outerID).x(180).y(270).uvlock())
						.put("facing=north,half=top,shape=outer_right", JState.model(outerID).x(180).uvlock())
						.put("facing=north,half=top,shape=straight", JState.model(straightID).x(180).y(270).uvlock())
						.put("facing=south,half=bottom,shape=inner_left", JState.model(innerID))
						.put("facing=south,half=bottom,shape=inner_right", JState.model(innerID).y(90).uvlock())
						.put("facing=south,half=bottom,shape=outer_left", JState.model(outerID))
						.put("facing=south,half=bottom,shape=outer_right", JState.model(outerID).y(90).uvlock())
						.put("facing=south,half=bottom,shape=straight", JState.model(straightID).y(90).uvlock())
						.put("facing=south,half=top,shape=inner_left", JState.model(innerID).x(180).y(90).uvlock())
						.put("facing=south,half=top,shape=inner_right", JState.model(innerID).x(180).y(180).uvlock())
						.put("facing=south,half=top,shape=outer_left", JState.model(outerID).x(180).y(90).uvlock())
						.put("facing=south,half=top,shape=outer_right", JState.model(outerID).x(180).y(180).uvlock())
						.put("facing=south,half=top,shape=straight", JState.model(straightID).x(180).y(90).uvlock())
						.put("facing=west,half=bottom,shape=inner_left", JState.model(innerID).y(90).uvlock())
						.put("facing=west,half=bottom,shape=inner_right", JState.model(innerID).y(180).uvlock())
						.put("facing=west,half=bottom,shape=outer_left", JState.model(outerID).y(90).uvlock())
						.put("facing=west,half=bottom,shape=outer_right", JState.model(outerID).y(180).uvlock())
						.put("facing=west,half=bottom,shape=straight", JState.model(straightID).y(180).uvlock())
						.put("facing=west,half=top,shape=inner_left", JState.model(innerID).x(180).y(180).uvlock())
						.put("facing=west,half=top,shape=inner_right", JState.model(innerID).x(180).y(270).uvlock())
						.put("facing=west,half=top,shape=outer_left", JState.model(outerID).x(180).y(180).uvlock())
						.put("facing=west,half=top,shape=outer_right", JState.model(outerID).x(180).y(270).uvlock())
						.put("facing=west,half=top,shape=straight", JState.model(straightID).x(180).y(180).uvlock())));
		// my brain is 🦀 now
	}
	
	/**
	 * Give this block a simple wall model with one texture.
	 *
	 * @param texture The Identifier for the texture for the block
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> wallModel(Identifier texture) {
		models.clear();
		JTextures textures = JModel.textures().var("wall", texture.toString());
		JModel post = JModel.model().parent("minecraft:block/template_wall_post").textures(textures);
		JModel side = JModel.model().parent("minecraft:block/template_wall_side").textures(textures);
		JModel tallSide = JModel.model().parent("minecraft:block/template_wall_side_tall").textures(textures);
		JModel inventory = JModel.model().parent("minecraft:block/wall_inventory").textures(textures);
		Identifier postID = new Identifier(getOwner().getModid(), "block/" + getName() + "_post");
		Identifier sideID = new Identifier(getOwner().getModid(), "block/" + getName() + "_side");
		Identifier tallSideID = new Identifier(getOwner().getModid(), "block/" + getName() + "_side_tall");
		Identifier inventoryID = new Identifier(getOwner().getModid(), "item/" + getName());
		models.put(postID, post);
		models.put(sideID, side);
		models.put(tallSideID, tallSide);
		models.put(inventoryID, inventory);
		return blockstate(getIdentifier(), JState.state()
				.add(JState.multipart().addModel(JState.model(postID)).when(JState.when().add("up", "true")))
				.add(JState.multipart().addModel(JState.model(sideID).uvlock()).when(JState.when().add("north", "low")))
				.add(JState.multipart().addModel(JState.model(sideID).y(90).uvlock()).when(JState.when().add("east", "low")))
				.add(JState.multipart().addModel(JState.model(sideID).y(180).uvlock()).when(JState.when().add("south", "low")))
				.add(JState.multipart().addModel(JState.model(sideID).y(270).uvlock()).when(JState.when().add("west", "low")))
				.add(JState.multipart().addModel(JState.model(tallSideID).uvlock()).when(JState.when().add("north", "tall")))
				.add(JState.multipart().addModel(JState.model(tallSideID).y(90).uvlock()).when(JState.when().add("east", "tall")))
				.add(JState.multipart().addModel(JState.model(tallSideID).y(180).uvlock()).when(JState.when().add("south", "tall")))
				.add(JState.multipart().addModel(JState.model(tallSideID).y(270).uvlock()).when(JState.when().add("west", "tall"))));
	}
	
	/**
	 * Give this block a simple fence model with one texture.
	 *
	 * @param texture The Identifier for the texture for the block
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> fenceModel(Identifier texture) {
		models.clear();
		JTextures textures = JModel.textures().var("texture", texture.toString());
		JModel post = JModel.model().parent("minecraft:block/fence_post").textures(textures);
		JModel side = JModel.model().parent("minecraft:block/fence_side").textures(textures);
		JModel inventory = JModel.model().parent("minecraft:block/fence_inventory").textures(textures);
		Identifier postID = new Identifier(getOwner().getModid(), "block/" + getName() + "_post");
		Identifier sideID = new Identifier(getOwner().getModid(), "block/" + getName() + "_side");
		Identifier inventoryID = new Identifier(getOwner().getModid(), "item/" + getName());
		models.put(postID, post);
		models.put(sideID, side);
		models.put(inventoryID, inventory);
		return blockstate(getIdentifier(), JState.state()
				.add(JState.multipart().addModel(JState.model(postID)))
				.add(JState.multipart().addModel(JState.model(sideID).uvlock()).when(JState.when().add("north", "true")))
				.add(JState.multipart().addModel(JState.model(sideID).y(90).uvlock()).when(JState.when().add("east", "true")))
				.add(JState.multipart().addModel(JState.model(sideID).y(180).uvlock()).when(JState.when().add("south", "true")))
				.add(JState.multipart().addModel(JState.model(sideID).y(270).uvlock()).when(JState.when().add("west", "true"))));
	}
	
	/**
	 * Configure the blockstate/models for this block.
	 *
	 * @param stateID The Identifier for the BlockState, should typically be equal to {@code new Identifier(getIdentifier())}, but may be changed to match the state.
	 * @param state   The BlockState to give this block, in the form of a raw {@link JState} object.
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> blockstate(Identifier stateID, JState state) {
		blockState = new Pair<>(stateID, state);
		return this;
	}
	
	/**
	 * Assign the default loot table. Block will drop itself.  Is applied by default, calling manually should not be necessary.
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
		lootTable = table;
		return this;
	}
	
	// ------- RECIPES -------
	
	/**
	 * Create a simple smithing recipe.
	 *
	 * @param base        The base item, such as a diamond tool.
	 * @param addition    The addition item, such as a Netherite ingot.
	 * @param outputCount The number of items in the output stack.
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> smithingRecipe(Item base, Item addition, int outputCount) {
		return recipe("smithing", JRecipe.smithing(JIngredient.ingredient().item(base), JIngredient.ingredient().item(addition), JResult.stackedResult(getIdentifierString(), outputCount)));
	}
	
	/**
	 * Create a simple recipe other than Shaped, Unshaped, and Smithing.
	 *
	 * @param input       The input for this recipe
	 * @param outputCount The number of items in the output stack
	 * @param type        The type of recipe, such as smelting or blasting.
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> recipe(Item input, int outputCount, RecipeTypes type) {
		if (type == RecipeTypes.SMELTING)
			return recipe("smelting", JRecipe.smelting(JIngredient.ingredient().item(input), JResult.stackedResult(getIdentifierString(), outputCount)));
		if (type == RecipeTypes.BLASTING)
			return recipe("blasting", JRecipe.blasting(JIngredient.ingredient().item(input), JResult.stackedResult(getIdentifierString(), outputCount)));
		if (type == RecipeTypes.SMOKING)
			return recipe("smoking", JRecipe.smoking(JIngredient.ingredient().item(input), JResult.stackedResult(getIdentifierString(), outputCount)));
		if (type == RecipeTypes.CAMPFIRE)
			return recipe("campfire", JRecipe.campfire(JIngredient.ingredient().item(input), JResult.stackedResult(getIdentifierString(), outputCount)));
		if (type == RecipeTypes.STONECUTTING)
			return recipe("stonecutting", JRecipe.stonecutting(JIngredient.ingredient().item(input), JResult.stackedResult(getIdentifierString(), outputCount)));
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
	 * <br>
	 * correct usage:<br>
	 * {@code quickShaped(1, "XXX", "XYX", "XXX", new Pair("X", Items.NETHERITE_SCRAP), new Pair("Y", Items.MOSSY_COBBLESTONE))}
	 *
	 * @param outputCount The number of items in the output stack
	 * @param row1        The first row of the shaped recipe
	 * @param row2        The second row of the shaped recipe
	 * @param row3        The third row of the shaped recipe
	 * @param keys        Pairs containing the keys to the items in the recipe.
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
	 * @param recipe     The recipe to add, in the form of a raw {@link JRecipe} object.
	 * @return this {@link BlockBuilder}
	 */
	public BlockBuilder<T, P> recipe(@Nullable String recipeType, JRecipe recipe) {
		getOwner().addRecipe(getName() + recipeType, recipe);
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
		if (getOwner().doDatagen) {
			for (Map.Entry<Identifier, JModel> entry : models.entrySet()) {
				getOwner().getResourcePack().addModel(entry.getValue(), entry.getKey());
			}
			getOwner().getResourcePack().addBlockState(blockState.getRight(), blockState.getLeft());
			if ((getParent() instanceof FluidBuilder) || ((AbstractBlock$SettingsAccessor) initialProperties.get()).getLootTableId() == LootTables.EMPTY) {
				return (BlockEntry<T>) super.register(); // fluid blocks don't get loot tables
			}
			getOwner().getResourcePack().addLootTable(new Identifier(getOwner().getModid(), "blocks/" + getName()), lootTable);
		}
		return (BlockEntry<T>) super.register();
	}
}
