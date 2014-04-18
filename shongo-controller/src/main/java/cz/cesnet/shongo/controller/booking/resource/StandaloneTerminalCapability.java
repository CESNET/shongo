package cz.cesnet.shongo.controller.booking.resource;

import javax.persistence.Entity;

/**
 * Capability tells that the device can participate even in 2-point calls
 * (that means without a device with {@link cz.cesnet.shongo.controller.booking.room.RoomProviderCapability ).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class StandaloneTerminalCapability extends TerminalCapability
{
    @Override
    public cz.cesnet.shongo.controller.api.Capability createApi()
    {
        return new cz.cesnet.shongo.controller.api.StandaloneTerminalCapability();
    }
}
