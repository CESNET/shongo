package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.editors.PeriodEditor;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@RequestMapping("/reservation-request")
@SessionAttributes("reservationRequest")
public class ReservationRequestUpdateController
{
    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor());
        binder.registerCustomEditor(Period.class, new PeriodEditor());
    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String getCreate(
            @RequestParam(value = "type", required = false) ReservationRequestModel.SpecificationType type,
            Model model)
    {
        ReservationRequestModel reservationRequest = new ReservationRequestModel();
        reservationRequest.setType(type);
        model.addAttribute("reservationRequest", reservationRequest);
        return "reservationRequestCreate";
    }

    @RequestMapping(value = "/create/confirmed", method = {RequestMethod.POST, RequestMethod.GET})
    public String getCreateConfirmed(
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel,
            BindingResult result)
    {
        reservationRequestModel.validate(result);
        if (result.hasErrors()) {
            return "reservationRequestCreate";
        }
        reservationRequestModel.setId("shongo:cz.cesnet:req:33");
        return "reservationRequestDetail";
    }

    @RequestMapping(value = "/modify/{id:.+}", method = RequestMethod.GET)
    public String getModify(@PathVariable(value = "id") String reservationRequestId, Model model)
    {
        ReservationRequestModel reservationRequest = new ReservationRequestModel();
        reservationRequest.setId(reservationRequestId);
        reservationRequest.setDescription("test");
        reservationRequest.setPurpose("EDUCATION");
        reservationRequest.setTechnology(ReservationRequestModel.Technology.H323_SIP);
        reservationRequest.setType(ReservationRequestModel.SpecificationType.ROOM);
        reservationRequest.setStart(DateTime.parse("2012-06-01T12:00"));
        reservationRequest.setEnd(DateTime.parse("2012-06-01T14:00"));
        reservationRequest.setDurationCount(2);
        reservationRequest.setDurationType(ReservationRequestModel.DurationType.HOUR);
        ReservationRequestModel.RoomSpecification room = new ReservationRequestModel.RoomSpecification();
        room.setParticipantCount(5);
        room.setPin("1234");
        reservationRequest.setRoom(room);
        model.addAttribute("reservationRequest", reservationRequest);
        return "reservationRequestModify";
    }

    @RequestMapping(value = "/modify/confirmed", method = {RequestMethod.POST, RequestMethod.GET})
    public String getModifyConfirmed(
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel,
            BindingResult result)
    {
        reservationRequestModel.validate(result);
        if (result.hasErrors()) {
            return "reservationRequestModify";
        }
        reservationRequestModel.setId("shongo:cz.cesnet:req:33");
        return "reservationRequestDetail";
    }
}
