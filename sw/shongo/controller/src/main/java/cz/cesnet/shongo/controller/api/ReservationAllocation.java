package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.common.api.TimeSlot;

import java.util.Map;

/**
 * Reservation allocation
 *
 * @author Martin Srom
 */
public class ReservationAllocation
{
    /** Map of resources that were allocated to slots */
    private Map<TimeSlot, String[]> slotResources;

    public Map<TimeSlot, String[]> getSlotResources() {
        return slotResources;
    }

    public void setSlotResources(Map<TimeSlot, String[]> slotResources) {
        this.slotResources = slotResources;
    }
}
