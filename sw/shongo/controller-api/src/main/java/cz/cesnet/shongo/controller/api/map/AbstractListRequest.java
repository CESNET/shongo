package cz.cesnet.shongo.controller.api.map;

import cz.cesnet.shongo.map.DataMap;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AbstractListRequest extends AbstractRequest
{
    /**
     * Index of first item which should be fetched.
     */
    private Integer start;

    /**
     * Number of items starting at {@link #start} which should be fetched.
     */
    private Integer count;

    public Integer getStart(Integer defaultStart)
    {
        return (start != null ? start : defaultStart);
    }

    public void setStart(Integer start)
    {
        this.start = start;
    }

    public Integer getCount(Integer defaultCount)
    {
        return (count != null ? count : count);
    }

    public void setCount(Integer count)
    {
        this.count = count;
    }

    @Override
    public DataMap toData()
    {
        DataMap data = super.toData();
        data.set("start", start);
        data.set("count", count);
        return data;
    }

    @Override
    public void fromData(DataMap data)
    {
        super.fromData(data);
        start = data.getInteger("start");
        count = data.getInteger("count");
    }
}
