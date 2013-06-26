package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.client.web.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.editors.LocalDateEditor;
import cz.cesnet.shongo.client.web.editors.PeriodEditor;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Controller for managing reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@RequestMapping("/reservation-request")
@SessionAttributes({"reservationRequest", "aclRecord"})
public class ReservationRequestUpdateController
{
    @Resource
    private ReservationService reservationService;

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor());
        binder.registerCustomEditor(LocalDate.class, new LocalDateEditor());
        binder.registerCustomEditor(Period.class, new PeriodEditor());
    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String getCreate(
            @RequestParam(value = "type", required = false) ReservationRequestModel.SpecificationType specificationType,
            Model model)
    {
        ReservationRequestModel reservationRequest = new ReservationRequestModel();
        reservationRequest.setSpecificationType(specificationType);
        reservationRequest.setStart(Temporal.roundDateTimeToMinutes(DateTime.now(), 5));
        reservationRequest.setPeriodicityType(ReservationRequestModel.PeriodicityType.NONE);
        model.addAttribute("reservationRequest", reservationRequest);
        return "reservationRequestCreate";
    }

    @RequestMapping(value = "/create/confirmed", method = {RequestMethod.POST, RequestMethod.GET})
    public String getCreateConfirmed(
            SecurityToken securityToken,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel,
            BindingResult result)
    {
        reservationRequestModel.validate(result);
        if (result.hasErrors()) {
            return "reservationRequestCreate";
        }
        AbstractReservationRequest reservationRequest = reservationRequestModel.toApi();
        String reservationRequestId = reservationService.createReservationRequest(securityToken, reservationRequest);
        return "redirect:/reservation-request/detail/" + reservationRequestId;
    }

    @RequestMapping(value = "/modify/{id:.+}", method = RequestMethod.GET)
    public String getModify(
            SecurityToken securityToken,
            @PathVariable(value = "id") String id,
            Model model)
    {
        AbstractReservationRequest reservationRequest = reservationService.getReservationRequest(securityToken, id);
        model.addAttribute("reservationRequest", new ReservationRequestModel(reservationRequest));
        return "reservationRequestModify";
    }

    @RequestMapping(value = "/modify/confirmed", method = {RequestMethod.POST, RequestMethod.GET})
    public String getModifyConfirmed(
            SecurityToken securityToken,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel,
            BindingResult result)
    {
        reservationRequestModel.validate(result);
        if (result.hasErrors()) {
            return "reservationRequestModify";
        }
        AbstractReservationRequest reservationRequest = reservationRequestModel.toApi();
        String reservationRequestId = reservationService.modifyReservationRequest(securityToken, reservationRequest);
        return "redirect:/reservation-request/detail/" + reservationRequestId;
    }
}
