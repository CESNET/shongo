package cz.cesnet.shongo.controller.resource;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Represents a special type of {@link AliasResource} which
 * can be allocated as aliases from an alias range.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasRangeResource extends AliasResource
{
    /**
     * Range starting value.
     */
    private String startValue;

    /**
     * Range end value.
     */
    private String endValue;

    /**
     * @return {@link #startValue}
     */
    @Column
    public String getStartValue()
    {
        return startValue;
    }

    /**
     * @param startValue sets the {@link #startValue}
     */
    public void setStartValue(String startValue)
    {
        this.startValue = startValue;
    }

    /**
     * @return {@link #endValue}
     */
    @Column
    public String getEndValue()
    {
        return endValue;
    }

    /**
     * @param endValue sets the {@link #endValue}
     */
    public void setEndValue(String endValue)
    {
        this.endValue = endValue;
    }
}
