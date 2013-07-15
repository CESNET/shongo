package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebNavigation;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.Page;
import cz.cesnet.shongo.client.web.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.editors.LocalDateEditor;
import cz.cesnet.shongo.client.web.editors.PeriodEditor;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.ReservationRequestValidator;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Controller for displaying wizard interface.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({"availablePages", "reservationRequest", "permanentRooms"})
public class WizardController
{
    @Resource
    private HttpSession httpSession;

    @Resource
    private ReservationService reservationService;

    @Resource
    private Cache cache;

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor());
        binder.registerCustomEditor(LocalDate.class, new LocalDateEditor());
        binder.registerCustomEditor(Period.class, new PeriodEditor());
    }

    @RequestMapping(value = ClientWebUrl.WIZARD, method = RequestMethod.GET)
    public String handleDefault()
    {
        return "forward:" + ClientWebUrl.WIZARD_SELECT;
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_SELECT, method = RequestMethod.GET)
    public ModelAndView handleSelect(SessionStatus sessionStatus)
    {
        sessionStatus.setComplete();
        return handleWizardView(ClientWebNavigation.WIZARD_SELECT);
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_RESERVATION_REQUEST_LIST, method = RequestMethod.GET)
    public ModelAndView handleReservationRequestList()
    {
        return handleWizardView(ClientWebNavigation.WIZARD_RESERVATION_REQUEST);
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_RESERVATION_REQUEST_DETAIL, method = RequestMethod.GET)
    public ModelAndView handleReservationRequestDetail(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        return handleWizardView(ClientWebNavigation.WIZARD_RESERVATION_REQUEST_DETAIL);
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM, method = RequestMethod.GET)
    public ModelAndView handleCreateRoom()
    {
        ModelAndView modelAndView = handleWizardView(ClientWebNavigation.WIZARD_CREATE_ROOM);
        ReservationRequestModel reservationRequestModel =
                (ReservationRequestModel) httpSession.getAttribute("reservationRequest");
        if (reservationRequestModel == null) {
            reservationRequestModel = new ReservationRequestModel();
            modelAndView.addObject("reservationRequest", reservationRequestModel);
        }
        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ADHOC_ROOM, method = RequestMethod.GET)
    public String handleCreateAdhocRoom(
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel)
    {
        reservationRequestModel.setSpecificationType(ReservationRequestModel.SpecificationType.ADHOC_ROOM);
        return "forward:" + ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES;
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM, method = RequestMethod.GET)
    public String handleCreatePermanentRoom(
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel)
    {
        reservationRequestModel.setSpecificationType(ReservationRequestModel.SpecificationType.PERMANENT_ROOM);
        return "forward:" + ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES;
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES, method = RequestMethod.GET)
    public ModelAndView handleCreateRoomAttributes(
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel)
    {
        return handleWizardView(ClientWebNavigation.WIZARD_CREATE_ROOM_ATTRIBUTES);
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_USER_ROLES, method = RequestMethod.GET)
    public ModelAndView handleCreateRoomRoles(
            @ModelAttribute("reservationRequestRoles") ReservationRequestModel reservationRequestModel)
    {
        return handleWizardView(ClientWebNavigation.WIZARD_CREATE_ROOM_ROLES);
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY, method = RequestMethod.GET)
    public ModelAndView handleCreatePermanentRoomCapacity(
            SecurityToken securityToken)
    {
        ModelAndView modelAndView = handleWizardView(ClientWebNavigation.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY);
        ReservationRequestModel reservationRequestModel = new ReservationRequestModel();
        reservationRequestModel.setSpecificationType(ReservationRequestModel.SpecificationType.PERMANENT_ROOM_CAPACITY);
        modelAndView.addObject("reservationRequest", reservationRequestModel);
        modelAndView.addObject("permanentRooms",
                ReservationRequestModel.getPermanentRooms(reservationService, securityToken, cache));
        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_CONFIRM, method = RequestMethod.POST)
    public ModelAndView handleCreateConfirm(
            SecurityToken token,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel,
            BindingResult result)
    {
        if (ReservationRequestValidator.validate(reservationRequestModel, result, token, reservationService)) {
            switch (reservationRequestModel.getSpecificationType()) {
                case ADHOC_ROOM:
                case PERMANENT_ROOM:
                    return handleWizardView(ClientWebNavigation.WIZARD_CREATE_ROOM_ROLES);
                case PERMANENT_ROOM_CAPACITY:
                    return handleWizardView(ClientWebNavigation.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM);
                default:
                    throw new TodoImplementException(reservationRequestModel.getSpecificationType().toString());
            }
        }
        else {
            switch (reservationRequestModel.getSpecificationType()) {
                case ADHOC_ROOM:
                case PERMANENT_ROOM:
                    return handleWizardView(ClientWebNavigation.WIZARD_CREATE_ROOM_ATTRIBUTES);
                case PERMANENT_ROOM_CAPACITY:
                    return handleWizardView(ClientWebNavigation.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY);
                default:
                    throw new TodoImplementException(reservationRequestModel.getSpecificationType().toString());
            }
        }
    }

    private ModelAndView handleWizardView(ClientWebNavigation currentNavigation)
    {
        @SuppressWarnings("unchecked")
        Set<Page> availablePages = (Set) httpSession.getAttribute("availablePages");
        if (availablePages == null) {
            availablePages = new HashSet<Page>();
        }

        Page currentPage = currentNavigation.getPage();

        // Get
        List<Page> wizardPages = new LinkedList<Page>();
        wizardPages.add(currentPage);
        availablePages.add(currentPage);
        Page page = currentPage.getParentPage();
        while (page != null) {
            wizardPages.add(0, page);
            availablePages.add(page);
            page = page.getParentPage();
        }
        page = currentPage;
        while (page != null) {
            List<Page> childPages = page.getChildPages();
            int childPageCount = childPages.size();
            if (childPageCount == 0) {
                break;
            }
            else if (childPageCount == 1) {
                page = childPages.get(0);
                wizardPages.add(page);
            }
            else {
                wizardPages.add(new Page(null, "..."));
                break;
            }
        }
        ModelAndView modelAndView = new ModelAndView("wizard");
        modelAndView.addObject("navigation", currentNavigation);
        modelAndView.addObject("wizardPages", wizardPages);
        modelAndView.addObject("availablePages", availablePages);
        return modelAndView;
    }
}
