package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.util.RangeSet;
import org.joda.time.DateTime;

/**
 * {@link cz.cesnet.shongo.util.RangeSet.Bucket} for {@link RecordingServiceReservation}s.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class RecordingBucket extends RangeSet.Bucket<DateTime, RecordingServiceReservation>
{
    /**
     * Sum of {@link RecordingServiceReservation}
     */
    private int licenseCount = 0;

    /**
     * Constructor.
     *
     * @param rangeValue
     */
    public RecordingBucket(DateTime rangeValue)
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
    public boolean add(RecordingServiceReservation roomReservation)
    {
        if (super.add(roomReservation)) {
            this.licenseCount ++;
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
            if (object instanceof RecordingServiceReservation) {
                throw new IllegalArgumentException("Object must be RecordingServiceReservation.class, but is: " + object.getClass());
            }
            this.licenseCount --;
            return true;
        }
        else {
            return false;
        }
    }
}
