package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.AliasSpecification;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.scheduler.report.NoAvailableAliasReport;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.*;

/**
 * Represents {@link cz.cesnet.shongo.controller.scheduler.ReservationTask} for one or multiple {@link AliasReservation}(s).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasGroupReservationTask extends ReservationTask
{
    /**
     * List of {@link AliasSpecification}s.
     */
    private List<AliasSpecification> aliasSpecifications = new ArrayList<AliasSpecification>();

    /**
     * {@link DeviceResource} for which the {@link AliasReservation} is being allocated and to which it
     * will be assigned.
     */
    private DeviceResource targetResource;

    /**
     * Constructor.
     *
     * @param context sets the {@link #context}
     */
    public AliasGroupReservationTask(Context context)
    {
        super(context);
    }

    /**
     * @param aliasSpecification to be added to the {@link #aliasSpecifications}
     */
    public void addAliasSpecification(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.add(aliasSpecification);
    }

    /**
     * @param targetResource sets the {@link #targetResource}
     */
    public void setTargetResource(DeviceResource targetResource)
    {
        this.targetResource = targetResource;
    }

    @Override
    protected Reservation createReservation() throws ReportException
    {
        Set<AliasType> allocatedAliasTypes = new HashSet<AliasType>();
        Set<Technology> allocatedAliasTechnologies = new HashSet<Technology>();
        ResourceRoomEndpoint allocatedRoomEndpoint = null;

        List<Reservation> createdReservations = new ArrayList<Reservation>();

        // Process all alias specifications
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            // Check whether current alias specification is already satisfied by allocated aliases
            boolean satisfied = true;
            // Check alias types
            Set<AliasType> aliasTypes = aliasSpecification.getAliasTypes();
            if (aliasTypes.size() == 0 && allocatedAliasTypes.size() == 0) {
                satisfied = false;
            }
            for (AliasType aliasType : aliasTypes) {
                if (!allocatedAliasTypes.contains(aliasType)) {
                    satisfied = false;
                    break;
                }
            }
            // Check alias technologies
            Set<Technology> aliasTechnologies = aliasSpecification.getTechnologies();
            if (aliasTechnologies.size() == 0 && allocatedAliasTechnologies.size() == 0) {
                satisfied = false;
            }
            for (Technology aliasTechnology : aliasTechnologies) {
                if (!aliasTechnologies.contains(aliasTechnology)) {
                    satisfied = false;
                    break;
                }
            }

            // No new alias is needed for satisfied specification
            if (satisfied) {
                continue;
            }

            // Create new reservation task
            AliasReservationTask aliasReservationTask = aliasSpecification.createReservationTask(getContext());
            if (targetResource != null) {
                aliasReservationTask.setTargetResource(targetResource);
            }
            if (allocatedRoomEndpoint != null) {
                aliasReservationTask.setTargetResource(allocatedRoomEndpoint.getDeviceResource());
            }

            // Allocate missing alias
            Reservation reservation = aliasReservationTask.perform();
            createdReservations.add(reservation);
            AliasReservation aliasReservation = reservation.getTargetReservation(AliasReservation.class);

            // Append all allocated alias types and technologies
            AliasProviderCapability aliasProviderCapability = aliasReservation.getAliasProviderCapability();
            for (Alias alias : aliasProviderCapability.getAliases()) {
                AliasType aliasType = alias.getType();
                allocatedAliasTypes.add(aliasType);
                allocatedAliasTechnologies.add(aliasType.getTechnology());
            }

            // If room endpoint is allocated by previous alias reservation
            if (allocatedRoomEndpoint != null) {
                // Assign all new aliases to the endpoint
                Set<Technology> technologies = allocatedRoomEndpoint.getTechnologies();
                for (Alias alias : aliasReservation.getAliases()) {
                    if (alias.getTechnology().isCompatibleWith(technologies)) {
                        allocatedRoomEndpoint.addAssignedAlias(alias);
                    }
                }

                // Set the room endpoint to the reservation
                aliasReservation.setExecutable(allocatedRoomEndpoint);
            }
            // If room endpoint is allocated by current alias reservation
            else if (aliasProviderCapability.isPermanentRoom()) {
                // Use it for next alias reservations
                allocatedRoomEndpoint = (ResourceRoomEndpoint) aliasReservation.getExecutable();
            }
        }

        if (createdReservations.size() == 1) {
            return createdReservations.get(0);
        }
        else {
            // Create compound reservation request
            Reservation reservation = new Reservation();
            reservation.setSlot(getInterval());
            for (Reservation createdReservation : createdReservations) {
                addChildReservation(createdReservation);
            }
            return reservation;
        }
    }
}
