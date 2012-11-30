package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an allocated compartment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Compartment extends Executable
{
    /**
     * List of {@link cz.cesnet.shongo.controller.api.Compartment.Endpoint}s.
     */
    private List<Endpoint> endpoints = new ArrayList<Endpoint>();

    /**
     * List of {@link cz.cesnet.shongo.controller.api.Compartment.RoomEndpoint}s.
     */
    private List<RoomEndpoint> roomEndpoints = new ArrayList<RoomEndpoint>();

    /**
     * List of {@link cz.cesnet.shongo.controller.api.Compartment.Connection}s.
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
    public List<RoomEndpoint> getRoomEndpoints()
    {
        return roomEndpoints;
    }

    /**
     * @param roomEndpoints sets the {@link #roomEndpoints}
     */
    public void setRoomEndpoints(List<RoomEndpoint> roomEndpoints)
    {
        this.roomEndpoints = roomEndpoints;
    }

    /**
     * @param roomEndpoint to be added to the {@link #roomEndpoints}
     */
    public void addRoomEndpoint(RoomEndpoint roomEndpoint)
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

    /**
     * Represents an endpoint which is participating in the {@link cz.cesnet.shongo.controller.api.Compartment}.
     */
    public static class Endpoint extends Executable
    {
        /**
         * Identifier of the {@link Endpoint}.
         */
        private String identifier;

        /**
         * Description of the {@link Endpoint}.
         */
        private String description;

        /**
         * List of assigned {@link cz.cesnet.shongo.api.Alias}es to the {@link Endpoint}.
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
     * Represents a virtual room which interconnects {@link cz.cesnet.shongo.controller.api.Compartment.Endpoint}s in the {@link cz.cesnet.shongo.controller.api.Compartment}.
     */
    public static class RoomEndpoint extends Endpoint
    {
        /**
         * License count.
         */
        private int licenseCount;

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
    }

    /**
     * Represents a connection between {@link cz.cesnet.shongo.controller.api.Compartment.Endpoint}s in the {@link cz.cesnet.shongo.controller.api.Compartment}.
     */
    public static class Connection extends Executable
    {
        /**
         * Identifier of endpoint which initiates the {@link Connection}.
         */
        private String endpointFromIdentifier;

        /**
         * Target endpoint for the {@link Connection}.
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
     * Represents a {@link Connection} by physical address.
     */
    public static class ConnectionByAddress extends Connection
    {
        /**
         * {@link cz.cesnet.shongo.Technology} for the {@link ConnectionByAddress}.
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
     * Represents a {@link Connection} by an {@link cz.cesnet.shongo.api.Alias}.
     */
    public static class ConnectionByAlias extends Connection
    {
        /**
         * {@link cz.cesnet.shongo.api.Alias} which is used for the {@link ConnectionByAlias}
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
