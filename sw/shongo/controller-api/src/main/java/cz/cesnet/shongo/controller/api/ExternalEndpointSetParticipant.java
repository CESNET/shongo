package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link AbstractParticipant} for one or multiple external endpoint.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExternalEndpointSetParticipant extends AbstractParticipant
{
    /**
     * Set of technologies of the external endpoint.
     */
    private Set<Technology> technologies = new HashSet<Technology>();


    /**
     * Number of same resources.
     */
    private Integer count;

    /**
     * Constructor.
     */
    public ExternalEndpointSetParticipant()
    {
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #TECHNOLOGIES}
     * @param count      sets the {@link #COUNT}
     */
    public ExternalEndpointSetParticipant(Technology technology, int count)
    {
        addTechnology(technology);
        setCount(count);
    }

    /**
     * Constructor.
     *
     * @param technologies to be added to the {@link #TECHNOLOGIES}
     * @param count        sets the {@link #COUNT}
     */
    public ExternalEndpointSetParticipant(Technology[] technologies, int count)
    {
        for (Technology technology : technologies) {
            addTechnology(technology);
        }
        setCount(count);
    }

    /**
     * @return {@link #TECHNOLOGIES}
     */
    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    /**
     * @param technologies sets the {@link #TECHNOLOGIES}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    /**
     * @param technology technology to be added to the {@link #TECHNOLOGIES}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @param technology technology to be removed from the {@link #TECHNOLOGIES}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
    }

    /**
     * @return {@link #COUNT}
     */
    public Integer getCount()
    {
        return count;
    }

    /**
     * @param count sets the {@link #COUNT}
     */
    public void setCount(Integer count)
    {
        this.count = count;
    }

    public static final String TECHNOLOGIES = "technologies";
    public static final String COUNT = "count";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(COUNT, count);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        technologies = dataMap.getSetRequired(TECHNOLOGIES, Technology.class);
        count = dataMap.getIntegerRequired(COUNT);
    }
}
