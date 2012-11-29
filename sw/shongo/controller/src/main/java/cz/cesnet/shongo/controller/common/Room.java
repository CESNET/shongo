package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.Technology;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a virtual room for a video/web conference.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Embeddable
@Access(AccessType.PROPERTY)
public class Room
{
    /**
     * Set of technologies which the virtual room shall support.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Number of participants which shall be able to join to the virtual room.
     */
    private int participantCount;

    /**
     * List of {@link RoomConfiguration}s for the {@link Room} (e.g., {@link Technology} specific).
     */
    private List<RoomConfiguration> roomConfigurations = new ArrayList<RoomConfiguration>();

    /**
     * Constructor.
     */
    public Room()
    {
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
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies.clear();
        for (Technology technology : technologies) {
            this.technologies.add(technology);
        }
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
     * @return {@link #participantCount}
     */
    @Column(nullable = false)
    public int getParticipantCount()
    {
        return participantCount;
    }

    /**
     * @param participantCount sets the {@link #participantCount}
     */
    public void setParticipantCount(int participantCount)
    {
        this.participantCount = participantCount;
    }

    /**
     * @return {@link #roomConfigurations}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<RoomConfiguration> getRoomConfigurations()
    {
        return roomConfigurations;
    }

    /**
     * @param roomConfigurations sets the {@link #roomConfigurations}
     */
    public void setRoomConfigurations(List<RoomConfiguration> roomConfigurations)
    {
        this.roomConfigurations = roomConfigurations;
    }

    /**
     * @param roomConfiguration to be added to the {@link #roomConfigurations}
     */
    public void addRoomConfiguration(RoomConfiguration roomConfiguration)
    {
        roomConfigurations.add(roomConfiguration);
    }

    /**
     * @param roomConfiguration to be removed from the {@link #roomConfigurations}
     */
    public void removeRoomConfiguration(RoomConfiguration roomConfiguration)
    {
        roomConfigurations.remove(roomConfiguration);
    }

    /**
     * @return number of license which are needed for joining {@link #participantCount} number of participants
     */
    @Transient
    public int getLicenseCount()
    {
        return participantCount;
    }

    /**
     * Synchronize this {@link Room} from given {@code room}.
     *
     * @param room from which this {@link Room} should be synchronized
     * @return true if some change was made, false otherwise
     */
    public boolean synchronizeFrom(Room room)
    {
        boolean modified = false;
        if (!technologies.equals(room.getTechnologies())) {
            setTechnologies(room.getTechnologies());
            modified = true;
        }
        if (!roomConfigurations.equals(room.getRoomConfigurations())) {
            setRoomConfigurations(room.getRoomConfigurations());
            modified = true;
        }
        modified |= !ObjectUtils.equals(getParticipantCount(), room.getParticipantCount());
        setParticipantCount(room.getParticipantCount());
        return modified;
    }
}
