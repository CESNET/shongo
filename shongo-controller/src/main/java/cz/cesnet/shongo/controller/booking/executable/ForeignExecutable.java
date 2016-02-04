package cz.cesnet.shongo.controller.booking.executable;

/**
 * Interface specifying that {@link Executable} is managed by foreign domain.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public interface ForeignExecutable
{
    String getForeignReservationRequestId();
}
