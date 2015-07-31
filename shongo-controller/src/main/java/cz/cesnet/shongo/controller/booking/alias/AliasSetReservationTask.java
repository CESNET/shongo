package cz.cesnet.shongo.controller.booking.alias;

import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

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
     * Constructor.
     *
     * @param schedulerContext sets the {@link #schedulerContext}
     * @param slot             sets the {@link #slot}
     */
    public AliasSetReservationTask(SchedulerContext schedulerContext, Interval slot)
    {
        super(schedulerContext, slot);
    }

    /**
     * @param aliasSpecification to be added to the {@link #aliasSpecifications}
     */
    public void addAliasSpecification(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.add(aliasSpecification);
    }

    @Override
    protected Reservation allocateReservation(Reservation currentReservation) throws SchedulerException
    {
        validateReservationSlot(AliasReservation.class);

        if (aliasSpecifications.size() == 1) {
            AliasSpecification aliasSpecification = aliasSpecifications.get(0);
            AliasReservationTask aliasReservationTask =
                    aliasSpecification.createReservationTask(schedulerContext, slot);
            Reservation reservation = aliasReservationTask.perform();
            addReports(aliasReservationTask);
            return reservation;
        }
        else {

            // Process all alias specifications
            List<Reservation> createdReservations = new ArrayList<Reservation>();
            for (AliasSpecification aliasSpecification : aliasSpecifications) {
                // Allocate alias
                AliasReservationTask aliasReservationTask =
                        aliasSpecification.createReservationTask(schedulerContext, slot);
                Reservation reservation = aliasReservationTask.perform();
                addReports(aliasReservationTask);
                createdReservations.add(reservation);
            }

            // Allocate compound reservation
            Reservation reservation = new Reservation();
            reservation.setSlot(slot);
            for (Reservation createdReservation : createdReservations) {
                addChildReservation(createdReservation);
            }
            return reservation;
        }
    }
}
