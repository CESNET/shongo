package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;

/**
 * {@link cz.cesnet.shongo.controller.api.Specification} for a permanent room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PermanentRoomSpecification extends StandaloneRoomSpecification
{
    /**
     * Constructor.
     */
    public PermanentRoomSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #technologies}
     */
    public PermanentRoomSpecification(Technology technology)
    {
        addTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param aliasType to create {@link #aliasSpecifications}
     */
    public PermanentRoomSpecification(AliasType aliasType)
    {
        addAliasSpecification(new AliasSpecification(aliasType));
    }

    /**
     * Constructor.
     *
     * @param aliasTypes to create {@link #aliasSpecifications}
     */
    public PermanentRoomSpecification(AliasType[] aliasTypes)
    {
        for (AliasType aliasType : aliasTypes) {
            addAliasSpecification(new AliasSpecification(aliasType));
        }
    }

    /**
     * @param resourceId sets the {@link #resourceId}
     * @return this {@link PermanentRoomSpecification} with {@link #resourceId} set to {@code resourceId}
     */
    public PermanentRoomSpecification withResourceId(String resourceId)
    {
        setResourceId(resourceId);
        return this;
    }

    /**
     * @param aliasType for the new {@link cz.cesnet.shongo.controller.api.AliasSpecification}
     * @param value     for the new {@link cz.cesnet.shongo.controller.api.AliasSpecification}
     * @return this {@link PermanentRoomSpecification}
     */
    public PermanentRoomSpecification withAlias(AliasType aliasType, String value)
    {
        addAliasSpecification(new AliasSpecification(aliasType).withValue(value));
        return this;
    }

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
    }
}
