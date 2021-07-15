package com.tterrag.registrarrp.builders;

import com.tterrag.registrarrp.AbstractRegistrate;
import com.tterrag.registrarrp.fabric.EnvExecutor;
import com.tterrag.registrarrp.fabric.RegistryObject;
import com.tterrag.registrarrp.util.LazySpawnEggItem;
import com.tterrag.registrarrp.util.entry.EntityEntry;
import com.tterrag.registrarrp.util.entry.RegistryEntry;
import com.tterrag.registrarrp.util.nullness.NonNullBiFunction;
import com.tterrag.registrarrp.util.nullness.NonNullConsumer;
import com.tterrag.registrarrp.util.nullness.NonNullSupplier;
import net.devtech.arrp.json.loot.JLootTable;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction.Location;
import net.minecraft.entity.SpawnRestriction.SpawnPredicate;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A builder for entities, allows for customization of the {@link FabricEntityTypeBuilder}, easy creation of spawn egg items, and configuration of data associated with entities (loot tables, etc.).
 *
 * @param <T> The type of entity being built
 * @param <P> Parent object type
 */
public class EntityBuilder<T extends Entity, B extends FabricEntityTypeBuilder<T>, P> extends AbstractBuilder<EntityType<?>, EntityType<T>, P, EntityBuilder<T, B, P>> {
	
	private final NonNullSupplier<B> builder;
	private NonNullConsumer<B> builderCallback = $ -> {
	};
	@Nullable
	private NonNullSupplier<EntityRendererRegistry.Factory> renderer;
	private boolean spawnConfigured;
	private @Nullable ItemBuilder<LazySpawnEggItem<T>, EntityBuilder<T, B, P>> spawnEggBuilder;
	
