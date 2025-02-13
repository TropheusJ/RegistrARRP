package com.tterrag.registrarrp.builders;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.tterrag.registrarrp.AbstractRegistrate;
import com.tterrag.registrarrp.fabric.*;
import com.tterrag.registrarrp.util.NonNullLazyValue;
import com.tterrag.registrarrp.util.entry.FluidEntry;
import com.tterrag.registrarrp.util.entry.RegistryEntry;
import com.tterrag.registrarrp.util.nullness.NonNullBiFunction;
import com.tterrag.registrarrp.util.nullness.NonNullConsumer;
import com.tterrag.registrarrp.util.nullness.NonNullFunction;
import com.tterrag.registrarrp.util.nullness.NonNullSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.Tag.Identified;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A builder for fluids, allows for customization of the {@link SimpleFlowableFluid.Properties}, and creation of the source variant, fluid block, and bucket item, as well as
 * data associated with fluids (tags, etc.).
 *
 * @param <T> The type of fluid being built
 * @param <P> Parent object type
 */
public class FluidBuilder<T extends SimpleFlowableFluid, P> extends AbstractBuilder<Fluid, T, P, FluidBuilder<T, P>> {
	private final Identifier stillTexture;
	private final Identifier flowingTexture;
	private final String sourceName;
	private final String bucketName;
	private final NonNullFunction<SimpleFlowableFluid.Properties, T> factory;
	private final List<Identified<Fluid>> tags = new ArrayList<>();
	@Nullable
	private Boolean defaultSource, defaultBlock, defaultBucket;
	private NonNullConsumer<SimpleFlowableFluid.Properties> properties;
	@Nullable
	private NonNullLazyValue<? extends SimpleFlowableFluid> source;
	private Supplier<Supplier<RenderLayer>> renderLayer;
	private Supplier<RenderHandlerFactory> renderHandler;
	private int color = -1;
	
