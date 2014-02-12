package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.ReservationRequestModificationModel;
import cz.cesnet.shongo.client.web.models.SpecificationType;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import org.joda.time.DateTime;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.WebUtils;

import javax.annotation.Resource;

/**
 * Controller for common wizard actions.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class WizardController
{
    @Resource
    private Cache cache;

    /**
     * Handle duplication of an existing reservation request.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_DUPLICATE, method = RequestMethod.GET)
    public String handleRoomDuplicate(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        ReservationRequestSummary reservationRequest =
                cache.getReservationRequestSummary(securityToken, reservationRequestId);
        SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);
        switch (specificationType) {
            case ADHOC_ROOM:
            case PERMANENT_ROOM:
                return "redirect:" + ClientWebUrl.format(
                        ClientWebUrl.WIZARD_ROOM_DUPLICATE, reservationRequestId);
            case PERMANENT_ROOM_CAPACITY:
                return "redirect:" + ClientWebUrl.format(
                        ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_DUPLICATE, reservationRequestId);
            default:
                throw new TodoImplementException(specificationType);
        }
    }

    /**
     * Modify existing virtual room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MODIFY, method = RequestMethod.GET)
    public String handleRoomModify(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        ReservationRequestSummary reservationRequest =
                cache.getReservationRequestSummary(securityToken, reservationRequestId);
        SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);
        switch (specificationType) {
            case ADHOC_ROOM:
            case PERMANENT_ROOM:
                return "redirect:" + ClientWebUrl.format(
                        ClientWebUrl.WIZARD_ROOM_MODIFY, reservationRequestId);
            case PERMANENT_ROOM_CAPACITY:
                return "redirect:" + ClientWebUrl.format(
                        ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_MODIFY, reservationRequestId);
            default:
                throw new TodoImplementException(specificationType);
        }
    }
}
