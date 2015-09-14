package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.Group;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link AbstractResponse} for listing objects.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ListResponse<T> extends AbstractResponse implements Iterable<T>
{
    /**
     * Index of the first item.
     */
    private int start = 0;

    /**
     * Total number of items available (but {@link #items} can contain only portion of them based on request).
     */
    private int count = 0;

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

    public void addAll(ListResponse<T> items)
    {
        this.items.addAll(items.getItems());
    }

    public void addAll(List<T> items)
    {
        this.items.addAll(items);
    }

    @Override
    public Iterator<T> iterator()
    {
        return items.iterator();
    }

    private static final String START = "start";
    private static final String COUNT = "count";
    private static final String ITEMS = "items";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(START, start);
        dataMap.set(COUNT, count);
        dataMap.set(ITEMS, items);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        start = dataMap.getInt(START);
        count = dataMap.getInt(COUNT);
        items = (List<T>) dataMap.getList(ITEMS, Object.class);
    }

    @Override
    public String toString()
    {
        StringBuilder output = new StringBuilder();
        output.append("ListResponse(start: ");
        output.append(start);
        output.append(", count: ");
        output.append(count);
        output.append("):");
        for (T item : items) {
            output.append("\n -");
            output.append(item);
        }
        return output.toString();
    }

    public static <T> ListResponse<T> fromRequest(ListRequest request, List<T> data)
    {
        int start = request.getStart(0, data.size());
        int end = start + request.getCount(data.size() - start);
        ListResponse<T> response = new ListResponse<T>();
        response.setStart(start);
        response.setCount(data.size());
        for (T item : data.subList(start, end)) {
            response.addItem(item);
        }
        return response;
    }

    public static <T> ListResponse<T> fromRequest(Integer start, Integer count, List<T> data)
    {
        return fromRequest(new ListRequest(start, count), data);
    }
}
