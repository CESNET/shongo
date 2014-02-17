package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.ReservationRequestModificationModel;
import cz.cesnet.shongo.client.web.models.SpecificationType;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import org.joda.time.DateTime;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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
            @ModelAttribute(WizardParticipantsController.RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        return null;
    }
}
