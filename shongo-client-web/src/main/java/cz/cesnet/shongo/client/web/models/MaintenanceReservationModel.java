package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.ReservationRequest;
import cz.cesnet.shongo.controller.api.ResourceSpecification;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;


/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class MaintenanceReservationModel {

    private String resourceId;

    private String description;

    private DateTimeZone timeZone;

    private LocalTime start;

    private LocalDate startDate;

    private LocalTime end;

    private LocalDate endDate;

    private Integer priority;

    public MaintenanceReservationModel() {
    }

    /**
     * Type of duration unit.
     */
    public static enum DurationType
    {
        MINUTE,
        HOUR,
        DAY
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DateTimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(DateTimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public LocalTime getStart() {
        return start;
    }

    public void setStart(LocalTime start) {
        this.start = start;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LocalTime getEnd() {
        return end;
    }

    public void setEnd(LocalTime end) {
        this.end = end;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public DateTime getStartDateTime() {
        return getStartDate().toDateTime(getStart(), getTimeZone());
    }

    public DateTime getEndDateTime() {
        return getEndDate().toDateTime(getEnd(), getTimeZone());
    }

    public ReservationRequest toApi () {
        ReservationRequest reservationRequest = new ReservationRequest();
        ResourceSpecification specification = new ResourceSpecification();
        specification.setResourceId(resourceId);
        reservationRequest.setSpecification(specification);
        reservationRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        reservationRequest.setDescription(description);
        reservationRequest.setSlot(getStartDateTime(), getEndDateTime());
        reservationRequest.setPriority(priority);
        return reservationRequest;
    }
}
