package cz.cesnet.shongo.controller.resource;

import javax.persistence.Entity;

/**
 * Capability tells that the device can participate even in 2-point videoconference call
 * (that means without a device with {@link VirtualRoomsCapability).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class StandaloneTerminalCapability extends TerminalCapability
{
}
