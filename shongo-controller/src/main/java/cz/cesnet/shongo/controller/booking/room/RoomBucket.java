package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.util.RangeSet;
import org.joda.time.DateTime;

import static cz.cesnet.shongo.util.RangeSet.Bucket;

/**
 * {@link cz.cesnet.shongo.util.RangeSet.Bucket} for {@link RoomReservation}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomBucket extends RangeSet.Bucket<DateTime, RoomReservation>
{
    /**
     * Sum of {@link RoomReservation#getLicenseCount()}
     */
    private int licenseCount = 0;

    /**
     * Constructor.
     *
     * @param rangeValue
     */
    public RoomBucket(DateTime rangeValue)
    {
        super(rangeValue);
    }

    /**
     * @return {@link #licenseCount}
     */
    public int getLicenseCount()
    {
        return licenseCount;
    }

    @Override
    public boolean add(RoomReservation roomReservation)
    {
        if (super.add(roomReservation)) {
            this.licenseCount += roomReservation.getLicenseCount();
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
            RoomReservation roomReservation = (RoomReservation) object;
            this.licenseCount -= roomReservation.getLicenseCount();
            return true;
        }
        else {
            return false;
        }
    }
}
