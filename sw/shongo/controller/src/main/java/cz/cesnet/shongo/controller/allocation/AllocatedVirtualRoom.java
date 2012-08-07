package cz.cesnet.shongo.controller.allocation;

import org.joda.time.Duration;
import org.joda.time.Period;

import javax.persistence.Entity;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AllocatedVirtualRoom extends AllocatedDevice
{
    /**
     * Maximum duration of allocated virtual room.
     */
    public static Duration MAXIMUM_DURATION = Duration.standardDays(1);

    /**
     * Allocated port count.
     */
    private Integer portCount;

    /**
     * @return {@link #portCount}
     */
    public Integer getPortCount()
    {
        return portCount;
    }

    /**
     * @param portCount {@link #portCount}
     */
    public void setPortCount(Integer portCount)
    {
        this.portCount = portCount;
    }
}
