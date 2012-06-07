package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.common.Person;

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
     * IP address on which the device is running.
     */
    private String ipAddress;

    /**
     * List of aliases that are permanently assigned to device.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * Set of technologies for which the device capability is applied.
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
     * @return {@link #ipAddress}
     */
    @Column
    public String getIpAddress()
    {
        return ipAddress;
    }

    /**
     * @param ipAddress sets the {@link #ipAddress}
     */
    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    /**
     * @return {@link #aliases}
     */
    @OneToMany
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * @param aliases sets the {@link #aliases}
     */
    private void setAliases(List<Alias> aliases)
    {
        this.aliases = aliases;
    }

    /**
     * @param alias alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        this.aliases.add(alias);
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
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
    }

    /**
     * @param technologies set the {@link #technologies}
     */
    private void setTechnologies(Set<Technology> technologies)
    {
        clearTechnologies();
        for (Technology technology : technologies) {
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
    public List<Person> getPermanentPersons()
    {
        return permanentPersons;
    }

    /**
     * @param permanentPersons sets the {@link #permanentPersons}
     */
    private void setPermanentPersons(List<Person> permanentPersons)
    {
        this.permanentPersons = permanentPersons;
    }

    /**
     * @param person person to be aded to the {@link #permanentPersons}
     */
    public void addPermanentPerson(Person person)
    {
        this.permanentPersons.add(person);
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

        if ( technologies.size() > 0 ) {
            StringBuilder builder = new StringBuilder();
            for (Technology technology : technologies) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(technology.toString());
            }
            map.put("technologies", "[" + builder.toString() + "]");
        }
        map.put("callable", (isCallable() ? "true" : "false"));
        map.put("mode", (isManaged() ? "managed" : "unmanaged"));
    }
}
