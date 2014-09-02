package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 * {@link AbstractRequest} for listing objects.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ListRequest extends AbstractRequest
{
    /**
     * Index of first item which should be fetched.
     */
    private Integer start;

    /**
     * Number of items starting at {@link #start} which should be fetched.
     */
    private Integer count;

    /**
     * Constructor.
     */
    public ListRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param start sets the {@link #start}
     * @param count sets the {@link #count}
     */
    public ListRequest(Integer start, Integer count)
    {
        this.start = start;
        this.count = count;
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public ListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    /**
     * @param minStart minimum {@link #start} which can be returned
     * @return {@link #start}
     */
    public int getStart(int minStart)
    {
        if (start == null || start < minStart) {
            return minStart;
        }
        return start;
    }

    /**
     * @param minStart minimum {@link #start} which can be returned
     * @param maxCount number of items to determine maximum {@link #start} which can be returned
     * @return {@link #start}
     */
    public int getStart(Integer minStart, Integer maxCount)
    {
        int start = getStart(minStart);
        int maxIndex = Math.max(0, maxCount- 1);
        if (start > maxIndex) {
            return maxIndex;
        }
        return start;
    }

    /**
     * @param start sets the {@link #start}
     */
    public void setStart(Integer start)
    {
        this.start = start;
    }

    /**
     * @return {@link #count} or {@code -1} if it is null
     */
    public int getCount()
    {
        if (count == null || count < 0) {
            return -1;
        }
        return count;
    }

    /**
     * @param maxCount maximum {@link #count} which can be returned
     * @return {@link #count}
     */
    public int getCount(int maxCount)
    {
        if (count == null || count < 0 || (count > maxCount)) {
            return maxCount;
        }
        return count;
    }

    /**
     * @param count sets the {@link #count}
     */
    public void setCount(Integer count)
    {
        this.count = count;
    }

    private static final String START = "start";
    private static final String COUNT = "count";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(START, start);
        dataMap.set(COUNT, count);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        start = dataMap.getInteger(START);
        count = dataMap.getInteger(COUNT);
    }
}
