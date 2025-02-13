package com.tterrag.registrarrp.builders;

import com.tterrag.registrarrp.AbstractRegistrate;
import com.tterrag.registrarrp.fabric.RegistryObject;
import com.tterrag.registrarrp.fabric.RegistryUtil;
import com.tterrag.registrarrp.util.Utils;
import com.tterrag.registrarrp.util.entry.LazyRegistryEntry;
import com.tterrag.registrarrp.util.entry.RegistryEntry;
import com.tterrag.registrarrp.util.nullness.NonNullSupplier;
import com.tterrag.registrarrp.util.nullness.NonnullType;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * Base class which most builders should extend, instead of implementing [@link {@link Builder} directly.
 * <p>
 * Provides the most basic functionality, and some utility methods that remove the need to pass the registry class.
 *
 * @param <R> Type of the registry for the current object. This is the concrete base class that all registry entries must extend, and the type used for the forge registry itself.
 * @param <T> Actual type of the object being built.
 * @param <P> Type of the parent object, this is returned from {@link #build()} and {@link #getParent()}.
 * @param <S> Self type
 * @see Builder
 */
public abstract class AbstractBuilder<R, T extends R, P, S extends AbstractBuilder<R, T, P, S>> implements Builder<R, T, P, S> {
	private final AbstractRegistrate<?> owner;
	private final P parent;
	private final String name;
	private final BuilderCallback callback;
	private final Class<? super R> registryType;
	/**
	 * A supplier for the entry that will discard the reference to this builder after it is resolved
	 */
	private final LazyRegistryEntry<T> safeSupplier = new LazyRegistryEntry<>(this);
	
	@javax.annotation.Generated("lombok")
	public AbstractBuilder(final AbstractRegistrate<?> owner, final P parent, final String name, final BuilderCallback callback, final Class<? super R> registryType) {
		this.owner = owner;
		this.parent = parent;
		this.name = name;
		this.callback = callback;
		this.registryType = registryType;
	}
	
	/**
	 * Create the built entry. This method will be lazily resolved at registration time, so it is safe to bake in values from the builder.
	 *
	 * @return The built entry
	 */
	@SuppressWarnings("null")
	@NonnullType
	protected abstract T createEntry();
	
	@Override
	public RegistryEntry<T> register() {
		return callback.accept(name, registryType, this, this::createEntry, this::createEntryWrapper);
	}
	
	protected RegistryEntry<T> createEntryWrapper(RegistryObject<T> delegate) {
		return new RegistryEntry<>(getOwner(), delegate);
	}
	
	@Override
	public NonNullSupplier<T> asSupplier() {
		return safeSupplier;
	}
	
	/**
	 * Tag this entry with a tag (or tags) of the correct type. Multiple calls will add additional tags.
	 *
	 * @param tags The tags to add
	 * @return this {@link Builder}
	 */
	@SuppressWarnings("unchecked")
	@SafeVarargs
	public final S tag(Tag.Identified<R>... tags) {
		for (Tag.Identified<R> tag : tags) {
			getOwner().addToTag(tag, getIdentifier());
		}
		return (S) this;
	}
	
	/**
	 * Tag this entry with a tag (or tags) of the correct type. Multiple calls will add additional tags.
	 *
	 * @param tags The tags to add
	 * @return this {@link Builder}
	 */
	@SuppressWarnings("unchecked")
	public final S tag(Identifier... tags) {
		for (Identifier tag : tags) {
			getOwner().addToTag(tag, getIdentifier());
		}
		return (S) this;
	}
	
	/**
	 * Set the lang for this entry to the default value. Is applied by default, calling manually should not be necessary.
	 *
	 * @return this {@link Builder}
	 */
	public S defaultLang() {
		return lang(Utils.toEnglishName(getName()));
	}
	
	/**
	 * Set the lang for this entry to the specified English value.
	 *
	 * @param name The name to use
	 * @return this {@link Builder}
	 */
	public S lang(String name) {
		return lang("en_us", name);
	}
	
	/**
	 * Set the lang for this entry to the specified value in the specified language.
	 *
	 * @param language The name of the language file, such as "en_us" for English.
	 * @param name     The name to use
	 * @return this {@link Builder}
	 */
	public S lang(String language, String name) {
		return lang(language,
				Util.createTranslationKey(RegistryUtil.getRegistry(getRegistryType()).getKey().getValue().getPath(), getIdentifier()),
				name);
	}
	
	/**
	 * Set the specified lang key to the specified name in the specified language.
	 *
	 * @param lang    A string of the lang name, such as "en_us" for English.
	 * @param langKey The lang key to translate
	 * @param name    The name to use
	 * @return this {@link Builder}
	 */
	public S lang(String lang, String langKey, String name) {
		getOwner().addLangEntry(this, lang, langKey, name);
		return (S) this;
	}
	
	public String getIdentifierString() {
		return getOwner().getModid() + ":" + getName();
	}
	
	public Identifier getIdentifier() {
		return new Identifier(getOwner().getModid(), getName());
	}
	
	@Override
	@javax.annotation.Generated("lombok")
	public AbstractRegistrate<?> getOwner() {
		return this.owner;
	}
	
	@Override
	@javax.annotation.Generated("lombok")
	public P getParent() {
		return this.parent;
	}
	
	@Override
	@javax.annotation.Generated("lombok")
	public String getName() {
		return this.name;
	}
	
	@javax.annotation.Generated("lombok")
	protected BuilderCallback getCallback() {
		return this.callback;
	}
	
	@Override
	@javax.annotation.Generated("lombok")
	public Class<? super R> getRegistryType() {
		return this.registryType;
	}
	
	@Override
	public String toString() {
		return getClass().getName().split("\\.")[getClass().getName().split("\\.").length - 1] + "[" +
				"Registry: " + RegistryUtil.getRegistry(getRegistryType()).getKey().getValue() + ", " +
				"Identifier: " + getIdentifierString() +
				"]";
	}
}
