package com.tterrag.registrarrp.util.entry;

import com.tterrag.registrarrp.AbstractRegistrate;
import com.tterrag.registrarrp.fabric.RegistryObject;
import com.tterrag.registrarrp.fabric.RegistryUtil;
import com.tterrag.registrarrp.util.nullness.NonNullSupplier;
import com.tterrag.registrarrp.util.nullness.NonnullType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Wraps a {@link RegistryObject}, providing a cleaner API with null-safe access, and registrarrp-specific extensions such as {@link #getSibling(Class)}.
 *
 * @param <T> The type of the entry
 */
public class RegistryEntry<T> implements NonNullSupplier<T> {
	private static final RegistryEntry<?> EMPTY = new RegistryEntry(null, RegistryObject.empty());
	private final AbstractRegistrate<?> owner;
	@Nullable
	private final RegistryObject<T> delegate;
	
	@SuppressWarnings("unused")
	public RegistryEntry(AbstractRegistrate<?> owner, RegistryObject<T> delegate) {
		if (EMPTY != null && owner == null) throw new NullPointerException("Owner must not be null");
		if (EMPTY != null && delegate == null) throw new NullPointerException("Delegate must not be null");
		this.owner = owner;
		this.delegate = delegate;
	}
	
	public static <T> RegistryEntry<T> empty() {
		@SuppressWarnings("unchecked")
		RegistryEntry<T> t = (RegistryEntry<T>) EMPTY;
		return t;
	}
	
	@SuppressWarnings("unchecked")
	protected static <E extends RegistryEntry<?>> E cast(Class<? super E> clazz, RegistryEntry<?> entry) {
		if (clazz.isInstance(entry)) {
			return (E) entry;
		}
		throw new IllegalArgumentException("Could not convert RegistryEntry: expecting " + clazz + ", found " + entry.getClass());
	}
	
	/**
	 * Update the underlying entry manually from the given registry.
	 *
	 * @param registry The registry to pull the entry from.
	 */
	public void updateReference(Registry<? super T> registry) {
		RegistryObject<T> delegate = this.delegate;
		Objects.requireNonNull(delegate, "Registry entry is empty").updateReference(registry);
	}
	
	/**
	 * Get the entry, throwing an exception if it is not present for any reason.
	 *
	 * @return The (non-null) entry
	 */
	@Override
	@NonnullType
	public T get() {
		RegistryObject<T> delegate = this.delegate;
		return Objects.requireNonNull(getUnchecked(), () -> delegate == null ? "Registry entry is empty" : "Registry entry not present: " + delegate.getId());
	}
	
	/**
	 * Get the entry without performing any checks.
	 *
	 * @return The (nullable) entry
	 */
	@Nullable
	public T getUnchecked() {
		RegistryObject<T> delegate = this.delegate;
		return delegate == null ? null : delegate.orElse(null);
	}
	
	@SuppressWarnings("unchecked")
	public <R, E extends R> RegistryEntry<E> getSibling(Class<? super R> registryType) {
		return this == EMPTY ? empty() : owner.get(getId().getPath(), (Class<R>) registryType);
	}
	
	public <R, E extends R> RegistryEntry<E> getSibling(Registry<R> registry) {
		return getSibling(RegistryUtil.getRegistrationClass(registry));
	}
	
	/**
	 * If an entry is present, and the entry matches the given predicate, return an {@link RegistryEntry} describing the value, otherwise return an empty {@link RegistryEntry}.
	 *
	 * @param predicate a {@link Predicate predicate} to apply to the entry, if present
	 * @return an {@link RegistryEntry} describing the value of this {@link RegistryEntry} if the entry is present and matches the given predicate, otherwise an empty {@link RegistryEntry}
	 * @throws NullPointerException if the predicate is null
	 */
	public RegistryEntry<T> filter(Predicate<? super T> predicate) {
		Objects.requireNonNull(predicate);
		if (!isPresent() || predicate.test(get())) {
			return this;
		}
		return empty();
	}
	
	public <R> boolean is(R entry) {
		return get() == entry;
	}
	
	@Override
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof final RegistryEntry<?> other)) return false;
		if (!other.canEqual(this)) return false;
		return Objects.equals(this.delegate, other.delegate);
	}
	
	protected boolean canEqual(final Object other) {
		return other instanceof RegistryEntry;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $delegate = this.delegate;
		result = result * PRIME + ($delegate == null ? 43 : $delegate.hashCode());
		return result;
	}
	
	public Identifier getId() {
		return this.delegate.getId();
	}
	
	public Stream<T> stream() {
		return this.delegate.stream();
	}
	
	public boolean isPresent() {
		return this.delegate.isPresent();
	}
	
	public void ifPresent(final Consumer<? super T> consumer) {
		this.delegate.ifPresent(consumer);
	}
	
	public <U> Optional<U> map(final Function<? super T, ? extends U> mapper) {
		return this.delegate.map(mapper);
	}
	
	public <U> Optional<U> flatMap(final Function<? super T, Optional<U>> mapper) {
		return this.delegate.flatMap(mapper);
	}
	
	public <U> Supplier<U> lazyMap(final Function<? super T, ? extends U> mapper) {
		return this.delegate.lazyMap(mapper);
	}
	
	public T orElse(final T other) {
		return this.delegate.orElse(other);
	}
	
	public T orElseGet(final Supplier<? extends T> other) {
		return this.delegate.orElseGet(other);
	}
	
	public <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier) throws X {
		return this.delegate.<X>orElseThrow(exceptionSupplier);
	}
	
	private interface Exclusions<T> {
		T get();
		
		RegistryObject<T> filter(Predicate<? super T> predicate);
		
		void updateReference(Registry<? extends T> registry);
	}
}
