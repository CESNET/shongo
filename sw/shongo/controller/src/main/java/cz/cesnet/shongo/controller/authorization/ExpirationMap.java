package cz.cesnet.shongo.controller.authorization;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Represents a map of {@link V} by {@link K} with {@link #expiration}.
 *
 * @param <K>
 * @param <V>
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
class ExpirationMap<K, V> implements Iterable<V>
{
    /**
     * Cache of {@link V} by {@link K}.
     */
    private Map<K, Entry<V>> entries = new HashMap<K, Entry<V>>();

    /**
     * Specifies expiration for the {@link #entries}.
     */
    private Duration expiration = null;

    /**
     * @param expiration sets the {@link #expiration}
     */
    public void setExpiration(Duration expiration)
    {
        this.expiration = expiration;
    }

    /**
     * @param key
     * @return {@link V} by given {@code key}
     */
    public synchronized V get(K key)
    {
        Entry<V> entry = entries.get(key);
        if (entry != null) {
            if (entry.expirationDateTime == null || entry.expirationDateTime.isAfter(DateTime.now())) {
                return entry.value;
            }
            else {
                entries.remove(key);
            }
        }
        return null;
    }

    /**
     * Put given {@code value} to the cache by the given {@code key}.
     *
     * @param key
     * @param value
     */
    public synchronized void put(K key, V value)
    {
        Entry<V> entry = entries.get(key);
        if (entry == null) {
            entry = new Entry<V>();
            entries.put(key, entry);
        }
        if (expiration != null) {
            entry.expirationDateTime = DateTime.now().plus(expiration);
        }
        else {
            entry.expirationDateTime = null;
        }
        entry.value = value;
    }

    /**
     * Remove given {@code key}.
     *
     * @param key
     */
    public synchronized void remove(K key)
    {
        entries.remove(key);
    }

    /**
     * @return number of added keys
     */
    public synchronized int size()
    {
        return entries.size();
    }

    /**
     * Clear all {@link #entries}.
     */
    public synchronized void clear()
    {
        entries.clear();
    }

    /**
     * Entry for {@link ExpirationMap}.
     */
    private static class Entry<V>
    {
        /**
         * Expiration {@link org.joda.time.DateTime}.
         */
        private DateTime expirationDateTime;

        /**
         * Value.
         */
        private V value;
    }

    @Override
    public Iterator<V> iterator()
    {
        final Iterator<Entry<V>> iterator = entries.values().iterator();
        return new Iterator<V>()
        {
            @Override
            public boolean hasNext()
            {
                return iterator.hasNext();
            }

            @Override
            public V next()
            {
                return iterator.next().value;
            }

            @Override
            public void remove()
            {
                iterator.remove();
            }
        };
    }
}
