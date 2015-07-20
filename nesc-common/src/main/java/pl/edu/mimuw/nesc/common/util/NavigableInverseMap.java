package pl.edu.mimuw.nesc.common.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>A map that maintains an inverse map to its own contents. The inverse map
 * maps values of the map to sets of keys and it is navigable. The sets of keys
 * are themselves navigable.</p>
 *
 * <p>This map does not permit <code>null</code> keys and <code>null</code>
 * values.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class NavigableInverseMap<K, V> implements Map<K, V> {
    /**
     * Map that contains the entries of this map.
     */
    private final Map<K, V> mapping = new HashMap<>();

    /**
     * The inverse of this map.
     */
    private final NavigableMap<V, NavigableSet<K>> inverseMapping = new TreeMap<>();

    /**
     * Unmodifiable view of the entry set of this map.
     */
    private final Set<Map.Entry<K, V>> unmodifiableEntrySet = Collections.unmodifiableSet(mapping.entrySet());

    /**
     * Unmodifiable view of the key set of this map.
     */
    private final Set<K> unmodifiableKeySet = Collections.unmodifiableSet(mapping.keySet());

    /**
     * Unmodifiable view of the values set of this map.
     */
    private final Collection<V> unmodifiableValuesSet = Collections.unmodifiableCollection(mapping.values());

    @Override
    public void clear() {
        mapping.clear();
        inverseMapping.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return mapping.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return mapping.containsValue(value);
    }

    /**
     * Get the entry set of this map. It is unmodifiable. Each entry supports
     * the {@link Entry#setValue} method but its behaviour is undefined.
     *
     * @return Unmodifiable view of the entry set of this map.
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return unmodifiableEntrySet;
    }

    @Override
    public boolean equals(Object other) {
        return mapping.equals(other);
    }

    @Override
    public V get(Object key) {
        checkNotNull(key, "key cannot be null");
        return mapping.get(key);
    }

    @Override
    public int hashCode() {
        return mapping.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return mapping.isEmpty();
    }

    /**
     * Get the set with all keys present in this map. The returned view is
     * unmodifiable.
     *
     * @return Unmodifiable view of keys in this map.
     */
    @Override
    public Set<K> keySet() {
        return unmodifiableKeySet;
    }

    @Override
    public V put(K key, V value) {
        checkNotNull(key, "key cannot be null");
        checkNotNull(value, "value cannot be null");

        // Associate the value with the given key and save the old value
        final V previousValue = mapping.put(key, value);

        // Remove the old value from the inverse view
        if (previousValue != null && !previousValue.equals(value)) {
            final NavigableSet<K> keys = inverseMapping.get(previousValue);
            if (!keys.remove(key)) {
                throw new RuntimeException("the inverse map does not contain the mapping");
            }
            if (keys.isEmpty()) {
                if (inverseMapping.remove(previousValue) != keys) {
                    throw new RuntimeException("invalid key removed in the inverse map");
                }
            }
        }

        // Add the new value to the inverse view
        if (previousValue == null || !previousValue.equals(value)) {
            final NavigableSet<K> keysForValue;
            if (!inverseMapping.containsKey(value)) {
                keysForValue = new TreeSet<>();
                inverseMapping.put(value, keysForValue);
            } else {
                keysForValue = inverseMapping.get(value);
            }
            if (!keysForValue.add(key)) {
                throw new RuntimeException("the given key has been already present in the inverse view");
            }
        }

        return previousValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> otherMap) {
        // Check the map
        checkNotNull(otherMap, "the map cannot be null");
        checkArgument(!otherMap.keySet().contains(null), "the other map contains a null key");
        checkArgument(!otherMap.values().contains(null), "the other map contains a null value");

        // Add all entries
        for (Map.Entry<? extends K, ? extends V> entry : otherMap.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        checkNotNull(key, "key cannot be null");

        final V previousValue = mapping.remove(key);

        if (previousValue != null) {
            final NavigableSet<K> keysForValue = inverseMapping.get(previousValue);
            if (!keysForValue.remove(key)) {
                throw new RuntimeException("the inverse map does not contain the mapping");
            }
            if (keysForValue.isEmpty()) {
                if (inverseMapping.remove(previousValue) != keysForValue) {
                    throw new RuntimeException("invalid set of keys for the value while removing a mapping");
                }
            }
        }

        return previousValue;
    }

    @Override
    public int size() {
        return mapping.size();
    }

    /**
     * Get a collection with all values contained in this map. It is an
     * unmodifiable view.
     *
     * @return Unmodifiable collection of values in this map.
     */
    @Override
    public Collection<V> values() {
        return unmodifiableValuesSet;
    }

    /**
     * Get the inverse map to this map. The map and sets contained in it shall
     * not be modified. However, if they are, the behaviour is undefined.
     *
     * @return The inverse map.
     */
    public NavigableMap<V, NavigableSet<K>> inverseMap() {
        return inverseMapping;
    }
}
