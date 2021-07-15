package com.tterrag.registrarrp.builders;

import com.tterrag.registrarrp.AbstractRegistrate;
import com.tterrag.registrarrp.fabric.EnvExecutor;
import com.tterrag.registrarrp.fabric.RegistryObject;
import com.tterrag.registrarrp.util.entry.ItemEntry;
import com.tterrag.registrarrp.util.entry.RegistryEntry;
import com.tterrag.registrarrp.util.nullness.NonNullFunction;
import com.tterrag.registrarrp.util.nullness.NonNullSupplier;
import com.tterrag.registrarrp.util.nullness.NonNullUnaryOperator;
import net.devtech.arrp.json.models.JModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A builder for items, allows for customization of the {@link FabricItemSettings} and configuration of data associated with items (models, recipes, etc.).
 *
 * @param <T> The type of item being built
 * @param <P> Parent object type
 */
public class ItemBuilder<T extends Item, P> extends AbstractBuilder<Item, T, P, ItemBuilder<T, P>> {
	
	private final NonNullFunction<FabricItemSettings, T> factory;
	private NonNullSupplier<FabricItemSettings> initialProperties = FabricItemSettings::new;
	private NonNullFunction<FabricItemSettings, FabricItemSettings> propertiesCallback = NonNullUnaryOperator.identity();
	@Nullable
	private NonNullSupplier<Supplier<ItemColorProvider>> colorHandler;
	
	protected ItemBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<FabricItemSettings, T> factory) {
		super(owner, parent, name, callback, Item.class);
		this.factory = factory;
	}
	
	/**
	 * Create a new {@link ItemBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
	 * <p>
	 * The item will be assigned the following data:
	 * <ul>
	 * <li>A simple generated model with one texture (via {@link #defaultModel()})</li>
	 * <li>The default translation (via {@link #defaultLang()})</li>
	 * </ul>
	 *
	 * @param <T>      The type of the builder
	 * @param <P>      Parent object type
	 * @param owner    The owning {@link AbstractRegistrate} object
	 * @param parent   The parent object
	 * @param name     Name of the entry being built
	 * @param callback A callback used to actually register the built entry
	 * @param factory  Factory to create the item
	 * @return A new {@link ItemBuilder} with reasonable default data generators.
	 */
	public static <T extends Item, P> ItemBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<FabricItemSettings, T> factory) {
		return create(owner, parent, name, callback, factory, null);
	}
	
	/**
	 * Create a new {@link ItemBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
	 * <p>
	 * The item will be assigned the following data:
	 * <ul>
	 * <li>A simple generated model with one texture (via {@link #defaultModel()})</li>
	 * <li>The default translation (via {@link #defaultLang()})</li>
	 * <li>An {@link ItemGroup} set in the properties from the group supplier parameter, if non-null</li>
	 * </ul>
	 *
	 * @param <T>      The type of the builder
	 * @param <P>      Parent object type
	 * @param owner    The owning {@link AbstractRegistrate} object
	 * @param parent   The parent object
	 * @param name     Name of the entry being built
	 * @param callback A callback used to actually register the built entry
	 * @param factory  Factory to create the item
	 * @param group    The {@link ItemGroup} for the object, can be null for none
	 * @return A new {@link ItemBuilder} with reasonable default data generators.
	 */
	public static <T extends Item, P> ItemBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<FabricItemSettings, T> factory, @Nullable NonNullSupplier<? extends ItemGroup> group) {
		return new ItemBuilder<>(owner, parent, name, callback, factory)
				.defaultModel().defaultLang()
				.transform(ib -> group == null ? ib : ib.group(group));
	}
	
	/**
	 * Modify the properties of the item. Modifications are done lazily, but the passed function is composed with the current one, and as such this method can be called multiple times to perform
	 * different operations.
	 * <p>
	 * If a different properties instance is returned, it will replace the existing one entirely.
	 *
	 * @param func The action to perform on the properties
	 * @return this {@link ItemBuilder}
	 */
	public ItemBuilder<T, P> properties(NonNullUnaryOperator<FabricItemSettings> func) {
		propertiesCallback = propertiesCallback.andThen(func);
		return this;
	}
	
	/**
	 * Replace the initial state of the item properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
	 *
	 * @param properties A supplier to to create the initial properties
	 * @return this {@link ItemBuilder}
	 */
	public ItemBuilder<T, P> initialProperties(NonNullSupplier<FabricItemSettings> properties) {
		initialProperties = properties;
		return this;
	}
	
	public ItemBuilder<T, P> group(NonNullSupplier<? extends ItemGroup> group) {
		return properties(p -> p.group(group.get()));
	}
	
	/**
	 * Register a block color handler for this item. The {@link ItemColorProvider} instance can be shared across many items.
	 *
	 * @param colorHandler The color handler to register for this item
	 * @return this {@link ItemBuilder}
	 */
	public ItemBuilder<T, P> color(NonNullSupplier<Supplier<ItemColorProvider>> colorHandler) {
		if (this.colorHandler == null) {
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::registerItemColor);
		}
		this.colorHandler = colorHandler;
		return this;
	}
	
	protected void registerItemColor() {
		onRegister(entry -> ColorProviderRegistry.ITEM.register(colorHandler.get().get(), entry));
	}
	
	/**
	 * Assign the default model to this item, which is simply a generated model with a single texture of the same name.
	 *
	 * @return this {@link ItemBuilder}
	 */
	public ItemBuilder<T, P> defaultModel() {
		JModel model = JModel.model();
		if (getParent() instanceof BlockBuilder) {
			model.parent(getOwner().getModid() + ":block/" + ((BlockBuilder<?, ?>) getParent()).getName());
		} else {
			model.parent("minecraft:item/generated");
			model.textures(JModel.textures().layer0(getOwner().getModid() + ":items/" + getName()));
		} // why is it "items" in one place and "item" in another just decide please minecraft
		return model(new Identifier(getOwner().getModid(), "item/" + getName()), model);
	}
	
	/**
	 * Configure the model for this item.
	 * Creates a simple item model with a flat texture.
	 *
	 * @param texture The identifier for the texture to use for this model.
	 * @return this {@link ItemBuilder}
	 */
	public ItemBuilder<T, P> model(Identifier texture) {
		JModel model = JModel.model().parent("minecraft:item/generated").textures(JModel.textures().layer0(texture.toString()));
		return model(new Identifier(getOwner().getModid(), "item/" + getName()), model);
	}
	
	/**
	 * Configure the model for this item.
	 *
	 * @param modelID The Identifier of the model, such as "minecraft:block/cobblestone_stairs_inner" or "minecraft:block/dirt"
	 * @param model   The model for this item, in the form of a raw {@link JModel} object.
	 * @return this {@link ItemBuilder}
	 */
	public ItemBuilder<T, P> model(Identifier modelID, JModel model) {
		getOwner().getResourcePack().addModel(model, modelID);
		return this;
	}
	
	@Override
	protected T createEntry() {
		FabricItemSettings properties = this.initialProperties.get();
		properties = propertiesCallback.apply(properties);
		return factory.apply(properties);
	}
	
	@Override
	protected RegistryEntry<T> createEntryWrapper(RegistryObject<T> delegate) {
		return new ItemEntry<>(getOwner(), delegate);
	}
	
	@Override
	public ItemEntry<T> register() {
		return (ItemEntry<T>) super.register();
	}
}
