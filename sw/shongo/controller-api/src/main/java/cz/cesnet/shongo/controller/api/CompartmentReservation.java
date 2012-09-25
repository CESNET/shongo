package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@link Reservation} for a {@link CompartmentSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentReservation extends Reservation
{
    /**
     * @see {@link Compartment}
     */
    private Compartment compartment = new Compartment();

    /**
     * @return {@link #compartment}
     */
    public Compartment getCompartment()
    {
        return compartment;
    }

    /**
     * @param compartment sets the {@link #compartment}
     */
    public void setCompartment(Compartment compartment)
    {
        this.compartment = compartment;
    }

    /**
     * Represents an allocated compartment.
     */
    public static class Compartment
    {
        /**
         * List of {@link Endpoint}s.
         */
        private List<Endpoint> endpoints = new ArrayList<Endpoint>();

        /**
         * List of {@link VirtualRoom}s.
         */
        private List<VirtualRoom> virtualRooms = new ArrayList<VirtualRoom>();

        /**
         * List of {@link Connection}s.
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
         * @return {@link #virtualRooms}
         */
        public List<VirtualRoom> getVirtualRooms()
        {
            return virtualRooms;
        }

        /**
         * @param virtualRooms sets the {@link #virtualRooms}
         */
        public void setVirtualRooms(List<VirtualRoom> virtualRooms)
        {
            this.virtualRooms = virtualRooms;
        }

        /**
         * @param virtualRoom to be added to the {@link #virtualRooms}
         */
        public void addVirtualRoom(VirtualRoom virtualRoom)
        {
            virtualRooms.add(virtualRoom);
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
     * Represents an endpoint which is participating in the {@link Compartment}.
     */
    public static class Endpoint
    {
        /**
         * Description of the {@link Endpoint}.
         */
        private String description;

        /**
         * List of assigned {@link Alias}es to the {@link Endpoint}.
         */
        private List<Alias> aliases = new ArrayList<Alias>();

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
     * Represents a virtual room which interconnects {@link Endpoint}s in the {@link Compartment}.
     */
    public static class VirtualRoom extends Endpoint
    {
    }

    /**
     * Represents a connection between {@link Endpoint}s in the {@link Compartment}.
     */
    public static class Connection
    {
        /**
         * Endpoint which initiates the {@link Connection}.
         */
        private String endpointFrom;

        /**
         * Target endpoint for the {@link Connection}.
         */
        private String endpointTo;

        /**
         * @return {@link #endpointFrom}
         */
        public String getEndpointFrom()
        {
            return endpointFrom;
        }

        /**
         * @param endpointFrom sets the {@link #endpointFrom}
         */
        public void setEndpointFrom(String endpointFrom)
        {
            this.endpointFrom = endpointFrom;
        }

        /**
         * @return {@link #endpointTo}
         */
        public String getEndpointTo()
        {
            return endpointTo;
        }

        /**
         * @param endpointTo sets the {@link #endpointTo}
         */
        public void setEndpointTo(String endpointTo)
        {
            this.endpointTo = endpointTo;
        }
    }

    /**
     * Represents a {@link Connection} by physical address.
     */
    public static class ConnectionByAddress extends Connection
    {
        /**
         * {@link Technology} for the {@link ConnectionByAddress}.
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
     * Represents a {@link Connection} by an {@link Alias}.
     */
    public static class ConnectionByAlias extends Connection
    {
        /**
         * {@link Alias} which is used for the {@link ConnectionByAlias}
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
