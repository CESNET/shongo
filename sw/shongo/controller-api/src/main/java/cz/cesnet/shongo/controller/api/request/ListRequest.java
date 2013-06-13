package cz.cesnet.shongo.controller.api.request;

/**
 * TODO:
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
}
