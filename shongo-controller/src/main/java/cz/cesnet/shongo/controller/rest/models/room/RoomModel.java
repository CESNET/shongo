package cz.cesnet.shongo.controller.rest.models.room;

import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.rest.models.TechnologyModel;
import cz.cesnet.shongo.controller.rest.models.TimeInterval;
import lombok.Data;
import org.joda.time.Interval;

/**
 * Represents room (executable).
 *
 * @author Filip Karnis
 */
@Data
public class RoomModel {

    private String id;
    private ExecutableSummary.Type type;
    private TimeInterval slot;
    private TimeInterval earliestSlot;
    private String description;
    private String name;
    private TechnologyModel technology;
    private RoomState state;
    private boolean isDeprecated;
    private int licenceCount;
    private int usageCount;

    public RoomModel(ExecutableSummary summary) {
        this.id = summary.getId();
        this.type = summary.getType();
        this.name = summary.getRoomName();
        this.description = summary.getRoomDescription();
        this.technology = TechnologyModel.find(summary.getRoomTechnologies());
        this.usageCount = summary.getRoomUsageCount();

        this.state = RoomState.fromRoomState(
                summary.getState(), summary.getRoomLicenseCount(),
                summary.getRoomUsageState());

        Interval slot = summary.getRoomUsageSlot();
        if (slot == null) {
            slot = summary.getSlot();
        }
        this.slot = new TimeInterval(slot);

        boolean isDeprecated;
        switch (state) {
            case STARTED:
            case STARTED_AVAILABLE:
                isDeprecated = false;
                break;
            default:
                isDeprecated = slot.getEnd().isBeforeNow();
                break;
        }
        this.isDeprecated = isDeprecated;

        Integer licenseCount = summary.getRoomUsageLicenseCount();
        if (licenseCount == null) {
            licenseCount = summary.getRoomLicenseCount();
        }
        this.licenceCount = licenseCount;
    }
}
