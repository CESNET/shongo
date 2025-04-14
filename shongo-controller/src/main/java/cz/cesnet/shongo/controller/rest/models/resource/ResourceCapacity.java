package cz.cesnet.shongo.controller.rest.models.resource;

import cz.cesnet.shongo.controller.api.RecordingCapability;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;

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
