package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 * {@link ListRequest} which can be sorted.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SortableListRequest<T extends Enum> extends ListRequest
{
    /**
     * Enum class of attributes by which the response should be sorted.
     */
    private Class<T> sortClass;

    /**
     * Attribute by which the response should by sorted.
     */
    private T sort;

    /**
     * Specifies whether sorting should be descending, otherwise it is ascending.
     */
    private Boolean sortDescending;

    /**
     * Constructor.
     *
     * @param sortClass sets the {@link #sortClass}
     */
    public SortableListRequest(Class<T> sortClass)
    {
        this.sortClass = sortClass;
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param sortClass sets the {@link #sortClass}
     */
    public SortableListRequest(Class<T> sortClass, SecurityToken securityToken)
    {
        super(securityToken);
        this.sortClass = sortClass;
    }

    /**
     * @return {@link #sort}
     */
    public T getSort()
    {
        return sort;
    }

    /**
     * @param sort sets the {@link #sort}
     */
    public void setSort(T sort)
    {
        this.sort = sort;
    }

    /**
     * @return {@link #sortDescending}
     */
    public Boolean getSortDescending()
    {
        return sortDescending;
    }

    /**
     * @param sortDescending sets the {@link #sortDescending}
     */
    public void setSortDescending(Boolean sortDescending)
    {
        this.sortDescending = sortDescending;
    }

    private static final String SORT = "sort";
    private static final String SORT_DESCENDING = "sortDescending";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(SORT, sort);
        dataMap.set(SORT_DESCENDING, sortDescending);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        sort = (T) dataMap.getEnum(SORT, sortClass);
        sortDescending = dataMap.getBool(SORT_DESCENDING);
    }
}
