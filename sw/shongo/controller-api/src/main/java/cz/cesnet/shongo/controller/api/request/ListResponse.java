package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.annotation.Transient;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link AbstractResponse} for listing objects.
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
     * Total number of items available (but {@link #items} can contain only portion of them based on request).
     */
    private int count;

    /**
     * List of fetched items.
     */
    private List<T> items = new LinkedList<T>();

    /**
     * Constructor.
     */
    public ListResponse()
    {
    }

    /**
     * @return {@link #start}
     */
    public int getStart()
    {
        return start;
    }

    /**
     * @param start sets the {@link #start}
     */
    public void setStart(int start)
    {
        this.start = start;
    }

    /**
     * @return {@link #count}
     */
    public int getCount()
    {
        return count;
    }

    /**
     * @param count sets the {@link #count}
     */
    public void setCount(int count)
    {
        this.count = count;
    }

    /**
     * @return {@link #items}
     */
    public List<T> getItems()
    {
        return items;
    }

    /**
     * @param index
     * @return item at {@code index}
     */
    public T getItem(int index)
    {
        return items.get(index);
    }

    /**
     * @return size of {@link #items}
     */
    @Transient
    public int getItemCount()
    {
        return items.size();
    }

    /**
     * @param item to be added to {@link #items}
     */
    public void addItem(T item)
    {
        items.add(item);
    }
}
