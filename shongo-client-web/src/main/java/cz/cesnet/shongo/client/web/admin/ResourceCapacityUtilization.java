package cz.cesnet.shongo.client.web.admin;

import cz.cesnet.shongo.controller.api.ReservationSummary;
import org.joda.time.Interval;

import java.util.*;

/**
* TODO:
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public class ResourceCapacityUtilization
{
    private List<ResourceCapacityBucket> buckets = new LinkedList<ResourceCapacityBucket>();

    private List<ReservationSummary> reservations;

    public ResourceCapacityUtilization(Collection<ResourceCapacityBucket> buckets)
    {
        this.buckets.addAll(buckets);
        Collections.sort(this.buckets);
    }

    public ResourceCapacityBucket getPeakBucket()
    {
        if (buckets.size() > 0) {
            return buckets.get(0);
        }
        else {
            return null;
        }
    }

    public Collection<ReservationSummary> getReservations()
    {
        if (this.reservations == null) {
            Set<ReservationSummary> reservations = new LinkedHashSet<ReservationSummary>();
            for (ResourceCapacityBucket bucket : buckets) {
                reservations.addAll(bucket);
            }
            this.reservations = new LinkedList<ReservationSummary>();
            this.reservations.addAll(reservations);
            Collections.sort(this.reservations, new Comparator<ReservationSummary>()
            {
                @Override
                public int compare(ReservationSummary reservation1, ReservationSummary reservation2)
                {
                    Interval reservationSlot1 = reservation1.getSlot();
                    Interval reservationSlot2 = reservation2.getSlot();

                    int result = reservationSlot1.getStart().compareTo(reservationSlot2.getStart());
                    if (result != 0) {
                        return result;
                    }

                    return reservationSlot1.getEnd().compareTo(reservationSlot2.getEnd());
                }
            });
        }
        return this.reservations;
    }

    public Collection<String> getReservationUserIds()
    {
        Set<String> userIds = new HashSet<String>();
        for (ReservationSummary reservation : getReservations()) {
            userIds.add(reservation.getUserId());
        }
        return userIds;
    }
}
