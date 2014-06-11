package cz.cesnet.shongo.client.web.admin;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.util.RangeSet;
import org.joda.time.DateTime;

/**
* TODO:
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public class ResourceCapacityBucket extends RangeSet.Bucket<DateTime, ReservationSummary>
        implements Comparable<ResourceCapacityBucket>
{
    /**
     * Sum of {@link cz.cesnet.shongo.controller.api.RoomReservation#getLicenseCount()}
     */
    private int licenseCount = 0;

    /**
     * Constructor.
     *
     * @param rangeValue
     */
    public ResourceCapacityBucket(DateTime rangeValue)
    {
        super(rangeValue);
    }

    /**
     * @return {@link #rangeValue}
     */
    public DateTime getDateTime()
    {
        return getRangeValue();
    }

    /**
     * @return {@link #licenseCount}
     */
    public int getLicenseCount()
    {
        return licenseCount;
    }

    @Override
    public boolean add(ReservationSummary reservation)
    {
        if (super.add(reservation)) {
            switch (reservation.getType()) {
                case ROOM:
                    this.licenseCount += reservation.getRoomLicenseCount();
                    break;
                case RECORDING_SERVICE:
                    this.licenseCount++;
                    break;
                default:
                    throw new TodoImplementException(reservation.getType());
            }
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public boolean remove(Object object)
    {
        if (super.remove(object)) {
            ReservationSummary reservation = (ReservationSummary) object;
            switch (reservation.getType()) {
                case ROOM:
                    this.licenseCount -= reservation.getRoomLicenseCount();
                    break;
                case RECORDING_SERVICE:
                    this.licenseCount--;
                    break;
                default:
                    throw new TodoImplementException(reservation.getType());
            }
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public int compareTo(ResourceCapacityBucket bucket)
    {
        return -Double.compare(getLicenseCount(), bucket.getLicenseCount());
    }
}
