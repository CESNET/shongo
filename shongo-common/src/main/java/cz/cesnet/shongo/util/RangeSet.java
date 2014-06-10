package cz.cesnet.shongo.util;

import org.joda.time.DateTime;

import java.util.*;

/**
 * Set of values. Each value has assigned range to which it belongs. Methods for retrieving values based on
 * whether they belongs to specific range are provided.
 *
 * @param <V> type of values in the set
 * @param <R> type of ranges
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RangeSet<V, R> implements Iterable<V>
{
    /**
     * Map of buckets accessible by range values.
     */
    NavigableMap<R, Bucket<R, V>> bucketMap = new TreeMap<R, Bucket<R, V>>();

    /**
     * Map ranges accessible by values.
     */
    Map<V, Range> rangeMap = new HashMap<V, Range>();

    /**
     * Add new value to the set.
     *
     * @param value rangeValue to be added
     * @param start start of rangeValue's range
     * @param end   end of rangeValue's range
     * @return true if value was added,
     *         false when the value already exists
     */
    public boolean add(V value, R start, R end)
    {
        // If rangeValue is already added, do nothing
        if (rangeMap.containsKey(value)) {
            return false;
        }

        // Find the nearest start/end and update buckets
        R floor = bucketMap.floorKey(start);
        R ceiling = bucketMap.ceilingKey(end);
        if (!start.equals(floor)) {
            Bucket<R, V> newBucket = createBucket(start);
            newBucket.addOwnerValue(value);
            bucketMap.put(start, newBucket);
            if (floor != null) {
                for (V floorValue : bucketMap.get(floor)) {
                    newBucket.add(floorValue);
                }
            }
        }
        else {
            bucketMap.get(start).addOwnerValue(value);
        }
        if (!end.equals(ceiling)) {
            Bucket<R, V> newBucket = createBucket(end);
            newBucket.addOwnerValue(value);
            bucketMap.put(end, newBucket);
            if (ceiling != null) {
                for (V ceilingValue : bucketMap.lowerEntry(end).getValue()) {
                    newBucket.add(ceilingValue);
                }
            }
        }
        else {
            bucketMap.get(end).addOwnerValue(value);
        }

        // Add rangeValue to proper buckets
        NavigableMap<R, Bucket<R, V>> subMap = bucketMap.subMap(start, true, end, false);
        for (NavigableMap.Entry<R, Bucket<R, V>> entry : subMap.entrySet()) {
            entry.getValue().add(value);
        }

        // Keep range for rangeValue
        rangeMap.put(value, new Range(start, end));

        return true;
    }


    /**
     * Remove owner value from bucket and if bucket hasn't any owner remove it from the set.
     *
     * @param bucket
     * @param ownerValue
     */
    private void removeBucketOwnerValue(Bucket<R, V> bucket, V ownerValue)
    {
        bucket.removeOwnerValue(ownerValue);
        if (bucket.hasNoneOwners()) {
            bucketMap.remove(bucket.getRangeValue());
        }
    }

    /**
     * Remove given value from the set.
     *
     * @param value
     * @return true if the value was removed,
     *         false if the value doesn't exist
     */
    public boolean remove(V value)
    {
        Range range = rangeMap.get(value);
        if (range == null) {
            return false;
        }

        // Remove rangeValue from buckets
        NavigableMap<R, Bucket<R, V>> subMap = bucketMap.subMap(range.getStart(), true, range.getEnd(), false);
        for (NavigableMap.Entry<R, Bucket<R, V>> entry : subMap.entrySet()) {
            entry.getValue().remove(value);
        }

        // Remove owner from buckets
        R start = range.getStart();
        R end = range.getEnd();
        removeBucketOwnerValue(bucketMap.get(start), value);
        if (!start.equals(end)) {
            removeBucketOwnerValue(bucketMap.get(end), value);
        }

        rangeMap.remove(value);

        return true;
    }

    /**
     * Remove all values from the set.
     */
    public void clear()
    {
        bucketMap.clear();
        rangeMap.clear();
    }

    /**
     * @param start
     * @param end
     * @return set of values which intersects the given range
     */
    public Set<V> getValues(R start, R end)
    {
        Set<V> values = new HashSet<V>();
        if (!bucketMap.isEmpty()) {
            R floor = bucketMap.floorKey(start);
            R ceiling = bucketMap.ceilingKey(end);
            if (floor == null) {
                floor = bucketMap.firstKey();
            }
            if (ceiling == null) {
                ceiling = bucketMap.lastKey();
            }
            NavigableMap<R, Bucket<R, V>> subMap = bucketMap.subMap(floor, true, ceiling, false);
            for (Bucket<R, V> bucket : subMap.values()) {
                for (V value : bucket) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    /**
     * @param start
     * @param end
     * @return collection of {@link Bucket}s the given range
     */
    public Collection<Bucket<R, V>> getBuckets(R start, R end)
    {
        if (!bucketMap.isEmpty()) {
            R floor = bucketMap.floorKey(start);
            R ceiling = bucketMap.ceilingKey(end);
            if (floor == null) {
                floor = bucketMap.firstKey();
            }
            if (ceiling == null) {
                ceiling = bucketMap.lastKey();
            }
            NavigableMap<R, Bucket<R, V>> subMap = bucketMap.subMap(floor, true, ceiling, false);
            return subMap.values();
        }
        else {
            return Collections.emptyList();
        }
    }

    /**
     * @param start
     * @param end
     * @param bucketClass
     * @return collection of {@link Bucket}s the given range
     */
    public <B extends Bucket<R, V>> Collection<B> getBuckets(R start, R end, Class<B> bucketClass)
    {
        return (Collection<B>) getBuckets(start, end);
    }

    /**
     * @param rangeValue
     * @return new instance of {@link Bucket}
     */
    protected Bucket<R, V> createBucket(R rangeValue)
    {
        return new Bucket<R, V>(rangeValue);
    }

    /**
     * @return collection of buckets from the set
     */
    protected Collection<Bucket<R, V>> getBuckets()
    {
        return bucketMap.values();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("RangeSet");
        for (NavigableMap.Entry<R, Bucket<R, V>> entry : bucketMap.entrySet()) {
            builder.append("\n");
            builder.append(entry.getKey().toString());
            builder.append(" (");
            boolean separate = false;
            for (V value : entry.getValue()) {
                if (separate) {
                    builder.append(", ");
                }
                builder.append(value.toString());
                separate = true;
            }
            builder.append(")");
        }
        return builder.toString();
    }

    /**
     * @return number of values in set
     */
    public int size()
    {
        return rangeMap.size();
    }

    @Override
    public Iterator<V> iterator()
    {
        return rangeMap.keySet().iterator();
    }

    /**
     * Represents a range unit. It covers the range from it's {@link #rangeValue} to {@link #rangeValue} of the
     * following {@link Bucket}. Each range unit can contain multiple values. The bucket must also hold the owning
     * values which specify exactly the {@link #rangeValue}.
     *
     * @param <R>
     * @param <V>
     */
    public static class Bucket<R, V> extends HashSet<V>
    {
        /**
         * Starting range value.
         */
        private R rangeValue;

        /**
         * Set of owner values. When empty, the bucket is not needed.
         */
        private Set<V> ownerValues = new HashSet<V>();

        /**
         * Constructor
         *
         * @param rangeValue
         */
        public Bucket(R rangeValue)
        {
            this.rangeValue = rangeValue;
        }

        /**
         * Constructor.
         *
         * @param rangeValue
         * @param values
         */
        public Bucket(R rangeValue, V[] values)
        {
            this.rangeValue = rangeValue;
            for (V value : values) {
                this.add(value);
            }
        }

        /**
         * @return {@link #rangeValue}
         */
        public R getRangeValue()
        {
            return rangeValue;
        }

        /**
         * @param value owner value to be added to the {@link #ownerValues}
         */
        public void addOwnerValue(V value)
        {
            ownerValues.add(value);
        }

        /**
         * @param value owner value to be removed from the {@link #ownerValues}
         */
        public void removeOwnerValue(V value)
        {
            ownerValues.remove(value);
        }

        /**
         * @return true if the {@link #ownerValues} is empty,
         *         false otherwise
         */
        public boolean hasNoneOwners()
        {
            return ownerValues.isEmpty();
        }

        @Override
        public boolean equals(Object object)
        {
            if (object instanceof Bucket && super.equals(object) && rangeValue.equals(((Bucket) object).rangeValue)) {
                return true;
            }
            return false;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            for (V value : this) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(value.toString());
            }
            return rangeValue.toString() + "[" + builder.toString() + "]";
        }
    }

    /**
     * Represents a range of a value in the set.
     */
    private class Range
    {
        /**
         * Start of the range.
         */
        private final R start;

        /**
         * End of the range.
         */
        private final R end;

        /**
         * Constructor.
         *
         * @param start sets the {@link #start}
         * @param end   sets the {@link #end}
         */
        public Range(R start, R end)
        {
            this.start = start;
            this.end = end;
        }

        /**
         * @return {@link #start}
         */
        public R getStart()
        {
            return start;
        }

        /**
         * @return {@link #end}
         */
        public R getEnd()
        {
            return end;
        }
    }
}
