package cz.cesnet.shongo.controller.resource;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a special type of resource a video/web conferencing equipment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class DeviceResource extends Resource
{
    /**
     * Mode of the device.
     */
    private Mode mode;

    /**
     * Set of technologies for which the device capability is applied.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Technology that is used when a multiple technologies are available.
     */
    private Technology preferredTechnology;

    /**
     * Option telling whether the device can be called by another device.
     */
    private boolean callable;

    /**
     * @return mode for device (managed/unmanaged).
     */
    @OneToOne
    public Mode getMode()
    {
        return mode;
    }

    /**
     * @param mode Sets the {@link #mode}
     */
    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    /**
     * @return true if device is managed by a connector,
     *         false otherwise
     */
    @Transient
    public boolean isManaged()
    {
        return this.mode instanceof ManagedMode;
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Access(AccessType.FIELD)
    @Enumerated(EnumType.STRING)
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
    }

    /**
     * @param technologies set the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        clearTechnologies();
        for ( Technology technology : technologies ) {
            addTechnology(technology);
        }
    }

    /**
     * @param technology single technology that a device provides (resets all technologies already added).
     */
    public void setTechnology(Technology technology)
    {
        clearTechnologies();
        addTechnology(technology);
    }

    /**
     * Remove all technologies that device supports.
     */
    public void clearTechnologies()
    {
        this.technologies.clear();
    }

    /**
     * @param technology technology to be added to the set of technologies that the device support.
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @return {@link #preferredTechnology}
     */
    @Column
    public Technology getPreferredTechnology()
    {
        return preferredTechnology;
    }

    /**
     * @param preferredTechnology Sets the {@link #preferredTechnology}
     */
    public void setPreferredTechnology(Technology preferredTechnology)
    {
        this.preferredTechnology = preferredTechnology;
    }

    /**
     * @return {@link #callable}
     */
    @Column(nullable = false)
    public boolean isCallable()
    {
        return callable;
    }

    /**
     * @param callable Sets the {@link #callable}
     */
    public void setCallable(boolean callable)
    {
        this.callable = callable;
    }

    @Override
    public Type getType()
    {
        return Type.DEVICE;
    }

    @Override
    public void setType(Type type)
    {
        if (type != Type.DEVICE) {
            throw new RuntimeException("Device resource must be always of resource type device.");
        }
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        StringBuilder technologies = new StringBuilder();
        for ( Technology technology : this.technologies) {
            if ( technologies.length() > 0 ) {
                technologies.append(",");
            }
            technologies.append(technology.toString());
        }
        map.put("technologies", "[" + technologies.toString() + "]");
        map.put("callable", (isCallable() ? "true" : "false"));
        map.put("mode", (isManaged() ? "managed" : "unmanaged"));
    }
}
