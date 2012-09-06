package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.annotation.AllowedTypes;
import cz.cesnet.shongo.api.annotation.ReadOnly;
import cz.cesnet.shongo.api.annotation.Required;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;
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
    public static final String ADDRESS = "address";

    /**
     * Set of technologies which the resource supports.
     */
    public static final String TECHNOLOGIES = "technologies";

    /**
     * Specifies the mode of the resource.
     */
    public static final String MODE = "mode";

    /**
     * @return {@link #ADDRESS}
     */
    public String getAddress()
    {
        return getPropertyStorage().getValue(ADDRESS);
    }

    /**
     * @param address sets the {@link #ADDRESS}
     */
    public void setAddress(String address)
    {
        getPropertyStorage().setValue(ADDRESS, address);
    }

    /**
     * @return {@link #TECHNOLOGIES}
     */
    @Required
    public Set<Technology> getTechnologies()
    {
        return getPropertyStorage().getCollection(TECHNOLOGIES, Set.class);
    }

    /**
     * @param technologies sets the {@link #TECHNOLOGIES}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        getPropertyStorage().setCollection(TECHNOLOGIES, technologies);
    }

    /**
     * @param technology technology to be added to the {@link #TECHNOLOGIES}
     */
    public void addTechnology(Technology technology)
    {
        getPropertyStorage().addCollectionItem(TECHNOLOGIES, technology, Set.class);
    }

    /**
     * @param technology technology to be removed from the {@link #TECHNOLOGIES}
     */
    public void removeTechnology(Technology technology)
    {
        getPropertyStorage().removeCollectionItem(TECHNOLOGIES, technology);
    }

    /**
     * @return {@link #MODE}
     */
    @AllowedTypes({String.class, ManagedMode.class})
    public Object getMode()
    {
        return getPropertyStorage().getValue(MODE);
    }

    /**
     * @param mode sets the {@link #MODE}
     */
    public void setMode(Object mode)
    {
        getPropertyStorage().setValue(MODE, mode);
    }
}