	protected FluidBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, Identifier stillTexture,
						   Identifier flowingTexture, NonNullFunction<SimpleFlowableFluid.Properties, T> factory) {
		super(owner, parent, "flowing_" + name, callback, Fluid.class);
		this.stillTexture = stillTexture;
		this.flowingTexture = flowingTexture;
		this.sourceName = name;
		this.bucketName = name + "_bucket";
		this.factory = factory;
		
		String bucketName = this.bucketName;
		this.properties = p -> p.bucket(() -> owner.get(bucketName, Item.class).get())
				.block(() -> owner.<Block, FluidBlock>get(name, Block.class).get());
	}
	
	/**
	 * Create a new {@link FluidBuilder} and configure data. The created builder will use the default fluid class ({@link SimpleFlowableFluid.Flowing}).
	 *
	 * @param <P>            Parent object type
	 * @param owner          The owning {@link AbstractRegistrate} object
	 * @param parent         The parent object
	 * @param name           Name of the entry being built
	 * @param callback       A callback used to actually register the built entry
	 * @param stillTexture   The texture to use for still fluids
	 * @param flowingTexture The texture to use for flowing fluids
	 * @return A new {@link FluidBuilder} with reasonable default data generators.
	 * @see #create(AbstractRegistrate, Object, String, BuilderCallback, Identifier, Identifier, NonNullFunction)
	 */
	public static <P> FluidBuilder<SimpleFlowableFluid.Flowing, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback,
																		  Identifier stillTexture, Identifier flowingTexture) {
		return create(owner, parent, name, callback, stillTexture, flowingTexture, SimpleFlowableFluid.Flowing::new);
	}
	
	/**
	 * Create a new {@link FluidBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
	 * <p>
	 * The fluid will be assigned the following data:
	 * <ul>
	 * <li>The default translation (via {@link #defaultLang()})</li>
	 * <li>A default {@link SimpleFlowableFluid.Still source fluid} (via {@link #defaultSource})</li>
	 * <li>A default block for the fluid, with its own default blockstate and model that configure the particle texture (via {@link #defaultBlock()})</li>
	 * <li>A default bucket item, that uses a simple generated item model with a texture of the same name as this fluid (via {@link #defaultBucket()})</li>
	 * <li>Tagged with {@link FluidTags#WATER}</li>
	 * </ul>
	 *
	 * @param <T>            The type of the builder
	 * @param <P>            Parent object type
	 * @param owner          The owning {@link AbstractRegistrate} object
	 * @param parent         The parent object
	 * @param name           Name of the entry being built
	 * @param callback       A callback used to actually register the built entry
	 * @param stillTexture   The texture to use for still fluids
	 * @param flowingTexture The texture to use for flowing fluids
	 * @param factory        A factory that creates the flowing fluid
	 * @return A new {@link FluidBuilder} with reasonable default data generators.
	 */
	public static <T extends SimpleFlowableFluid, P> FluidBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name,
																			   BuilderCallback callback, Identifier stillTexture,
																			   Identifier flowingTexture,
																			   NonNullFunction<SimpleFlowableFluid.Properties, T> factory) {
		FluidBuilder<T, P> ret = new FluidBuilder<>(owner, parent, name, callback, stillTexture, flowingTexture, factory)
				.defaultLang().defaultSource().defaultBlock().defaultBucket()
				.tag(FluidTags.WATER);
		
		return ret;
	}
	
	/**
	 * Modify the properties of the fluid. Modifications are done lazily, but the passed function is composed with the current one, and as such this method can be called multiple times to perform
	 * different operations.
	 *
	 * @param cons The action to perform on the properties
	 * @return this {@link FluidBuilder}
	 */
	public FluidBuilder<T, P> properties(NonNullConsumer<SimpleFlowableFluid.Properties> cons) {
		properties = properties.andThen(cons);
		return this;
	}
	
	/**
	 * Create a standard {@link SimpleFlowableFluid.Still} for this fluid which will be built and registered along with this fluid.
	 *
	 * @return this {@link FluidBuilder}
	 * @throws IllegalStateException If {@link #source(NonNullFunction)} has been called before this method
	 * @see #source(NonNullFunction)
	 */
	public FluidBuilder<T, P> defaultSource() {
		if (this.defaultSource != null) {
			throw new IllegalStateException("Cannot set a default source after a custom source has been created");
		}
		this.defaultSource = true;
		return this;
	}
	
	/**
	 * Create a {@link SimpleFlowableFluid} for this fluid, which is created by the given factory, and which will be built and registered along with this fluid.
	 *
	 * @param factory A factory for the fluid, which accepts the properties and returns a new fluid
	 * @return this {@link FluidBuilder}
	 */
	public FluidBuilder<T, P> source(NonNullFunction<SimpleFlowableFluid.Properties, ? extends SimpleFlowableFluid> factory) {
		this.defaultSource = false;
		this.source = new NonNullLazyValue<>(() -> factory.apply(makeProperties()));
		return this;
	}
	
	/**
	 * Create a standard {@link FluidBlock} for this fluid, building it immediately, and not allowing for further configuration.
	 *
	 * @return this {@link FluidBuilder}
	 * @throws IllegalStateException If {@link #block()} or {@link #block(NonNullBiFunction)} has been called before this method
	 * @see #block()
	 */
	public FluidBuilder<T, P> defaultBlock() {
		if (this.defaultBlock != null) {
			throw new IllegalStateException("Cannot set a default block after a custom block has been created");
		}
		this.defaultBlock = true;
		return this;
	}
	
	/**
	 * Create a standard {@link FluidBlock} for this fluid, setting the model to the one specified by the Identifier, and building it immediately.
	 *
	 * @return the {@link BlockBuilder} for the {@link FluidBlock}
	 */
	public FluidBuilder<T, P> block(Identifier texture) {
		return block1(FluidBlockHelper::createFluidBlock).cubeModel(texture).build();
	}
	
	/**
	 * Create a standard {@link FluidBlock} for this fluid, and return the builder for it so that further customization can be done.
	 *
	 * @return the {@link BlockBuilder} for the {@link FluidBlock}
	 */
	public BlockBuilder<FluidBlock, FluidBuilder<T, P>> block() {
		return block1(FluidBlockHelper::createFluidBlock);
	}
	
	/**
	 * Create a {@link FluidBlock} for this fluid, which is created by the given factory, and return the builder for it so that further customization can be done.
	 *
	 * @param <B>     The type of the block
	 * @param factory A factory for the block, which accepts the block object and properties and returns a new block
	 * @return the {@link BlockBuilder} for the {@link FluidBlock}
	 */
	public <B extends FluidBlock> BlockBuilder<B, FluidBuilder<T, P>> block(NonNullBiFunction<NonNullSupplier<? extends T>, FabricBlockSettings, ? extends B> factory) {
		if (this.defaultBlock == Boolean.FALSE) {
			throw new IllegalStateException("Only one call to block/noBlock per builder allowed");
		}
		this.defaultBlock = false;
		NonNullSupplier<T> supplier = asSupplier();
		return getOwner().<B, FluidBuilder<T, P>>block(this, sourceName, p -> factory.apply(supplier, p))
				.properties(p -> FabricBlockSettings.copyOf(Blocks.WATER).dropsNothing())
//                .properties(p -> {
//                    // TODO is this ok?
//                    FluidAttributes attrs = this.attributes.get().build(Fluids.WATER);
//                    return p.lightLevel($ -> attrs.getLuminosity());
//                })
                /*.blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), prov.models().getBuilder(sourceName)
                                .texture("particle", stillTexture)))*/;
	}
	
	// Fabric TODO
	@SuppressWarnings("unchecked")
	public <B extends FluidBlock> BlockBuilder<B, FluidBuilder<T, P>> block1(NonNullBiFunction<? extends T, FabricBlockSettings, ? extends B> factory) {
		return block((supplier, settings) -> ((NonNullBiFunction<T, FabricBlockSettings, ? extends B>) factory).apply(supplier.get(), settings));
	}
	
	@Beta
	public FluidBuilder<T, P> noBlock() {
		if (this.defaultBlock == Boolean.FALSE) {
			throw new IllegalStateException("Only one call to block/noBlock per builder allowed");
		}
		this.defaultBlock = false;
		return this;
	}
	
	/**
	 * Create a standard {@link BucketItem} for this fluid, building it immediately, and not allowing for further configuration.
	 *
	 * @return this {@link FluidBuilder}
	 * @throws IllegalStateException If {@link #bucket()}, {@link #bucket(NonNullBiFunction)}, or {@link #bucket(NonNullBiFunction, String)} has been called before this method
	 * @see #bucket()
	 */
	public FluidBuilder<T, P> defaultBucket() {
		if (this.defaultBucket != null) {
			throw new IllegalStateException("Cannot set a default bucket after a custom bucket has been created");
		}
		defaultBucket = true;
		return this;
	}
	
	/**
	 * Create a standard {@link BucketItem} for this fluid, setting the model to the one specified by the Identifier, and building it immediately.
	 *
	 * @param modelID The Identifier for the model this bucket will use.
	 * @return the {@link ItemBuilder} for the {@link BucketItem}
	 */
	public FluidBuilder<T, P> bucket(Identifier modelID) {
		return bucket().model(modelID).build();
	}
	
	/**
	 * Create a standard {@link BucketItem} for this fluid, and return the builder for it so that further customization can be done.
	 *
	 * @return the {@link ItemBuilder} for the {@link BucketItem}
	 */
	public ItemBuilder<BucketItem, FluidBuilder<T, P>> bucket() {
		return bucket(BucketItem::new);
	}
	
	/**
	 * Create a {@link BucketItem} for this fluid, which is created by the given factory, and return the builder for it so that further customization can be done.
	 *
	 * @param <I>     The type of the bucket item
	 * @param factory A factory for the bucket item, which accepts the fluid object supplier and properties and returns a new item
	 * @return the {@link ItemBuilder} for the {@link BucketItem}
	 */
	public <I extends BucketItem> ItemBuilder<I, FluidBuilder<T, P>> bucket(NonNullBiFunction<? extends SimpleFlowableFluid, FabricItemSettings, ? extends I> factory) {
		return bucket(factory, bucketName);
	}
	
	/**
	 * Create a {@link BucketItem} for this fluid, which is created by the given factory, and return the builder for it so that further customization can be done.
	 *
	 * @param <I>     The type of the bucket item
	 * @param factory A factory for the bucket item, which accepts the fluid object supplier and properties and returns a new item
	 * @return the {@link ItemBuilder} for the {@link BucketItem}
	 */
	public <I extends BucketItem> ItemBuilder<I, FluidBuilder<T, P>> bucket(NonNullBiFunction<? extends SimpleFlowableFluid, FabricItemSettings, ? extends I> factory, String bucketName) {
		if (this.defaultBucket == Boolean.FALSE) {
			throw new IllegalStateException("Only one call to bucket/noBucket per builder allowed");
		}
		this.defaultBucket = false;
		return getOwner().<I, FluidBuilder<T, P>>item(this, bucketName, p -> ((NonNullBiFunction<SimpleFlowableFluid, FabricItemSettings, ? extends I>) factory).apply(this.source.get(), p)) // Fabric TODO
				.properties(p -> p.recipeRemainder(Items.BUCKET).maxCount(1));
	}
	
	@Beta
	public FluidBuilder<T, P> noBucket() {
		if (this.defaultBucket == Boolean.FALSE) {
			throw new IllegalStateException("Only one call to bucket/noBucket per builder allowed");
		}
		this.defaultBucket = false;
		return this;
	}
	
	private SimpleFlowableFluid getSource() {
		NonNullLazyValue<? extends SimpleFlowableFluid> source = this.source;
		Preconditions.checkNotNull(source, "Fluid has no source block: " + sourceName);
		return source.get();
	}
	
	private SimpleFlowableFluid.Properties makeProperties() {
		SimpleFlowableFluid.Properties ret = new SimpleFlowableFluid.Properties(source, asSupplier());
		properties.accept(ret);
		return ret;
	}
	
	
	// New - Fabric only
	
	@Override
	protected T createEntry() {
		return factory.apply(makeProperties());
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * Additionally registers the source fluid.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public FluidEntry<T> register() {
		if (defaultSource == Boolean.TRUE) {
			source(SimpleFlowableFluid.Still::new);
		}
		if (defaultBlock == Boolean.TRUE) {
			block().register();
		}
		if (defaultBucket == Boolean.TRUE) {
			bucket().register();
		}
		NonNullSupplier<? extends SimpleFlowableFluid> source = this.source;
		if (source != null) {
			getCallback().accept(sourceName, Fluid.class, (FluidBuilder) this, source);
		} else {
			throw new IllegalStateException("Fluid must have a source version: " + getName());
		}
		if (renderHandler == null) {
			this.setDefaultRenderHandler();
		}
		onRegister(this::registerRenderHandler);
		return (FluidEntry<T>) super.register();
	}
	
	@Override
	protected RegistryEntry<T> createEntryWrapper(RegistryObject<T> delegate) {
		return new FluidEntry<>(getOwner(), delegate);
	}
	
	public FluidBuilder<T, P> layer(Supplier<Supplier<RenderLayer>> layer) {
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
			Preconditions.checkArgument(RenderLayer.getBlockLayers().contains(layer.get().get()), "Invalid block layer: " + layer);
		});
		if (this.renderLayer == null) {
			onRegister(this::registerLayer);
		}
		this.renderLayer = layer;
		return this;
	}
	
	protected void registerLayer(T entry) {
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
			final RenderLayer layer = renderLayer.get().get();
			BlockRenderLayerMap.INSTANCE.putFluid(entry, layer);
		});
	}
	
	public FluidBuilder<T, P> renderHandler(Supplier<RenderHandlerFactory> handler) {
		if (this.color != -1) {
			throw new IllegalArgumentException("Can only set either color or render handler factory!");
		}
		this.renderHandler = handler;
		return this;
	}
	
	public FluidBuilder<T, P> color(int color) {
		if (this.renderHandler != null) {
			throw new IllegalArgumentException("Can only set either color or render handler factory!");
		}
		this.color = color;
		return this;
	}
	
	protected void registerRenderHandler(T entry) {
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
			final FluidRenderHandler handler = renderHandler.get().create(stillTexture, flowingTexture);
			FluidRenderHandlerRegistry.INSTANCE.register(entry, handler);
			FluidRenderHandlerRegistry.INSTANCE.register(entry.getStill(), handler);
		});
	}
	
	protected void setDefaultRenderHandler() {
		this.renderHandler = () -> (stillTexture, flowingTexture) -> {
			final SimpleFluidRenderHandler handler = new SimpleFluidRenderHandler(color);
			handler.registerListeners(stillTexture, flowingTexture);
			return handler;
		};
	}
	
	public interface RenderHandlerFactory {
		FluidRenderHandler create(Identifier stillTexture, Identifier flowingTexture);
	}
}
