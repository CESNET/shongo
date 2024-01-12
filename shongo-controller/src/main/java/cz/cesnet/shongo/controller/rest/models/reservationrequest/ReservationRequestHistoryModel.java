package cz.cesnet.shongo.controller.rest.models.reservationrequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import cz.cesnet.shongo.controller.api.AllocationState;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
import cz.cesnet.shongo.controller.rest.CacheProvider;
import lombok.Data;
import org.joda.time.DateTime;

import static cz.cesnet.shongo.controller.rest.models.TimeInterval.ISO_8601_PATTERN;

/**
 * Represents reservation request's history.
 *
 * @author Filip Karnis
 */
@Data
public class ReservationRequestHistoryModel
{

    private String id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ISO_8601_PATTERN)
    private DateTime createdAt;
    private String createdBy;
    private ReservationRequestType type;
    private AllocationState allocationState;
    private ReservationRequestState state;

    public ReservationRequestHistoryModel(ReservationRequestSummary summary, CacheProvider cacheProvider)
    {
        this.id = summary.getId();
        this.createdAt = summary.getDateTime();
        this.createdBy = cacheProvider.getUserInformation(summary.getUserId()).getFullName();
        this.type = summary.getType();
        this.allocationState = summary.getAllocationState();
        this.state = ReservationRequestState.fromApi(summary);
    }
}
