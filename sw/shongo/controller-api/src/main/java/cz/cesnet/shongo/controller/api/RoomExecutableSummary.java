package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents summary of an allocated {@link RoomExecutable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomExecutableSummary extends ExecutableSummary
{
    /**
     * Room name.
     */
    private String name;

    /**
     * Room technologies.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #technologies}
     */
    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    /**
     * @param technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    private static final String NAME = "name";
    private static final String TECHNOLOGIES = "technologies";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(NAME, name);
        dataMap.set(TECHNOLOGIES, technologies);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        name = dataMap.getString(NAME);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
    }
}
