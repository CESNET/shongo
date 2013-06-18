package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.client.web.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.editors.LocalDateEditor;
import cz.cesnet.shongo.client.web.editors.PeriodEditor;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
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
        binder.registerCustomEditor(LocalDate.class, new LocalDateEditor());
        binder.registerCustomEditor(Period.class, new PeriodEditor());
    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String getCreate(
            @RequestParam(value = "type", required = false) ReservationRequestModel.Type type,
            Model model)
    {
        ReservationRequestModel reservationRequest = new ReservationRequestModel();
        reservationRequest.setType(type);
        reservationRequest.setStart(Temporal.roundDateTimeToMinutes(DateTime.now(), 5));
        reservationRequest.setPeriodicityType(ReservationRequestModel.PeriodicityType.NONE);
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
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        reservationRequest.setTechnology(ReservationRequestModel.Technology.H323_SIP);
        reservationRequest.setStart(DateTime.parse("2012-06-01T12:00"));
        reservationRequest.setEnd(DateTime.parse("2012-06-01T14:00"));
        reservationRequest.setDurationCount(2);
        reservationRequest.setDurationType(ReservationRequestModel.DurationType.HOUR);
        reservationRequest.setType(ReservationRequestModel.Type.ROOM);
        reservationRequest.setRoomParticipantCount(5);
        reservationRequest.setRoomPin("1234");
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
