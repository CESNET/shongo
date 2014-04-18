package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.Alias;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link AbstractParticipant} for single external endpoint.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExternalEndpointParticipant extends AbstractParticipant
{
    /**
     * Set of technologies of the external endpoint.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Number of same resources.
     */
    private Alias alias;

    /**
     * Constructor.
     */
    public ExternalEndpointParticipant()
    {
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #TECHNOLOGIES}
     */
    public ExternalEndpointParticipant(Technology technology)
    {
        addTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #TECHNOLOGIES}
     * @param alias      sets the {@link #ALIAS}
     */
    public ExternalEndpointParticipant(Technology technology, Alias alias)
    {
        addTechnology(technology);
        setAlias(alias);
    }

    /**
     * Constructor.
     *
     * @param technologies to be added to the {@link #TECHNOLOGIES}
     * @param alias        sets the {@link #ALIAS}
     */
    public ExternalEndpointParticipant(Technology[] technologies, Alias alias)
    {
        for (Technology technology : technologies) {
            addTechnology(technology);
        }
        setAlias(alias);
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
     * @return {@link #ALIAS}
     */
    public Alias getAlias()
    {
        return alias;
    }

    /**
     * @param alias sets the {@link #ALIAS}
     */
    public void setAlias(Alias alias)
    {
        this.alias = alias;
    }

    public static final String TECHNOLOGIES = "technologies";
    public static final String ALIAS = "alias";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(ALIAS, alias);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        alias = dataMap.getComplexType(ALIAS, Alias.class);
    }
}
