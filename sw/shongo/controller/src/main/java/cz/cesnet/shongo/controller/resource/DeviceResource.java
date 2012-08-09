package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.common.Person;

import javax.persistence.*;
import java.util.*;

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
     * Address on which the device is running (IP address or URL)
     */
    private String address;

    /**
     * Set of technologies which the resource supports.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Technology that is used when a multiple technologies are available.
     */
    private Technology preferredTechnology;

    /**
     * List of persons which automatically use the resource in all reservation requests.
     */
    private List<Person> permanentPersons = new ArrayList<Person>();

    /**
     * Option telling whether the device can be called by another device.
     */
    private boolean callable;

    /**
     * @return mode for device (managed/unmanaged).
     */
    @OneToOne(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
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
     * @return {@link #address}
     */
    @Column
    public String getAddress()
    {
        return address;
    }

    /**
     * @return true if device resource has IP address filled, otherwise false
     */
    public boolean hasIpAddress()
    {
        return address != null;
    }

    /**
     * @param address sets the {@link #address}
     */
    public void setAddress(String address)
    {
        this.address = address;
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
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
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
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
    }

    /**
     * @return {@link #preferredTechnology}
     */
    @Column
    @Enumerated(EnumType.STRING)
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
     * @return {@link #permanentPersons}
     */
    @OneToMany
    @Access(AccessType.FIELD)
    public List<Person> getPermanentPersons()
    {
        return permanentPersons;
    }

    /**
     * @param person person to be added to the {@link #permanentPersons}
     */
    public void addPermanentPerson(Person person)
    {
        permanentPersons.add(person);
    }

    /**
     * @param person person to be removed from the {@link #permanentPersons}
     */
    public void removePermanentPerson(Person person)
    {
        permanentPersons.remove(person);
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
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("callable", (isCallable() ? "true" : "false"));
        map.put("mode", (isManaged() ? "managed" : "unmanaged"));
        addCollectionToMap(map, "technologies", technologies);
    }
}
