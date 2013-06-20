package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.client.web.UserCache;
import cz.cesnet.shongo.client.web.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.editors.LocalDateEditor;
import cz.cesnet.shongo.client.web.editors.PeriodEditor;
import cz.cesnet.shongo.client.web.models.AclRecordValidator;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.AclRecord;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.AclRecordListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.*;

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

    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private UserCache userCache;

    @Resource
    private MessageSource messageSource;

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

    @RequestMapping(value = "/create/confirmed", method = {RequestMethod.POST})
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

    @RequestMapping(value = "/modify/confirmed", method = {RequestMethod.POST})
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

    @RequestMapping(value = "/{reservationRequestId:.+}/acl", method = RequestMethod.GET)
    @ResponseBody
    public Map getAcl(
            Locale locale,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count)
    {
        // List reservation requests
        AclRecordListRequest request = new AclRecordListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.addEntityId(reservationRequestId);
        ListResponse<AclRecord> response = authorizationService.listAclRecords(request);

        // Build response
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (AclRecord aclRecord : response.getItems()) {

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", aclRecord.getId());
            item.put("user", userCache.getUserInformation(securityToken, aclRecord.getUserId()));
            String role = aclRecord.getRole().toString();
            item.put("role", messageSource.getMessage("views.aclRecord.role." + role, null, locale));
            items.add(item);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("items", items);
        return data;
    }

    private ModelAndView getAclCreate(String reservationRequestId, AclRecord aclRecord)
    {
        ModelAndView modelAndView = new ModelAndView("aclRecordCreate");
        modelAndView.addObject("aclRecord", aclRecord);
        modelAndView.addObject("entity", "views.reservationRequest");
        modelAndView.addObject("roles", EntityType.RESERVATION_REQUEST.getOrderedRoles());
        modelAndView.addObject("confirm", "views.button.create");
        modelAndView.addObject("confirmUrl", "/reservation-request/" + reservationRequestId + "/acl/create/confirmed");
        modelAndView.addObject("backUrl", "/reservation-request/detail/" + reservationRequestId);
        return modelAndView;
    }

    @RequestMapping(value = "/{reservationRequestId:.+}/acl/create", method = RequestMethod.GET)
    public ModelAndView getAclCreate(
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        AclRecord aclRecord = new AclRecord();
        aclRecord.setEntityId(reservationRequestId);
        return getAclCreate(reservationRequestId, aclRecord);
    }

    @RequestMapping(value = "/{reservationRequestId:.+}/acl/create/confirmed", method = RequestMethod.POST)
    public Object getAclCreateConfirmed(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @ModelAttribute("aclRecord") AclRecord aclRecord,
            BindingResult result)
    {
        AclRecordValidator aclRecordValidator = new AclRecordValidator();
        aclRecordValidator.validate(aclRecord, result);
        if (result.hasErrors()) {
            return getAclCreate(reservationRequestId, aclRecord);
        }
        authorizationService.createAclRecord(securityToken,
                aclRecord.getUserId(), aclRecord.getEntityId(), aclRecord.getRole());
        return "redirect:/reservation-request/detail/" + reservationRequestId;
    }

    @RequestMapping(value = "/{reservationRequestId:.+}/acl/delete/{aclRecordId}", method = RequestMethod.GET)
    public String getAclDelete(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @PathVariable(value = "aclRecordId") String aclRecordId,
            Model model)
    {
        AclRecordListRequest request = new AclRecordListRequest();
        request.setSecurityToken(securityToken);
        request.addEntityId(reservationRequestId);
        request.addRole(Role.OWNER);
        ListResponse<AclRecord> aclRecords = authorizationService.listAclRecords(request);
        if (aclRecords.getItemCount() == 1 && aclRecords.getItem(0).getId().equals(aclRecordId)) {
            model.addAttribute("title", "views.reservationRequestDetail.userRoles.cannotDeleteLastOwner.title");
            model.addAttribute("message", "views.reservationRequestDetail.userRoles.cannotDeleteLastOwner.message");
            model.addAttribute("backUrl", "/reservation-request/detail/" + reservationRequestId);
            return "message";
        }
        authorizationService.deleteAclRecord(securityToken, aclRecordId);
        return "redirect:/reservation-request/detail/" + reservationRequestId;
    }
}
