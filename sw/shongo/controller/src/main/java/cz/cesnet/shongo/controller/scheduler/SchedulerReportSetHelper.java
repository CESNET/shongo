package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerReportSetHelper
{
    public static String formatResource(Resource resource)
    {
        return String.format("%s '%s'",
                (resource instanceof DeviceResource ? "device" : "resource"),
                EntityIdentifier.formatId(resource));
    }
}
