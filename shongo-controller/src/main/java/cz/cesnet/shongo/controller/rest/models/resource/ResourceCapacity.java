package cz.cesnet.shongo.controller.rest.models.resource;

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
 * Represents a type of capacity which can be utilized in a resource.
 * <p>
 * Theoretically multiple different capacities can be utilized for a single resource
 * (e.g., capacity of room licenses and recording capacity)
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ResourceCapacity
{
    /**
     * {@link ResourceSummary} of the resource in which the capacity is utilized.
     */
    protected ResourceSummary resource;

    /**
     * Constructor.
     *
     * @param resource sets the {@link #resource}
     */
    public ResourceCapacity(ResourceSummary resource)
    {
        this.resource = resource;
    }

    /**
     * @return class name of this type of capacity
     */
    public String getClassName()
    {
        return getClass().getSimpleName();
    }

    /**
     * @return {@link ResourceSummary#getId()} for {@link #resource}
     */
    public String getResourceId()
    {
        return resource.getId();
    }

    /**
     * @return {@link ResourceSummary#getName()} for {@link #resource}
     */
    public String getResourceName()
    {
        return resource.getName();
    }

    /**
     * @return {@link ReservationSummary.Type} of reservations which allocate this resource capacity
     * (e.g., capacity of room licenses are allocated by {@link ReservationSummary.Type#ROOM})
     */
    public abstract ReservationSummary.Type getReservationType();

    /**
     * @param utilization
     * @return css classes which shall be rendered for given {@code utilization}
     */
    public abstract String getCssClass(ResourceCapacityUtilization utilization);

    /**
     * @param utilization
     * @param t           {@link FormatType} which should be used
     * @param s           {@link FormatStyle} which should be used
     * @return html content which shall be rendered for given {@code utilization}
     */
    public abstract String formatUtilization(ResourceCapacityUtilization utilization, FormatType t, FormatStyle s);

    /**
     * Specifies what utilization should be formatted in {@link #formatUtilization}.
     */
    public enum FormatType
    {
        /**
         * Maximum utilization of resource capacity (e.g., maximum number of utilized license count).
         */
        MAXIMUM,

        /**
         * Average utilization of resource capacity (e.g., average of all utilizations).
         */
        AVERAGE
    }

    /**
     * Specifies how the utilization value should be formatted in {@link #formatUtilization}.
     */
    public enum FormatStyle
    {
        /**
         * Absolute utilization (e.g., number of licenses).
         */
        ABSOLUTE,

        /**
         * Percentage utilization (e.g., percentage of utilization).
         */
        RELATIVE
    }

    /**
     * Abstract {@link ResourceCapacity} for types with license count (e.g., recording capacity or room capacity).
     */
    protected static abstract class LicenseCount extends ResourceCapacity
    {
        /**
         * Total number of available licenses in the resource.
         */
        protected Integer licenseCount;

        /**
         * Constructor.
         *
         * @param resource     sets the {@link #resource}
         * @param licenseCount sets the {@link #licenseCount}
         */
        public LicenseCount(ResourceSummary resource, Integer licenseCount)
        {
            super(resource);

            this.licenseCount = licenseCount;
        }

        /**
         * @return {@link #licenseCount}
         */
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
                    output.append((int) ((utilizedLicenseCount.doubleValue() * 100.0) / (double) licenseCount));
                    output.append("%");
                    break;
                default:
                    throw new TodoImplementException(style);
            }
            return output.toString();
        }

        /**
         * @param utilization
         * @param type
         * @return utilized license count for given {@code type}
         */
        private Number getUtilizedLicenseCount(ResourceCapacityUtilization utilization, FormatType type)
        {
            if (utilization != null) {
                switch (type) {
                    case MAXIMUM:
                        ResourceCapacityBucket peakBucket = utilization.getPeakBucket();
                        if (peakBucket != null) {
                            return peakBucket.getLicenseCount();
                        }
                        else {
                            return 0;
                        }
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

    /**
     * {@link ResourceCapacity} for resources which provides virtual rooms.
     */
    public static class Room extends LicenseCount
    {
        /**
         * Constructor.
         *
         * @param resource   sets the {@link #resource}
         * @param capability to be used for determining available license count
         */
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

    /**
     * {@link ResourceCapacity} for resources which provides recording.
     */
    public static class Recording extends LicenseCount
    {
        /**
         * Constructor.
         *
         * @param resource   sets the {@link #resource}
         * @param capability to be used for determining available license count
         */
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