	protected EntityBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, EntityType.EntityFactory<T> factory, SpawnGroup classification, NonNullBiFunction<SpawnGroup, EntityType.EntityFactory<T>, B> function) {
		super(owner, parent, name, callback, EntityType.class);
		this.builder = () -> function.apply(classification, factory);
	}
	
	/**
	 * Create a new {@link EntityBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
	 * <p>
	 * The entity will be assigned the following data:
	 * <ul>
	 * <li>The default translation (via {@link #defaultLang()})</li>
	 * </ul>
	 *
	 * @param <T>            The type of the builder
	 * @param <P>            Parent object type
	 * @param owner          The owning {@link AbstractRegistrate} object
	 * @param parent         The parent object
	 * @param name           Name of the entry being built
	 * @param callback       A callback used to actually register the built entry
	 * @param factory        Factory to create the entity
	 * @param classification The {@link SpawnGroup} of the entity
	 * @return A new {@link EntityBuilder} with reasonable default data generators.
	 */
	public static <T extends Entity, P> EntityBuilder<T, FabricEntityTypeBuilder<T>, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, EntityType.EntityFactory<T> factory,
																							   SpawnGroup classification) {
		return new EntityBuilder<>(owner, parent, name, callback, factory, classification, FabricEntityTypeBuilder::create)
				.defaultLang();
	}
	
	public static <T extends LivingEntity, P> EntityBuilder<T, FabricEntityTypeBuilder.Living<T>, P> createLiving(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, EntityType.EntityFactory<T> factory,
																												  SpawnGroup classification) {
		return new EntityBuilder<>(owner, parent, name, callback, factory, classification, (s, f) -> FabricEntityTypeBuilder.createLiving().spawnGroup(s).entityFactory(f))
				.defaultLang();
	}
	
	public static <T extends MobEntity, P> EntityBuilder<T, FabricEntityTypeBuilder.Mob<T>, P> createMob(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, EntityType.EntityFactory<T> factory,
																										 SpawnGroup classification) {
		return new EntityBuilder<>(owner, parent, name, callback, factory, classification, (s, f) -> FabricEntityTypeBuilder.createMob().spawnGroup(s).entityFactory(f))
				.defaultLang();
	}
	
	/**
	 * Modify the properties of the entity. Modifications are done lazily, but the passed function is composed with the current one, and as such this method can be called multiple times to perform
	 * different operations.
	 *
	 * @param cons The action to perform on the properties
	 * @return this {@link EntityBuilder}
	 */
	public EntityBuilder<T, B, P> properties(NonNullConsumer<B> cons) {
		builderCallback = builderCallback.andThen(cons);
		return this;
	}
	
	/**
	 * Register an {@link EntityRenderer} for this entity.
	 * <p>
	 *
	 * @param renderer A (server safe) supplier to an {@link EntityRendererRegistry.Factory} that will provide this entity's renderer
	 * @return this {@link EntityBuilder}
	 * @apiNote This requires the {@link Class} of the entity object, which can only be gotten by inspecting an instance of it. Thus, the entity will be constructed with a {@code null} {@link World}
	 * to register the renderer.
	 */
	public EntityBuilder<T, B, P> renderer(NonNullSupplier<EntityRendererRegistry.Factory> renderer) {
		if (this.renderer == null) { // First call only
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::registerRenderer);
		}
		this.renderer = renderer;
		return this;
	}
	
	protected void registerRenderer() {
		onRegister(entry -> {
			try {
				EntityRendererRegistry.INSTANCE.register(entry, renderer.get());
			} catch (Exception e) {
				throw new IllegalStateException("Failed to register renderer for Entity " + get().getId(), e);
			}
		});
	}
	
	/**
	 * Register a spawn placement for this entity. The entity must extend {@link MobEntity} and allow construction with a {@code null} {@link World}.
	 * <p>
	 * Cannot be called more than once per builder.
	 *
	 * @param type      The type of placement to use
	 * @param heightmap Which heightmap to use to choose placement locations
	 * @param predicate A predicate to check spawn locations for validity
	 * @return this {@link EntityBuilder}
	 * @throws IllegalStateException When called more than once
	 */
	@Deprecated
	@SuppressWarnings({"unchecked", "rawtypes"})
	public EntityBuilder<T, B, P> spawnPlacement(Location type, Heightmap.Type heightmap, SpawnPredicate<T> predicate) {
		if (spawnConfigured) {
			throw new IllegalStateException("Cannot configure spawn placement more than once");
		}
		spawnConfigured = true;
		if (builder.get() instanceof FabricEntityTypeBuilder.Mob) {
			((FabricEntityTypeBuilder.Mob) builder.get()).spawnRestriction(type, heightmap, predicate);
		}
		return this;
	}
	
	/**
	 * Create a spawn egg item for this entity using the given colors, not allowing for any extra configuration.
	 *
	 * @param primaryColor   The primary color of the egg
	 * @param secondaryColor The secondary color of the egg
	 * @return this {@link EntityBuilder}
	 * @deprecated This does not work properly, see <a href="https://github.com/MinecraftForge/MinecraftForge/pull/6299">this issue</a>.
	 * <p>
	 * As a temporary measure, uses a custom egg class that imperfectly emulates the functionality
	 */
	@Deprecated
	public EntityBuilder<T, B, P> defaultSpawnEgg(int primaryColor, int secondaryColor) {
		return spawnEgg(primaryColor, secondaryColor).build();
	}
	
	/**
	 * Create a spawn egg item for this entity using the given colors, and return the builder for further configuration.
	 *
	 * @param primaryColor   The primary color of the egg
	 * @param secondaryColor The secondary color of the egg
	 * @return the {@link ItemBuilder} for the egg item
	 * @deprecated This does not work properly, see <a href="https://github.com/MinecraftForge/MinecraftForge/pull/6299">this issue</a>.
	 * <p>
	 * As a temporary measure, uses a custom egg class that imperfectly emulates the functionality
	 */
	@Deprecated
	public ItemBuilder<? extends SpawnEggItem, EntityBuilder<T, B, P>> spawnEgg(int primaryColor, int secondaryColor) {
		ItemBuilder<LazySpawnEggItem<T>, EntityBuilder<T, B, P>> ret = getOwner().item(this, getName() + "_spawn_egg", p -> new LazySpawnEggItem<>(asSupplier(), primaryColor, secondaryColor, p)).properties(p -> p.group(ItemGroup.MISC))
				/*.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), new Identifier("item/template_spawn_egg")))*/;
		if (this.spawnEggBuilder == null) { // First call only
			this.onRegister(this::injectSpawnEggType);
		}
		this.spawnEggBuilder = ret;
		return ret;
	}
	
	/**
	 * Configure the loot table for this entity.
	 *
	 * @param table The loot table for this entity, in the form of a raw {@link JLootTable} object.
	 * @return this {@link EntityBuilder}
	 */
	public EntityBuilder<T, B, P> loot(JLootTable table) {
		if (getOwner().doDatagen) {
			getOwner().getResourcePack().addLootTable(new Identifier(getOwner().getModid(), "entities/" + getName()), table);
		}
		return this;
	}
	
	@Override
	protected EntityType<T> createEntry() {
		B builder = this.builder.get();
		builderCallback.accept(builder);
		return builder.build();
	}
	
	protected void injectSpawnEggType(EntityType<T> entry) {
		ItemBuilder<LazySpawnEggItem<T>, EntityBuilder<T, B, P>> spawnEggBuilder = this.spawnEggBuilder;
		if (spawnEggBuilder != null) {
			spawnEggBuilder.getEntry().injectType();
		}
	}
	
	@Override
	protected RegistryEntry<EntityType<T>> createEntryWrapper(RegistryObject<EntityType<T>> delegate) {
		return new EntityEntry<>(getOwner(), delegate);
	}
	
	@Override
	public EntityEntry<T> register() {
		return (EntityEntry<T>) super.register();
	}
}
