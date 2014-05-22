package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DeviceResource extends Resource
{
    /**
     * String representing unmanaged mode.
     */
    public static final String UNMANAGED_MODE = "UNMANAGED";

    /**
     * Address on which the device is running (IP address or URL).
     */
    private String address;

    /**
     * Set of technologies which the resource supports.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Specifies the mode of the resource.
     */
    private Object mode;

    /**
     * @return {@link #address}
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * @param address sets the {@link #address}
     */
    public void setAddress(String address)
    {
        this.address = address;
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
     * @param technology technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
    }

    /**
     * @return {@link #mode}
     */
    public Object getMode()
    {
        return mode;
    }

    /**
     * @param mode sets the {@link #mode}
     */
    public void setMode(Object mode)
    {
        this.mode = mode;
    }

    public static final String ADDRESS = "address";
    public static final String TECHNOLOGIES = "technologies";
    public static final String MODE = "mode";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ADDRESS, address);
        dataMap.set(TECHNOLOGIES, technologies);
        if (UNMANAGED_MODE.equals(mode)) {
            dataMap.set(MODE, UNMANAGED_MODE);
        }
        else if (mode instanceof ManagedMode) {
            dataMap.set(MODE, (ManagedMode) mode);
        }
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        address = dataMap.getString(ADDRESS, DEFAULT_COLUMN_LENGTH);
        technologies = dataMap.getSetRequired(TECHNOLOGIES, Technology.class);
        mode = dataMap.getVariant(MODE, ManagedMode.class, String.class);
    }
}
