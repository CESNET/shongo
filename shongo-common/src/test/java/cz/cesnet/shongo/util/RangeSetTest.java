package cz.cesnet.shongo.util;

import cz.cesnet.shongo.util.RangeSet;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * Tests for {@link cz.cesnet.shongo.util.RangeSet}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@SuppressWarnings("unchecked")
public class RangeSetTest
{
    @Test
    public void test() throws Exception
    {
        RangeSet<Integer, Integer> rangeSet = new RangeSet<Integer, Integer>();

        rangeSet.clear();
        rangeSet.add(1, 50, 150);
        rangeSet.add(2, 100, 200);
        assertArrayEquals(new RangeSet.Bucket[]{
                new RangeSet.Bucket(50, new Object[]{1}),
                new RangeSet.Bucket(100, new Object[]{1, 2}),
                new RangeSet.Bucket(150, new Object[]{2}),
                new RangeSet.Bucket(200)
        }, rangeSet.getBuckets().toArray());

        rangeSet.clear();
        rangeSet.add(1, 0, 500);
        rangeSet.add(2, 100, 400);
        rangeSet.add(3, 200, 300);
        rangeSet.add(4, 240, 260);

        assertArrayEquals(new RangeSet.Bucket[]{
                new RangeSet.Bucket(0, new Object[]{1}),
                new RangeSet.Bucket(100, new Object[]{1, 2}),
                new RangeSet.Bucket(200, new Object[]{1, 2, 3}),
                new RangeSet.Bucket(240, new Object[]{1, 2, 3, 4}),
                new RangeSet.Bucket(260, new Object[]{1, 2, 3}),
                new RangeSet.Bucket(300, new Object[]{1, 2}),
                new RangeSet.Bucket(400, new Object[]{1}),
                new RangeSet.Bucket(500)
        }, rangeSet.getBuckets().toArray());

        rangeSet.clear();
        rangeSet.add(1, 0, 200);
        rangeSet.add(2, 100, 300);
        rangeSet.add(3, 150, 250);
        assertArrayEquals(new RangeSet.Bucket[]{
                new RangeSet.Bucket(0, new Object[]{1}),
                new RangeSet.Bucket(100, new Object[]{1, 2}),
                new RangeSet.Bucket(150, new Object[]{1, 2, 3}),
                new RangeSet.Bucket(200, new Object[]{2, 3}),
                new RangeSet.Bucket(250, new Object[]{2}),
                new RangeSet.Bucket(300),
        }, rangeSet.getBuckets().toArray());
        assertArrayEquals(new Object[]{1, 2}, rangeSet.getValues(100, 150).toArray());
        assertArrayEquals(new Object[]{1, 2, 3}, rangeSet.getValues(100, 151).toArray());

        rangeSet.clear();
        rangeSet.add(1, 100, 500);
        rangeSet.add(2, 100, 400);
        rangeSet.add(3, 200, 300);
        rangeSet.add(4, 240, 260);
        rangeSet.remove(1);
        rangeSet.remove(3);
        assertArrayEquals(new RangeSet.Bucket[]{
                new RangeSet.Bucket(100, new Object[]{2}),
                new RangeSet.Bucket(240, new Object[]{2, 4}),
                new RangeSet.Bucket(260, new Object[]{2}),
                new RangeSet.Bucket(400),
        }, rangeSet.getBuckets().toArray());
    }

    @Test
    public void testEmpty() throws Exception
    {
        RangeSet<Integer, Integer> rangeSet = new RangeSet<Integer, Integer>();
        rangeSet.add(1, 50, 50);
        rangeSet.remove(1);
    }
}
