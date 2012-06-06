package cz.cesnet.shongo.controller.resource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

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
     * Technology of aliases.
     */
    private Technology technology;

    /**
     * Type of aliases.
     */
    private Type type;

    /**
     * Range starting value.
     */
    private String startValue;

    /**
     * Range end value.
     */
    private String endValue;

    /**
     * @return {@link #technology}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Technology getTechnology()
    {
        return technology;
    }

    /**
     * @param technology sets the {@link #technology}
     */
    public void setTechnology(Technology technology)
    {
        this.technology = technology;
    }

    /**
     * @return {@link #type}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

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
