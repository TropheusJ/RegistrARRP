package com.tterrag.registrarrp.builders;

import com.tterrag.registrarrp.AbstractRegistrate;
import com.tterrag.registrarrp.fabric.EnvExecutor;
import com.tterrag.registrarrp.fabric.RegistryObject;
import com.tterrag.registrarrp.util.entry.RegistryEntry;
import com.tterrag.registrarrp.util.entry.TileEntityEntry;
import com.tterrag.registrarrp.util.nullness.NonNullFunction;
import com.tterrag.registrarrp.util.nullness.NonNullSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A builder for tile entities, allows for customization of the valid blocks.
 *
 * @param <T> The type of tile entity being built
 * @param <P> Parent object type
 */
public class TileEntityBuilder<T extends BlockEntity, P> extends AbstractBuilder<BlockEntityType<?>, BlockEntityType<T>, P, TileEntityBuilder<T, P>> {
	
	private final NonNullFunction<BlockEntityType<T>, ? extends T> factory;
	private final Set<NonNullSupplier<? extends Block>> validBlocks = new HashSet<>();
	@Nullable
	private NonNullSupplier<Function<BlockEntityRenderDispatcher, BlockEntityRenderer<? super T>>> renderer;
	
	protected TileEntityBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<BlockEntityType<T>, ? extends T> factory) {
		super(owner, parent, name, callback, BlockEntityType.class);
		this.factory = factory;
	}
	
	/**
	 * Create a new {@link TileEntityBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
	 * <p>
	 * The tile entity will be assigned the following data:
	 *
	 * @param <T>      The type of the builder
	 * @param <P>      Parent object type
	 * @param owner    The owning {@link AbstractRegistrate} object
	 * @param parent   The parent object
	 * @param name     Name of the entry being built
	 * @param callback A callback used to actually register the built entry
	 * @param factory  Factory to create the tile entity
	 * @return A new {@link TileEntityBuilder} with reasonable default data generators.
	 */
	public static <T extends BlockEntity, P> TileEntityBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<BlockEntityType<T>, ? extends T> factory) {
		return new TileEntityBuilder<>(owner, parent, name, callback, factory);
	}
	
	/**
	 * Add a valid block for this tile entity.
	 *
	 * @param block A supplier for the block to add at registration time
	 * @return this {@link TileEntityBuilder}
	 */
	public TileEntityBuilder<T, P> validBlock(NonNullSupplier<? extends Block> block) {
		validBlocks.add(block);
		return this;
	}
	
	/**
	 * Add valid blocks for this tile entity.
	 *
	 * @param blocks An array of suppliers for the block to add at registration time
	 * @return this {@link TileEntityBuilder}
	 */
	@SafeVarargs
	public final TileEntityBuilder<T, P> validBlocks(NonNullSupplier<? extends Block>... blocks) {
		Arrays.stream(blocks).forEach(this::validBlock);
		return this;
	}
	
	/**
	 * Register an {@link BlockEntityRenderer} for this tile entity.
	 * <p>
	 *
	 * @param renderer A (server safe) supplier to an {@link Function} that will provide this tile entity's renderer given the renderer dispatcher
	 * @return this {@link TileEntityBuilder}
	 * @apiNote This requires the {@link Class} of the tile entity object, which can only be gotten by inspecting an instance of it. Thus, the entity will be constructed to register the renderer.
	 */
	public TileEntityBuilder<T, P> renderer(NonNullSupplier<Function<BlockEntityRenderDispatcher, BlockEntityRenderer<? super T>>> renderer) {
		if (this.renderer == null) { // First call only
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::registerRenderer);
		}
		this.renderer = renderer;
		return this;
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected void registerRenderer() {
		onRegister(entry -> BlockEntityRendererRegistry.INSTANCE.register((BlockEntityType) entry, (Function) renderer.get()));
	}
	
	@Override
	protected BlockEntityType<T> createEntry() {
		NonNullFunction<BlockEntityType<T>, ? extends T> factory = this.factory;
		Supplier<BlockEntityType<T>> supplier = asSupplier();
		return BlockEntityType.Builder.<T>create(() -> factory.apply(supplier.get()), validBlocks.stream().map(NonNullSupplier::get).toArray(Block[]::new))
				.build(null);
	}
	
	@Override
	protected RegistryEntry<BlockEntityType<T>> createEntryWrapper(RegistryObject<BlockEntityType<T>> delegate) {
		return new TileEntityEntry<>(getOwner(), delegate);
	}
	
	@Override
	public TileEntityEntry<T> register() {
		return (TileEntityEntry<T>) super.register();
	}
}
