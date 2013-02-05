package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Set;

/**
 * Represents a cache of {@link ExistingReservation}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReusedReservationCache extends AbstractReservationCache<Reservation, ExistingReservation>
{
    private static Logger logger = LoggerFactory.getLogger(ReusedReservationCache.class);

    public void loadObjects(EntityManager entityManager)
    {
        logger.debug("Loading reused reservations...");

        ReservationManager reservationManager = new ReservationManager(entityManager);
        List<Reservation> reusedReservations = reservationManager.getReusedReservations();
        for (Reservation reusedReservation : reusedReservations) {
            addObject(reusedReservation, entityManager);
        }
    }

    @Override
    protected void updateObjectState(Reservation object, Interval workingInterval, EntityManager entityManager)
    {
        ReservationManager reservationManager = new ReservationManager(entityManager);
        List<ExistingReservation> existingReservations = reservationManager.getExistingReservations(object);
        for (ExistingReservation existingReservation : existingReservations) {
            addReservation(object, existingReservation);
        }
    }

    @Override
    protected void onAddReservation(Reservation object, ExistingReservation reservation)
    {
        if (getObject(object.getId()) == null) {
            addObject(object);
        }
        super.onAddReservation(object, reservation);
    }

    @Override
    public void onRemoveReservation(Reservation object, ExistingReservation reservation)
    {
        super.onRemoveReservation(object, reservation);
        ObjectState<ExistingReservation> objectState = getObjectStateRequired(object);
        if (objectState.isEmpty()) {
            removeObject(object);
        }
    }

    /**
     *
     * @param providedReservation
     * @param interval
     * @return
     */
    public boolean isProvidedReservationAvailable(Reservation providedReservation, Interval interval)
    {
        ObjectState<ExistingReservation> objectState = getObjectState(providedReservation);
        if (objectState == null) {
            return true;
        }
        Set<ExistingReservation> existingReservations = objectState.getReservations(interval);
        return existingReservations.size() == 0;
    }
}
