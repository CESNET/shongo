package cz.cesnet.shongo.controller.api.map;

import cz.cesnet.shongo.map.AbstractObject;
import cz.cesnet.shongo.map.DataMap;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AbstractListResponse<T> extends AbstractObject
{
    /**
     * Index of the first item.
     */
    private int start;

    /**
     * Total number of items available.
     */
    private int count;

    /**
     * List of fetched items.
     */
    private List<T> items = new LinkedList<T>();

    private Class<T> itemClass;

    public AbstractListResponse(Class<T> itemClass)
    {
        this.itemClass = itemClass;
    }

    public int getStart()
    {
        return start;
    }

    public void setStart(int start)
    {
        this.start = start;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    /**
     * @return {@link #items}
     */
    public List<T> getItems()
    {
        return Collections.unmodifiableList(items);
    }

    /**
     * @param item to be added to {@link #items}
     */
    public void addItem(T item)
    {
        items.add(item);
    }

    @Override
    public DataMap toData()
    {
        DataMap data = super.toData();
        data.set("start", start);
        data.set("count", count);
        data.set("items", items);
        return data;
    }

    @Override
    public void fromData(DataMap data)
    {
        super.fromData(data);
        start = data.getInt("start");
        count = data.getInt("count");
        items.clear();
        for (T item : data.getCollection("items", itemClass)) {
            items.add(item);
        }
    }
}
