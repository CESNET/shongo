package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.SpecificationType;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.editors.*;
import cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.joda.time.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;

/**
 * Controller for common wizard actions.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({
        WizardParticipantsController.RESERVATION_REQUEST_ATTRIBUTE
})
public class WizardController
{
    public static final String SUBMIT_RESERVATION_REQUEST = "javascript: " +
            "document.getElementById('reservationRequest').submit();";

    public static final String SUBMIT_RESERVATION_REQUEST_FINISH = "javascript: " +
            "$('form#reservationRequest').append('<input type=\\'hidden\\' name=\\'finish\\' value=\\'true\\'/>');" +
            "document.getElementById('reservationRequest').submit();";

    @Resource
    private Cache cache;

    /**
     * Initialize model editors for additional types.
     *
     * @param binder to be initialized
     */
    @InitBinder
    public void initBinder(WebDataBinder binder, DateTimeZone timeZone)
    {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor(timeZone));
        binder.registerCustomEditor(DateTimeZone.class, new DateTimeZoneEditor());
        binder.registerCustomEditor(LocalDate.class, new LocalDateEditor());
        binder.registerCustomEditor(LocalDateTime.class, new LocalDateTimeEditor());
    }

    /**
     * Handle duplication of an existing reservation request.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_DUPLICATE, method = RequestMethod.GET)
    public String handleRoomDuplicate(
            HttpServletRequest request,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        ReservationRequestSummary reservationRequest =
                cache.getReservationRequestSummary(securityToken, reservationRequestId);
        SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);
        switch (specificationType) {
            case ADHOC_ROOM:
            case PERMANENT_ROOM:
                return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.format(
                        ClientWebUrl.WIZARD_ROOM_DUPLICATE, reservationRequestId));
            case PERMANENT_ROOM_CAPACITY:
                return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.format(
                        ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_DUPLICATE, reservationRequestId));
            default:
                throw new TodoImplementException(specificationType);
        }
    }

    /**
     * Modify existing virtual room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MODIFY, method = RequestMethod.GET)
    public String handleRoomModify(
            HttpServletRequest request,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        ReservationRequestSummary reservationRequest =
                cache.getReservationRequestSummary(securityToken, reservationRequestId);
        SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);
        switch (specificationType) {
            case ADHOC_ROOM:
            case PERMANENT_ROOM:
                return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.format(
                        ClientWebUrl.WIZARD_ROOM_MODIFY, reservationRequestId));
            case PERMANENT_ROOM_CAPACITY:
                return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.format(
                        ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_MODIFY, reservationRequestId));
            case MEETING_ROOM:
                return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.format(
                        ClientWebUrl.WIZARD_MEETING_ROOM_MODIFY, reservationRequestId));
            default:
                throw new TodoImplementException(specificationType);
        }
    }

    /**
     * Update reservation request in memory.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_UPDATE, method = {RequestMethod.POST})
    @ResponseBody
    public Object handleUpdate(
            @ModelAttribute(
                    WizardParticipantsController.RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        return null;
    }

    /**
     * Handle periodic events request.
     *
     * @param request
     * @return periodic events (null means "and more")
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERIODIC_EVENTS, method = RequestMethod.POST)
    public
    @ResponseBody
    List<String> handlePeriodicEvents(
            DateTimeZone currentTimeZone,
            @RequestBody PeriodicEventsRequest request)
    {
        DateTimeZone timeZone = request.getTimeZone();
        if (timeZone == null) {
            timeZone = currentTimeZone;
        }
        DateTime start = request.getStart().toDateTime(timeZone);
        Period period = request.getPeriodicityType().toPeriod();
        LocalDate periodicityEnd = request.getPeriodicityEnd();

        List<String> periodicEvents = new LinkedList<String>();
        int availableCount = request.getMaxCount();
        while (periodicityEnd == null || !start.isAfter(periodicityEnd.toDateTime(start))) {
            if (availableCount <= 0) {
                periodicEvents.add(null);
                break;
            }
            // Add event
            periodicEvents.add(start.withZone(timeZone).toString());

            // Prepare next event
            start = start.plus(period);
            availableCount--;
        }
        return periodicEvents;
    }

    public static class PeriodicEventsRequest
    {
        private Integer maxCount = 10;

        private DateTimeZone timeZone;

        private LocalDateTime start;

        private PeriodicDateTimeSlot.PeriodicityType periodicityType;

        private LocalDate periodicityEnd;

        public Integer getMaxCount()
        {
            return maxCount;
        }

        public void setMaxCount(Integer maxCount)
        {
            this.maxCount = maxCount;
        }

        public DateTimeZone getTimeZone()
        {
            return timeZone;
        }

        @JsonDeserialize(using = DateTimeZoneDeserializer.class)
        public void setTimeZone(DateTimeZone timeZone)
        {
            this.timeZone = timeZone;
        }

        public LocalDateTime getStart()
        {
            return start;
        }

        public void setStart(LocalDateTime start)
        {
            this.start = start;
        }

        public PeriodicDateTimeSlot.PeriodicityType getPeriodicityType()
        {
            return periodicityType;
        }

        public void setPeriodicityType(PeriodicDateTimeSlot.PeriodicityType periodicityType)
        {
            this.periodicityType = periodicityType;
        }

        public LocalDate getPeriodicityEnd()
        {
            return periodicityEnd;
        }

        public void setPeriodicityEnd(LocalDate periodicityEnd)
        {
            this.periodicityEnd = periodicityEnd;
        }
    }
}
