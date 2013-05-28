package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.RoomSetting;
import cz.cesnet.shongo.api.util.IdentifiedObject;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents an allocated object which can be executed.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Executable extends IdentifiedObject
{
    /**
     * Reservation for which the {@link Executable} is allocated.
     */
    private String reservationId;

    /**
     * Slot of the {@link cz.cesnet.shongo.controller.api.Executable}.
     */
    private Interval slot;

    /**
     * Current state of the {@link cz.cesnet.shongo.controller.api.Executable}.
     */
    private State state;

    /**
     * Description of state.
     */
    private String stateReport;

    /**
     * {@link Executable} is migrated to this {@link Executable}.
     */
    private Executable migratedExecutable;

    /**
     * @return {@link #reservationId}
     */
    public String getReservationId()
    {
        return reservationId;
    }

    /**
     * @param reservationId sets the {@link #reservationId}
     */
    public void setReservationId(String reservationId)
    {
        this.reservationId = reservationId;
    }

    /**
     * @return {@link #slot}
     */
    public Interval getSlot()
    {
        return slot;
    }

    /**
     * @param slot sets the {@link #slot}
     */
    public void setSlot(Interval slot)
    {
        this.slot = slot;
    }

    /**
     * @return {@link #state}
     */
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    /**
     * @return {@link #stateReport}
     */
    public String getStateReport()
    {
        return stateReport;
    }

    /**
     * @param stateReport sets the {@link #stateReport}
     */
    public void setStateReport(String stateReport)
    {
        this.stateReport = stateReport;
    }

    /**
     * @return {@link #migratedExecutable}
     */
    public Executable getMigratedExecutable()
    {
        return migratedExecutable;
    }

    /**
     * @param migratedExecutable sets the {@link #migratedExecutable}
     */
    public void setMigratedExecutable(Executable migratedExecutable)
    {
        this.migratedExecutable = migratedExecutable;
    }

    /**
     * State of the {@link cz.cesnet.shongo.controller.api.Executable}.
     */
    public static enum State
    {
        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} has not been started yet.
         */
        NOT_STARTED,

        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} is already started.
         */
        STARTED,

        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} failed to start.
         */
        STARTING_FAILED,

        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} has been already stopped.
         */
        STOPPED,

        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} failed to stop.
         */
        STOPPING_FAILED
    }

    /**
     * Represents an allocated compartment.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    public static class Compartment extends Executable
    {
        /**
         * List of {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint}s.
         */
        private List<Endpoint> endpoints = new ArrayList<Endpoint>();

        /**
         * List of {@link cz.cesnet.shongo.controller.api.Executable.ResourceRoom}s.
         */
        private List<ResourceRoom> rooms = new ArrayList<ResourceRoom>();

        /**
         * List of {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Connection}s.
         */
        private List<Connection> connections = new ArrayList<Connection>();

        /**
         * @return {@link #endpoints}
         */
        public List<Endpoint> getEndpoints()
        {
            return endpoints;
        }

        /**
         * @param endpoints sets the {@link #endpoints}
         */
        public void setEndpoints(List<Endpoint> endpoints)
        {
            this.endpoints = endpoints;
        }

        /**
         * @param endpoint to be added to the {@link #endpoints}
         */
        public void addEndpoint(Endpoint endpoint)
        {
            endpoints.add(endpoint);
        }

        /**
         * @return {@link #rooms}
         */
        public List<ResourceRoom> getRooms()
        {
            return rooms;
        }

        /**
         * @param rooms sets the {@link #rooms}
         */
        public void setRooms(List<ResourceRoom> rooms)
        {
            this.rooms = rooms;
        }

        /**
         * @param room to be added to the {@link #rooms}
         */
        public void addRoom(ResourceRoom room)
        {
            rooms.add(room);
        }

        /**
         * @return {@link #connections}
         */
        public List<Connection> getConnections()
        {
            return connections;
        }

        /**
         * @param connections sets the {@link #connections}
         */
        public void setConnections(List<Connection> connections)
        {
            this.connections = connections;
        }

        /**
         * @param connection to be added to the {@link #connections}
         */
        public void addConnection(Connection connection)
        {
            connections.add(connection);
        }

    }

    /**
     * Represents an endpoint which is participating in the {@link cz.cesnet.shongo.controller.api.Executable.Compartment}.
     */
    public static class Endpoint extends Executable
    {
        /**
         * Id of the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint}.
         */
        private String id;

        /**
         * Description of the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint}.
         */
        private String description;

        /**
         * List of assigned {@link cz.cesnet.shongo.api.Alias}es to the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint}.
         */
        private List<Alias> aliases = new ArrayList<Alias>();

        /**
         * @return {@link #id}
         */
        public String getId()
        {
            return id;
        }

        /**
         * @param id sets the {@link #id}
         */
        public void setId(String id)
        {
            this.id = id;
        }

        /**
         * @return {@link #description}
         */
        public String getDescription()
        {
            return description;
        }

        /**
         * @param description sets the {@link #description}
         */
        public void setDescription(String description)
        {
            this.description = description;
        }

        /**
         * @return {@link #aliases}
         */
        public List<Alias> getAliases()
        {
            return aliases;
        }

        /**
         * @param aliases sets the {@link #aliases}
         */
        public void setAliases(List<Alias> aliases)
        {
            this.aliases = aliases;
        }

        /**
         * @param alias to be added to the {@link #aliases}
         */
        public void addAlias(Alias alias)
        {
            aliases.add(alias);
        }
    }

    /**
     * Represents a virtual room which interconnects
     * {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint}s in
     * the {@link cz.cesnet.shongo.controller.api.Executable.Compartment}.
     */
    public static class ResourceRoom extends Executable
    {
        /**
         * Id of the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint}.
         */
        private String id;

        /**
         * Id of the {@link cz.cesnet.shongo.controller.api.Resource}.
         */
        private String resourceId;

        /**
         * Technology specific room identifier.
         */
        private String roomId;

        /**
         * Set of technologies which the virtual room shall support.
         */
        private Set<Technology> technologies = new HashSet<Technology>();

        /**
         * License count.
         */
        private int licenseCount;

        /**
         * List of assigned {@link cz.cesnet.shongo.api.Alias}es to the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint}.
         */
        private List<Alias> aliases = new ArrayList<Alias>();

        /**
         * List of {@link cz.cesnet.shongo.api.RoomSetting}s for the {@link cz.cesnet.shongo.controller.api.Executable.ResourceRoom}.
         */
        private List<RoomSetting> roomSettings = new ArrayList<RoomSetting>();

        /**
         * @return {@link #id}
         */
        public String getId()
        {
            return id;
        }

        /**
         * @param id sets the {@link #id}
         */
        public void setId(String id)
        {
            this.id = id;
        }

        /**
         * @return {@link #resourceId}
         */
        public String getResourceId()
        {
            return resourceId;
        }

        /**
         * @param resourceId sets the {@link #resourceId}
         */
        public void setResourceId(String resourceId)
        {
            this.resourceId = resourceId;
        }

        /**
         * @return {@link #roomId}
         */
        public String getRoomId()
        {
            return roomId;
        }

        /**
         * @param roomId sets the {@link #roomId}
         */
        public void setRoomId(String roomId)
        {
            this.roomId = roomId;
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
         * Clear {@link #technologies}.
         */
        public void clearTechnologies()
        {
            technologies.clear();
        }

        /**
         * @param technology technology to be added to the set of technologies that the device support.
         */
        public void addTechnology(Technology technology)
        {
            technologies.add(technology);
        }

        /**
         * @return {@link #licenseCount}
         */
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
         * @return {@link #aliases}
         */
        public List<Alias> getAliases()
        {
            return aliases;
        }

        /**
         * @param aliases sets the {@link #aliases}
         */
        public void setAliases(List<Alias> aliases)
        {
            this.aliases = aliases;
        }

        /**
         * Clear {@link #aliases}.
         */
        public void clearAliases()
        {
            aliases.clear();
        }

        /**
         * @param alias to be added to the {@link #aliases}
         */
        public void addAlias(Alias alias)
        {
            aliases.add(alias);
        }

        /**
         * @return {@link #roomSettings}
         */
        public List<RoomSetting> getRoomSettings()
        {
            return roomSettings;
        }

        /**
         * @param roomSettings sets the {@link #roomSettings}
         */
        public void setRoomSettings(List<RoomSetting> roomSettings)
        {
            this.roomSettings = roomSettings;
        }

        /**
         * Clear {@link #roomSettings}.
         */
        public void clearRoomSettings()
        {
            roomSettings.clear();
        }

        /**
         * @param roomSetting to be added to the {@link #roomSettings}
         */
        public void addRoomSetting(RoomSetting roomSetting)
        {
            roomSettings.add(roomSetting);
        }
    }

    /**
     * Represents a connection between {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint}s in the {@link cz.cesnet.shongo.controller.api.Executable.Compartment}.
     */
    public static class Connection extends Executable
    {
        /**
         * Id of endpoint which initiates the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Connection}.
         */
        private String endpointFromId;

        /**
         * Id of target endpoint for the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Connection}.
         */
        private String endpointToId;

        /**
         * {@link cz.cesnet.shongo.api.Alias} which is used for the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Connection}
         */
        private Alias alias;

        /**
         * @return {@link #endpointFromId}
         */
        public String getEndpointFromId()
        {
            return endpointFromId;
        }

        /**
         * @param endpointFromId sets the {@link #endpointFromId}
         */
        public void setEndpointFromId(String endpointFromId)
        {
            this.endpointFromId = endpointFromId;
        }

        /**
         * @return {@link #endpointToId}
         */
        public String getEndpointToId()
        {
            return endpointToId;
        }

        /**
         * @param endpointToId sets the {@link #endpointToId}
         */
        public void setEndpointToId(String endpointToId)
        {
            this.endpointToId = endpointToId;
        }

        /**
         * @return {@link #alias}
         */
        public Alias getAlias()
        {
            return alias;
        }

        /**
         * @param alias sets the {@link #alias}
         */
        public void setAlias(Alias alias)
        {
            this.alias = alias;
        }
    }
}
