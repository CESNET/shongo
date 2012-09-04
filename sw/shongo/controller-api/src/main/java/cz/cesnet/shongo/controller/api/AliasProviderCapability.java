package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.annotation.Required;

/**
 * Capability tells that the resource can allocated can allocate aliases from an alias range.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasProviderCapability extends Capability
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
    public static final String PATTERN = "pattern";

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
     * @return {@link #PATTERN}
     */
    @Required
    public String getPattern()
    {
        return getPropertyStorage().getValue(PATTERN);
    }

    /**
     * @param pattern sets the {@link #PATTERN}
     */
    public void setPattern(String pattern)
    {
        getPropertyStorage().setValue(PATTERN, pattern);
    }
}
