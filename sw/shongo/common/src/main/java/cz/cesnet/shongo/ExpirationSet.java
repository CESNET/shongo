package cz.cesnet.shongo;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Represents a set of {@link V} with {@link #expiration}.
 *
 * @param <V>
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExpirationSet<V> implements Iterable<V>
{
    /**
     * Cache of {@link V}.
     */
    private Map<V, Entry> entries = new HashMap<V, Entry>();

    /**
     * Specifies expiration for the {@link #entries}.
     */
    private Duration expiration = null;

    /**
     * Constructor.
     */
    public ExpirationSet()
    {
    }

    /**
     * Constructor.
     *
     * @param expiration sets the {@link #expiration}
     */
    public ExpirationSet(Duration expiration)
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
     * @param value
     * @return true if given {@code key} exists, false otherwise
     */
    public synchronized boolean contains(V value)
    {
        Entry entry = entries.get(value);
        if (entry != null) {
            if (entry.expirationDateTime == null || entry.expirationDateTime.isAfter(DateTime.now())) {
                return true;
            }
            else {
                entries.remove(value);
            }
        }
        return false;
    }

    /**
     * Put given {@code value} to the cache by the given {@code key}.
     *
     * @param value
     */
    public synchronized void add(V value)
    {
        Entry entry = entries.get(value);
        if (entry == null) {
            entry = new Entry();
            entries.put(value, entry);
        }
        if (expiration != null) {
            entry.expirationDateTime = DateTime.now().plus(expiration);
        }
        else {
            entry.expirationDateTime = null;
        }
    }

    /**
     * Remove given {@code key}.
     *
     * @param value
     * @return removed value for the {@code key} or null
     */
    public synchronized void remove(V value)
    {
        entries.remove(value);
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
     * Remove all expired values.
     *
     * @param dateTime which represents "now"
     */
    public void clearExpired(DateTime dateTime)
    {
        Iterator<Map.Entry<V, Entry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<V, Entry> itemEntry = iterator.next();
            Entry entry = itemEntry.getValue();
            if (entry.expirationDateTime != null && !entry.expirationDateTime.isAfter(dateTime)) {
                iterator.remove();
            }
        }
    }

    /**
     * Entry for {@link cz.cesnet.shongo.ExpirationSet}.
     */
    private static class Entry
    {
        /**
         * Expiration {@link org.joda.time.DateTime}.
         */
        private DateTime expirationDateTime;
    }

    @Override
    public Iterator<V> iterator()
    {
        clearExpired(DateTime.now());
        return entries.keySet().iterator();
    }
}
