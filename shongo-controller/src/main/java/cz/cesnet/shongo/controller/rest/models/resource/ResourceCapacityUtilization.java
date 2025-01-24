package cz.cesnet.shongo.controller.rest.models.resource;

import cz.cesnet.shongo.controller.api.ReservationSummary;
import org.joda.time.Interval;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Represents a utilization of {@link ResourceCapacity} for a specific interval.
 * <p/>
 * It can be initialized from list of {@link ResourceCapacityBucket}s (which contain all reservations in interval).
 * From the buckets we can determine maximum utilization or compute average utilization.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceCapacityUtilization
{
    /**
     * List of {@link ResourceCapacityBucket}s. Each bucket represents an interval in which {@link ResourceCapacity} is
     * utilized in some way. Different buckets means different utilization. Buckets shall be sorted to be able to
     * determine maximum utilization.
     */
    private final List<ResourceCapacityBucket> buckets = new LinkedList<>();

    /**
     * List of {@link ReservationSummary} in all {@link #buckets}.
     */
    private List<ReservationSummary> reservations;

    /**
     * Constructor.
     *
     * @param buckets sets the {@link #buckets}
     */
    public ResourceCapacityUtilization(Collection<ResourceCapacityBucket> buckets)
    {
        this.buckets.addAll(buckets);

        // Sort buckets (to be able to determine maximum utilization)
        Collections.sort(this.buckets);
    }

    /**
     * @return first {@link ResourceCapacityBucket} from {@link #buckets} with maximum utilization.
     */
    public ResourceCapacityBucket getPeakBucket()
    {
        if (buckets.size() > 0) {
            return buckets.get(0);
        }
        else {
            return null;
        }
    }

    /**
     * @return {@link #buckets}
     */
    public List<ResourceCapacityBucket> getBuckets()
    {
        return Collections.unmodifiableList(buckets);
    }

    /**
     * @return {@link #reservations}
     */
    public Collection<ReservationSummary> getReservations()
    {
        if (this.reservations == null) {
            Set<ReservationSummary> reservations = new LinkedHashSet<ReservationSummary>();
            for (ResourceCapacityBucket bucket : buckets) {
                reservations.addAll(bucket);
            }
            this.reservations = new LinkedList<>();
            this.reservations.addAll(reservations);
            this.reservations.sort((reservation1, reservation2) -> {
                Interval reservationSlot1 = reservation1.getSlot();
                Interval reservationSlot2 = reservation2.getSlot();

                int result = reservationSlot1.getStart().compareTo(reservationSlot2.getStart());
                if (result != 0) {
                    return result;
                }

                return reservationSlot1.getEnd().compareTo(reservationSlot2.getEnd());
            });
        }
        return Collections.unmodifiableList(this.reservations);
    }

    /**
     * @return user-ids of {@link #reservations}
     */
    public Collection<String> getReservationUserIds()
    {
        Set<String> userIds = new HashSet<>();
        for (ReservationSummary reservation : getReservations()) {
            userIds.add(reservation.getUserId());
        }
        return userIds;
    }
}
