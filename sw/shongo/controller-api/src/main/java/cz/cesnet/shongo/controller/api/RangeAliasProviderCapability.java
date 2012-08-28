package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.annotation.Required;

/**
 * Capability tells that the resource can allocated can allocate aliases from an alias range.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RangeAliasProviderCapability extends Capability
{
    /**
     * Technology of aliases.
     */
    public static final String TECHNOLOGY = "technology";

    /**
     * Type of aliases.
     */
    public static final String TYPE = "type";

    /**
     * Range starting value.
     */
    public static final String START_VALUE = "startValue";

    /**
     * Range ending value.
     */
    public static final String END_VALUE = "endValue";

    /**
     * @return {@link #TECHNOLOGY}
     */
    @Required
    public Technology getTechnology()
    {
        return getPropertyStorage().getValue(TECHNOLOGY);
    }

    /**
     * @param technology sets the {@link #TECHNOLOGY}
     */
    public void setTechnology(Technology technology)
    {
        getPropertyStorage().setValue(TECHNOLOGY, technology);
    }

    /**
     * @return {@link #TYPE}
     */
    @Required
    public AliasType getType()
    {
        return getPropertyStorage().getValue(TYPE);
    }

    /**
     * @param type sets the {@link #TYPE}
     */
    public void setType(AliasType type)
    {
        getPropertyStorage().setValue(TYPE, type);
    }

    /**
     * @return {@link #START_VALUE}
     */
    @Required
    public String getStartValue()
    {
        return getPropertyStorage().getValue(START_VALUE);
    }

    /**
     * @param startValue sets the {@link #START_VALUE}
     */
    public void setStartValue(String startValue)
    {
        getPropertyStorage().setValue(START_VALUE, startValue);
    }

    /**
     * @return {@link #END_VALUE}
     */
    @Required
    public String getEndValue()
    {
        return getPropertyStorage().getValue(END_VALUE);
    }

    /**
     * @param endValue sets the {@link #END_VALUE}
     */
    public void setEndValue(String endValue)
    {
        getPropertyStorage().setValue(END_VALUE, endValue);
    }
}
