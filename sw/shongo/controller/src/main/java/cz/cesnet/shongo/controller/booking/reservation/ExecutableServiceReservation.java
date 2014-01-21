package cz.cesnet.shongo.controller.booking.reservation;

import cz.cesnet.shongo.controller.booking.executable.ExecutableService;

import javax.persistence.*;

/**
 * Represents a {@link Reservation} for a {@link ExecutableService}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class ExecutableServiceReservation extends TargetedReservation
{
    /**
     * {@link ExecutableService} which is allocated.
     */
    private ExecutableService executableService;

    /**
     * Constructor.
     */
    public ExecutableServiceReservation()
    {
    }

    /**
     * @return {@link #executableService}
     */
    @OneToOne(cascade = CascadeType.PERSIST, optional = false, fetch = FetchType.LAZY)
    @Access(AccessType.FIELD)
    public ExecutableService getExecutableService()
    {
        return executableService;
    }

    /**
     * @param executableService sets the {@link #executableService}
     */
    public void setExecutableService(ExecutableService executableService)
    {
        this.executableService = executableService;
    }
}
