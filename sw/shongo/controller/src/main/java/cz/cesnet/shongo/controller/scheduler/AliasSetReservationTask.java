package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.request.AliasSpecification;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents {@link cz.cesnet.shongo.controller.scheduler.ReservationTask} for one or multiple {@link AliasReservation}(s).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasSetReservationTask extends ReservationTask
{
    /**
     * List of {@link AliasSpecification}s.
     */
    private List<AliasSpecification> aliasSpecifications = new ArrayList<AliasSpecification>();

    /**
     * Share created executable.
     */
    private boolean sharedExecutable = false;

    /**
     * Constructor.
     *
     * @param schedulerContext sets the {@link #schedulerContext}
     */
    public AliasSetReservationTask(SchedulerContext schedulerContext)
    {
        super(schedulerContext);
    }

    /**
     * @param aliasSpecification to be added to the {@link #aliasSpecifications}
     */
    public void addAliasSpecification(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.add(aliasSpecification);
    }

    /**
     * @param sharedExecutable sets the {@link #sharedExecutable}
     */
    public void setSharedExecutable(boolean sharedExecutable)
    {
        this.sharedExecutable = sharedExecutable;
    }

    @Override
    protected Reservation allocateReservation(Reservation allocatedReservation) throws SchedulerException
    {
        validateReservationSlot(AliasReservation.class);

        SchedulerContext schedulerContext = getSchedulerContext();

        if (aliasSpecifications.size() == 1) {
            AliasSpecification aliasSpecification = aliasSpecifications.get(0);
            AliasReservationTask aliasReservationTask = aliasSpecification.createReservationTask(schedulerContext);
            Reservation reservation = aliasReservationTask.perform(allocatedReservation);
            addReports(aliasReservationTask);
            return reservation;
        }
        else {
            ResourceRoomEndpoint allocatedRoomEndpoint = null;

            List<Reservation> createdReservations = new ArrayList<Reservation>();

            // Process all alias specifications
            for (AliasSpecification aliasSpecification : aliasSpecifications) {
                // Create new reservation task
                AliasReservationTask aliasReservationTask = aliasSpecification.createReservationTask(schedulerContext);
                if (allocatedRoomEndpoint != null) {
                    aliasReservationTask.setTargetResource(allocatedRoomEndpoint.getDeviceResource());
                }

                // Allocate missing alias
                Reservation reservation = aliasReservationTask.perform(null);
                addReports(aliasReservationTask);
                createdReservations.add(reservation);
                AliasReservation aliasReservation = reservation.getTargetReservation(AliasReservation.class);
                AliasProviderCapability aliasProviderCapability = aliasReservation.getAliasProviderCapability();

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
                else if (aliasProviderCapability.isPermanentRoom() && schedulerContext
                        .isExecutableAllowed() && sharedExecutable) {
                    // Use it for next alias reservations
                    allocatedRoomEndpoint = (ResourceRoomEndpoint) aliasReservation.getExecutable();
                }
            }

            // Create compound reservation request
            Reservation reservation;
            if (allocatedReservation != null && allocatedReservation.getClass().equals(Reservation.class)) {
                // Reallocate existing reservation
                addReport(new SchedulerReportSet.ReservationReallocatingReport(allocatedReservation));
                reservation = allocatedReservation;
                reservation.clearChildReservations();
            }
            else {
                // Create new reservation
                reservation = new Reservation();
            }
            reservation.setSlot(getInterval());
            if (sharedExecutable) {
                reservation.setExecutable(allocatedRoomEndpoint);
            }
            for (Reservation createdReservation : createdReservations) {
                addChildReservation(createdReservation);
            }
            return reservation;
        }
    }
}
