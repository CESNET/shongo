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
     * Identifier of the owner user.
     */
    private Integer userId;

    /**
     * Slot of the {@link cz.cesnet.shongo.controller.api.Executable}.
     */
    private Interval slot;

    /**
     * Current state of the {@link cz.cesnet.shongo.controller.api.Executable}.
     */
    private State state;

    /**
     * @return {@link #userId}
     */
    public Integer getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(Integer userId)
    {
        this.userId = userId;
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
        STOPPED
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
         * List of {@link cz.cesnet.shongo.controller.api.Executable.ResourceRoomEndpoint}s.
         */
        private List<ResourceRoomEndpoint> roomEndpoints = new ArrayList<ResourceRoomEndpoint>();

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
         * @return {@link #roomEndpoints}
         */
        public List<ResourceRoomEndpoint> getRoomEndpoints()
        {
            return roomEndpoints;
        }

        /**
         * @param roomEndpoints sets the {@link #roomEndpoints}
         */
        public void setRoomEndpoints(List<ResourceRoomEndpoint> roomEndpoints)
        {
            this.roomEndpoints = roomEndpoints;
        }

        /**
         * @param roomEndpoint to be added to the {@link #roomEndpoints}
         */
        public void addRoomEndpoint(ResourceRoomEndpoint roomEndpoint)
        {
            roomEndpoints.add(roomEndpoint);
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
         * Identifier of the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint}.
         */
        private String identifier;

        /**
         * Description of the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint}.
         */
        private String description;

        /**
         * List of assigned {@link cz.cesnet.shongo.api.Alias}es to the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint}.
         */
        private List<Alias> aliases = new ArrayList<Alias>();

        /**
         * @return {@link #identifier}
         */
        public String getIdentifier()
        {
            return identifier;
        }

        /**
         * @param identifier sets the {@link #identifier}
         */
        public void setIdentifier(String identifier)
        {
            this.identifier = identifier;
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
    public static class ResourceRoomEndpoint extends Executable
    {
        /**
         * Identifier of the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint}.
         */
        private String identifier;

        /**
         * Identifier of the {@link cz.cesnet.shongo.controller.api.Resource}.
         */
        private String resourceIdentifier;

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
         * List of {@link cz.cesnet.shongo.api.RoomSetting}s for the {@link ResourceRoomEndpoint}.
         */
        private List<RoomSetting> roomSettings = new ArrayList<RoomSetting>();

        /**
         * @return {@link #identifier}
         */
        public String getIdentifier()
        {
            return identifier;
        }

        /**
         * @param identifier sets the {@link #identifier}
         */
        public void setIdentifier(String identifier)
        {
            this.identifier = identifier;
        }

        /**
         * @return {@link #resourceIdentifier}
         */
        public String getResourceIdentifier()
        {
            return resourceIdentifier;
        }

        /**
         * @param resourceIdentifier sets the {@link #resourceIdentifier}
         */
        public void setResourceIdentifier(String resourceIdentifier)
        {
            this.resourceIdentifier = resourceIdentifier;
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
    public static abstract class Connection extends Executable
    {
        /**
         * Identifier of endpoint which initiates the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Connection}.
         */
        private String endpointFromIdentifier;

        /**
         * Target endpoint for the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Connection}.
         */
        private String endpointToIdentifier;

        /**
         * @return {@link #endpointFromIdentifier}
         */
        public String getEndpointFromIdentifier()
        {
            return endpointFromIdentifier;
        }

        /**
         * @param endpointFromIdentifier sets the {@link #endpointFromIdentifier}
         */
        public void setEndpointFromIdentifier(String endpointFromIdentifier)
        {
            this.endpointFromIdentifier = endpointFromIdentifier;
        }

        /**
         * @return {@link #endpointToIdentifier}
         */
        public String getEndpointToIdentifier()
        {
            return endpointToIdentifier;
        }

        /**
         * @param endpointToIdentifier sets the {@link #endpointToIdentifier}
         */
        public void setEndpointToIdentifier(String endpointToIdentifier)
        {
            this.endpointToIdentifier = endpointToIdentifier;
        }
    }

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Connection} by physical address.
     */
    public static class ConnectionByAddress extends Connection
    {
        /**
         * {@link cz.cesnet.shongo.Technology} for the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.ConnectionByAddress}.
         */
        private Technology technology;

        /**
         * Target address.
         */
        private String address;

        /**
         * @return {@link #technology}
         */
        public Technology getTechnology()
        {
            return technology;
        }

        /**
         * @param technology sets the {@link #technology}
         */
        public void setTechnology(Technology technology)
        {
            this.technology = technology;
        }

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
    }

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Executable.Compartment.Connection} by an {@link cz.cesnet.shongo.api.Alias}.
     */
    public static class ConnectionByAlias extends Connection
    {
        /**
         * {@link cz.cesnet.shongo.api.Alias} which is used for the {@link cz.cesnet.shongo.controller.api.Executable.Compartment.ConnectionByAlias}
         */
        private Alias alias;

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
