package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;

/**
 * {@link AbstractParticipant} which searches for available endpoint {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class LookupEndpointParticipant extends AbstractParticipant
{
    /**
     * Technology of the resource.
     */
    private Technology technology;

    /**
     * @return {@link #technology}
     */
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

    public static final String TECHNOLOGY = "technology";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TECHNOLOGY, technology);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        technology = dataMap.getEnumRequired(TECHNOLOGY, Technology.class);
    }
}
