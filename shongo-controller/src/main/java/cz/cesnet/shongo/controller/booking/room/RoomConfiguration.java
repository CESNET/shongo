package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a configuration for a virtual room for a video/web conference.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RoomConfiguration extends SimplePersistentObject
{
    /**
     * Set of technologies which the room shall support (if the set is empty the room shall support all possible technologies).
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Number of licenses which shall the virtual room have.
     */
    private int licenseCount = 0;

    /**
     * List of {@link cz.cesnet.shongo.controller.booking.room.settting.RoomSetting}s for the {@link RoomConfiguration} (e.g., {@link Technology} specific).
     */
    private List<RoomSetting> roomSettings = new ArrayList<RoomSetting>();

    /**
     * Constructor.
     */
    public RoomConfiguration()
    {
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies.clear();
        this.technologies.addAll(technologies);
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
     * @return {@link #licenseCount}
     */
    @Column(nullable = false)
    public int getLicenseCount()
    {
        return licenseCount;
    }

    /**
     * @param licenseCount sets the {@link #licenseCount}
     */
    public void setLicenseCount(int licenseCount)
    {
        this.licenseCount = licenseCount;
    }

    /**
     * @return {@link #roomSettings}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<RoomSetting> getRoomSettings()
    {
        return roomSettings;
    }

    /**
     * @param roomSettings sets the {@link #roomSettings}
     */
    public void setRoomSettings(Collection<RoomSetting> roomSettings)
    {
        this.roomSettings.clear();
        for (RoomSetting roomConfiguration : roomSettings) {
            this.roomSettings.add(roomConfiguration);
        }
    }

    /**
     * @param roomSetting to be added to the {@link #roomSettings}
     */
    public void addRoomSetting(RoomSetting roomSetting)
    {
        roomSettings.add(roomSetting);
    }

    /**
     * @param roomSetting to be removed from the {@link #roomSettings}
     */
    public void removeRoomSetting(RoomSetting roomSetting)
    {
        roomSettings.remove(roomSetting);
    }
}
