package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ParticipantModel;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * Controller for managing participants for reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({ReservationRequestParticipantController.PARTICIPANT_ATTRIBUTE})
public class ReservationRequestParticipantController
{
    protected static final String PARTICIPANT_ATTRIBUTE = "participant";

    @Resource
    private ReservationService reservationService;

    @Resource
    private Cache cache;

    private ReservationRequestModel getReservationRequest(SecurityToken securityToken, String reservationRequestId)
    {
        ReservationRequestModel reservationRequestModel = new ReservationRequestModel(
                reservationService.getReservationRequest(securityToken, reservationRequestId),
                new CacheProvider(cache, securityToken));
        return reservationRequestModel;
    }

    /**
     * Show form for adding new participant for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_CREATE, method = RequestMethod.GET)
    public ModelAndView handleParticipantCreate(
            SecurityToken securityToken,
            @PathVariable("reservationRequestId") String reservationRequestId)
    {
        ModelAndView modelAndView = new ModelAndView("reservationRequestParticipant");
        modelAndView.addObject(PARTICIPANT_ATTRIBUTE, new ParticipantModel(new CacheProvider(cache, securityToken)));
        return modelAndView;
    }

    /**
     * Store new {@code participant} to reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_CREATE, method = RequestMethod.POST)
    public String handleParticipantCreateProcess(
            HttpSession httpSession,
            SessionStatus sessionStatus,
            SecurityToken securityToken,
            @PathVariable("reservationRequestId") String reservationRequestId,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(securityToken, reservationRequestId);
        if (ParticipantModel.createParticipant(reservationRequest, participant, bindingResult)) {
            sessionStatus.setComplete();
            return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESERVATION_REQUEST_DETAIL, reservationRequestId);
        }
        else {
            return "reservationRequestParticipant";
        }
    }

    /**
     * Show form for modifying existing participant for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_MODIFY, method = RequestMethod.GET)
    public ModelAndView handleParticipantModify(
            SecurityToken securityToken,
            @PathVariable("reservationRequestId") String reservationRequestId,
            @PathVariable("participantId") String participantId)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(securityToken, reservationRequestId);
        ParticipantModel participant = ParticipantModel.getParticipant(reservationRequest, participantId);
        ModelAndView modelAndView = new ModelAndView("reservationRequestParticipant");
        modelAndView.addObject(PARTICIPANT_ATTRIBUTE, participant);
        return modelAndView;
    }

    /**
     * Store changes for existing {@code participant} to reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_MODIFY, method = RequestMethod.POST)
    public String handleParticipantModifyProcess(
            HttpSession httpSession,
            SessionStatus sessionStatus,
            SecurityToken securityToken,
            @PathVariable("reservationRequestId") String reservationRequestId,
            @PathVariable("participantId") String participantId,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(securityToken, reservationRequestId);
        if (ParticipantModel.modifyParticipant(reservationRequest, participantId, participant, bindingResult)) {
            sessionStatus.setComplete();
            return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESERVATION_REQUEST_DETAIL, reservationRequestId);
        }
        else {
            return "reservationRequestParticipant";
        }
    }

    /**
     * Delete existing {@code participant} from reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_DELETE, method = RequestMethod.GET)
    public String handleParticipantDelete(
            SecurityToken securityToken,
            @PathVariable("reservationRequestId") String reservationRequestId,
            @PathVariable("participantId") String participantId)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(securityToken, reservationRequestId);
        ParticipantModel.deleteParticipant(reservationRequest, participantId);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESERVATION_REQUEST_DETAIL, reservationRequestId);
    }
}
