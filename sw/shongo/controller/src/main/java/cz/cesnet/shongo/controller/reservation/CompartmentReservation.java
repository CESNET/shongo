package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.compartment.*;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

/**
 * Represents a {@link Reservation} for a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class CompartmentReservation extends Reservation
{
    /**
     * {@link Compartment} which is allocated by the {@link CompartmentReservation}.
     */
    private Compartment compartment;

    /**
     * @return {@link #compartment}
     */
    @OneToOne(cascade = CascadeType.ALL)
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


    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.CompartmentReservation();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api, Domain domain)
    {
        cz.cesnet.shongo.controller.api.CompartmentReservation compartmentReservationApi =
                (cz.cesnet.shongo.controller.api.CompartmentReservation) api;
        for (Endpoint endpoint : compartment.getEndpoints()) {
            cz.cesnet.shongo.controller.api.CompartmentReservation.Endpoint endpointApi =
                    new cz.cesnet.shongo.controller.api.CompartmentReservation.Endpoint();
            endpointApi.setDescription(endpoint.getReportDescription());
            for (Alias alias : endpoint.getAliases()) {
                endpointApi.addAlias(alias.toApi());
            }
            compartmentReservationApi.getCompartment().addEndpoint(endpointApi);
        }
        for (VirtualRoom virtualRoom : compartment.getVirtualRooms()) {
            cz.cesnet.shongo.controller.api.CompartmentReservation.VirtualRoom virtualRoomApi =
                    new cz.cesnet.shongo.controller.api.CompartmentReservation.VirtualRoom();
            virtualRoomApi.setDescription(virtualRoom.getReportDescription());
            for (Alias alias : virtualRoom.getAliases()) {
                virtualRoomApi.addAlias(alias.toApi());
            }
            compartmentReservationApi.getCompartment().addVirtualRoom(virtualRoomApi);
        }
        for (Connection connection : compartment.getConnections()) {
            if (connection instanceof ConnectionByAddress) {
                ConnectionByAddress connectionByAddress = (ConnectionByAddress) connection;
                cz.cesnet.shongo.controller.api.CompartmentReservation.ConnectionByAddress connectionByAddressApi =
                        new cz.cesnet.shongo.controller.api.CompartmentReservation.ConnectionByAddress();
                connectionByAddressApi.setEndpointFrom(connection.getEndpointFrom().getReportDescription());
                connectionByAddressApi.setEndpointTo(connection.getEndpointTo().getReportDescription());
                connectionByAddressApi.setAddress(connectionByAddress.getAddress().getValue());
                connectionByAddressApi.setTechnology(connectionByAddress.getTechnology());
                compartmentReservationApi.getCompartment().addConnection(connectionByAddressApi);
            }
            else if (connection instanceof ConnectionByAlias) {
                ConnectionByAlias connectionByAlias = (ConnectionByAlias) connection;
                cz.cesnet.shongo.controller.api.CompartmentReservation.ConnectionByAlias connectionByAliasApi =
                        new cz.cesnet.shongo.controller.api.CompartmentReservation.ConnectionByAlias();
                connectionByAliasApi.setEndpointFrom(connection.getEndpointFrom().getReportDescription());
                connectionByAliasApi.setEndpointTo(connection.getEndpointTo().getReportDescription());
                connectionByAliasApi.setAlias(connectionByAlias.getAlias().toApi());
                compartmentReservationApi.getCompartment().addConnection(connectionByAliasApi);
            }
            else {
                throw new TodoImplementException(connection.getClass().getCanonicalName());
            }
        }
        super.toApi(api, domain);
    }
}
