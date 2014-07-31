package cz.cesnet.shongo;

import org.apache.commons.lang.NotImplementedException;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.persistence.Entity;
import java.util.*;

/**
 * Represents a map of {@link V} by {@link K} with {@link #expiration}.
 *
 * @param <K>
 * @param <V>
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExpirationMap<K, V> implements Iterable<V>
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
     * Constructor.
     */
    public ExpirationMap()
    {
    }

    /**
     * Constructor.
     *
     * @param expiration sets the {@link #expiration}
     */
    public ExpirationMap(Duration expiration)
    {
        setExpiration(expiration);
    }

    /**
     * @param expiration sets the {@link #expiration}
     */
    public void setExpiration(Duration expiration)
    {
        this.expiration = expiration;
    }

    /**
     * @param key
     * @return true if given {@code key} exists, false otherwise
     */
    public synchronized boolean contains(K key)
    {
        Entry<V> entry = entries.get(key);
        if (entry != null) {
            if (entry.expirationDateTime == null || entry.expirationDateTime.isAfter(DateTime.now())) {
                return true;
            }
            else {
                entries.remove(key);
            }
        }
        return false;
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
     * @return removed value for the {@code key} or null
     */
    public synchronized V remove(K key)
    {
        Entry<V> entry = entries.remove(key);
        if (entry != null) {
            return entry.value;
        }
        else {
            return null;
        }
    }

    /**
     * Removes all entries with given {@code value}.
     *
     * @param value
     */
    public void removeByValue(V value)
    {
        Iterator<Map.Entry<K, Entry<V>>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, Entry<V>> itemEntry = iterator.next();
            Entry<V> entry = itemEntry.getValue();
            if (entry.value.equals(value)) {
                iterator.remove();
            }
        }
    }

    /**
     * @return number of added keys
     */
    public synchronized int size()
    {
        return entries.size();
    }

    /**
     * @return true whether {@link #entries} are empty, false otherwise
     */
    public synchronized boolean isEmpty()
    {
        return entries.isEmpty();
    }

    /**
     * Clear all {@link #entries}.
     */
    public synchronized void clear()
    {
        entries.clear();
    }

    /**
     * {@link #entries}.
     */
    public synchronized Set<K> keySet()
    {
        return entries.keySet();
    }

    /**
     * {@link #entries}.
     */
    public synchronized Collection<V> values()
    {
        List<V> values = new LinkedList<V>();
        for (Entry<V> entry : entries.values()) {
            values.add(entry.value);
        }
        return values;
    }

    /**
     * Remove all expired values.
     *
     * @param dateTime which represents "now"
     */
    public synchronized List<Map.Entry<K, V>> clearExpired(DateTime dateTime)
    {
        List<Map.Entry<K, V>> cleared = new LinkedList<Map.Entry<K, V>>();
        Iterator<Map.Entry<K, Entry<V>>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, Entry<V>> itemEntry = iterator.next();
            Entry<V> entry = itemEntry.getValue();
            final K key = itemEntry.getKey();
            final V value = entry.value;
            if (dateTime == null || (entry.expirationDateTime != null && !entry.expirationDateTime.isAfter(dateTime))) {
                cleared.add(new Map.Entry<K, V>()
                {
                    @Override
                    public K getKey()
                    {
                        return key;
                    }

                    @Override
                    public V getValue()
                    {
                        return value;
                    }

                    @Override
                    public V setValue(V value)
                    {
                        throw new NotImplementedException();
                    }
                });
                iterator.remove();
            }
        }
        return cleared;
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
        clearExpired(DateTime.now());
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
