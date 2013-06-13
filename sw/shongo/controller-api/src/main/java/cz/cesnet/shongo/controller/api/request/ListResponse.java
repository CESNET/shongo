package cz.cesnet.shongo.controller.api.request;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ListResponse<T> extends AbstractResponse
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

    public ListResponse(Class<T> itemClass)
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
}
