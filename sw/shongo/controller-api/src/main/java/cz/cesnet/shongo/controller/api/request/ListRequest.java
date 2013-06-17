package cz.cesnet.shongo.controller.api.request;

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
     * @param securityToken sets the {@link #securityToken}
     */
    public ListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    /**
     * @return {@link #start}
     */
    public Integer getStart()
    {
        return start;
    }

    /**
     * @param defaultStart to be returned if {@link #start} is null
     * @return {@link #start} of {@code defaultStart}
     */
    public Integer getStart(Integer defaultStart)
    {
        return (start != null ? start : defaultStart);
    }

    /**
     * @param start sets the {@link #start}
     */
    public void setStart(Integer start)
    {
        this.start = start;
    }

    /**
     * @return {@link #count}
     */
    public Integer getCount()
    {
        return count;
    }

    /**
     * @param defaultCount to be returned if {@link #count} is null
     * @return {@link #count} of {@code defaultCount}
     */
    public Integer getCount(Integer defaultCount)
    {
        return (count != null ? count : defaultCount);
    }

    /**
     * @param count sets the {@link #count}
     */
    public void setCount(Integer count)
    {
        this.count = count;
    }
}
