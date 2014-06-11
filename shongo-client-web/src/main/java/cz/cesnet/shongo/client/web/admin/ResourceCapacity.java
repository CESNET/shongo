package cz.cesnet.shongo.client.web.admin;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.RecordingCapability;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;

/**
* TODO:
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public abstract class ResourceCapacity
{
    protected ResourceSummary resource;

    public ResourceCapacity(ResourceSummary resource)
    {
        this.resource = resource;
    }

    public String getClassName()
    {
        return getClass().getSimpleName();
    }

    public String getResourceId()
    {
        return resource.getId();
    }

    public String getResourceName()
    {
        return resource.getName();
    }

    public abstract ReservationSummary.Type getReservationType();

    public abstract String formatUtilization(ResourceCapacityUtilization utilization, FormatType type);

    public static enum FormatType
    {
        ABSOLUTE,
        RELATIVE
    }

    protected static abstract class LicenseCount extends ResourceCapacity
    {
        protected Integer licenseCount;

        public LicenseCount(ResourceSummary resource, Integer licenseCount)
        {
            super(resource);

            this.licenseCount = licenseCount;
        }

        @Override
        public String formatUtilization(ResourceCapacityUtilization utilization, FormatType type)
        {
            int utilizedLicenseCount = 0;
            if (utilization != null) {
                ResourceCapacityBucket peakBucket = utilization.getPeakBucket();
                utilizedLicenseCount = peakBucket.getLicenseCount();
            }
            StringBuilder output = new StringBuilder();
            switch (type) {
                case ABSOLUTE:
                    output.append(utilizedLicenseCount);
                    output.append("/");
                    output.append(licenseCount);
                    break;
                case RELATIVE:
                    output.append((utilizedLicenseCount * 100) / licenseCount);
                    output.append("%");
                    break;
                default:
                    throw new TodoImplementException(type);
            }
            if (utilizedLicenseCount > 0) {
                output.insert(0, "<strong>");
                output.append("</strong>");
            }
            return output.toString();
        }
    }

    public static class Room extends LicenseCount
    {
        public Room(ResourceSummary resource, RoomProviderCapability capability)
        {
            super(resource, capability.getLicenseCount());
        }

        @Override
        public ReservationSummary.Type getReservationType()
        {
            return ReservationSummary.Type.ROOM;
        }
    }

    public static class Recording extends LicenseCount
    {
        public Recording(ResourceSummary resource, RecordingCapability capability)
        {
            super(resource, capability.getLicenseCount());
        }

        @Override
        public ReservationSummary.Type getReservationType()
        {
            return ReservationSummary.Type.RECORDING_SERVICE;
        }
    }
}
