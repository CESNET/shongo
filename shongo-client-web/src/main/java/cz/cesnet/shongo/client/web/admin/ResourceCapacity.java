package cz.cesnet.shongo.client.web.admin;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.RecordingCapability;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public abstract String getCssClass(ResourceCapacityUtilization utilization);

    public abstract String formatUtilization(ResourceCapacityUtilization utilization, FormatType t, FormatStyle s);

    public static enum FormatType
    {
        MAXIMUM,
        AVERAGE
    }

    public static enum FormatStyle
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

        public Integer getLicenseCount()
        {
            return licenseCount;
        }

        @Override
        public String getCssClass(ResourceCapacityUtilization utilization)
        {
            Set<String> cssClasses = new HashSet<String>();
            int utilizedLicenseCount = getUtilizedLicenseCount(utilization, FormatType.MAXIMUM).intValue();
            int relativeUtilizedLicenseCount = (utilizedLicenseCount * 100) / licenseCount;
            if (utilizedLicenseCount > 0) {
                cssClasses.add("utilized");
                cssClasses.add("utilized" + ((relativeUtilizedLicenseCount / 10) * 10));
            }
            return StringUtils.join(cssClasses, " ");
        }

        @Override
        public String formatUtilization(ResourceCapacityUtilization utilization, FormatType type, FormatStyle style)
        {
            Number utilizedLicenseCount = getUtilizedLicenseCount(utilization, type);
            StringBuilder output = new StringBuilder();
            switch (style) {
                case ABSOLUTE:
                    String value = String.format("%1$.1f", utilizedLicenseCount.doubleValue());
                    if (value.endsWith(".0")) {
                        value = String.valueOf(utilizedLicenseCount.intValue());
                    }
                    output.append(value);
                    break;
                case RELATIVE:
                    output.append((utilizedLicenseCount.intValue() * 100) / licenseCount);
                    output.append("%");
                    break;
                default:
                    throw new TodoImplementException(style);
            }
            return output.toString();
        }

        private Number getUtilizedLicenseCount(ResourceCapacityUtilization utilization, FormatType type)
        {
            if (utilization != null) {
                switch (type) {
                    case MAXIMUM:
                        ResourceCapacityBucket peakBucket = utilization.getPeakBucket();
                        return peakBucket.getLicenseCount();
                    case AVERAGE:
                        List<ResourceCapacityBucket> buckets = utilization.getBuckets();
                        double totalLicenseCount = 0;
                        int bucketCount = 0;
                        for (ResourceCapacityBucket bucket : buckets) {
                            if (bucket.isEmpty()) {
                                continue;
                            }
                            totalLicenseCount += bucket.getLicenseCount();
                            bucketCount++;
                        }
                        if (bucketCount == 0) {
                            return 0;
                        }
                        return totalLicenseCount / (double) bucketCount;
                    default:
                        throw new TodoImplementException(type);
                }
            }
            else {
                return 0;
            }
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
