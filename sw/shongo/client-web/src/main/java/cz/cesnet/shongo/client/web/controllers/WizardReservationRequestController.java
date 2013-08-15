package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.*;
import cz.cesnet.shongo.client.web.models.ReservationRequestDetailModel;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.UserRoleModel;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AclRecordListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Controller for displaying wizard interface.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class WizardReservationRequestController extends AbstractWizardController
{
    @Resource
    private ReservationService reservationService;

    @Resource
    private ExecutableService executableService;

    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private Cache cache;

    @Resource
    private MessageSource messageSource;

    private static enum Page
    {
        RESERVATION_REQUEST,
        RESERVATION_REQUEST_DETAIL,
        RESERVATION_REQUEST_DELETE
    }

    @Override
    protected void initWizardPages(WizardView wizardView, Object currentWizardPageId)
    {
        wizardView.addPage(WizardController.createSelectWizardPage());
        wizardView.addPage(new WizardPage(Page.RESERVATION_REQUEST, ClientWebUrl.WIZARD_RESERVATION_REQUEST_LIST,
                "views.wizard.page.reservationRequestList"));

        if (Page.RESERVATION_REQUEST_DELETE.equals(currentWizardPageId)) {
            wizardView.addPage(new WizardPage(Page.RESERVATION_REQUEST_DELETE,
                    ClientWebUrl.WIZARD_RESERVATION_REQUEST_DETAIL, "views.wizard.page.reservationRequestDelete"));
        }
        else {
            wizardView.addPage(new WizardPage(Page.RESERVATION_REQUEST_DETAIL,
                    ClientWebUrl.WIZARD_RESERVATION_REQUEST_DETAIL, "views.wizard.page.reservationRequestDetail"));
        }
    }

    /**
     * Display list of reservation requests.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_RESERVATION_REQUEST_LIST, method = RequestMethod.GET)
    public ModelAndView handleReservationRequestList()
    {
        WizardView wizardView = getWizardView(Page.RESERVATION_REQUEST, "wizardReservationRequestList.jsp");
        wizardView.setNextPage(null);
        return wizardView;
    }

    /**
     * Display detail of reservation request with given {@code reservationRequestId}.
     *
     * @param reservationRequestId
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_RESERVATION_REQUEST_DETAIL, method = RequestMethod.GET)
    public ModelAndView handleReservationRequestDetail(
            Locale locale,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        MessageProvider messageProvider = new MessageProvider(messageSource, locale);

        // Get reservation request
        AbstractReservationRequest abstractReservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);

        // Get reservation
        Reservation reservation = null;
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            String reservationId = reservationRequest.getLastReservationId();
            if (reservationId != null) {
                reservation = reservationService.getReservation(cacheProvider.getSecurityToken(), reservationId);
            }
        }

        // Create reservation request model
        ReservationRequestDetailModel reservationRequest = new ReservationRequestDetailModel(
                abstractReservationRequest, reservation, cacheProvider, messageProvider, executableService);

        // Get user roles
        AclRecordListRequest request = new AclRecordListRequest();
        request.setSecurityToken(securityToken);
        request.addEntityId(reservationRequestId);
        ListResponse<AclRecord> response = authorizationService.listAclRecords(request);
        List<UserRoleModel> userRoles = new LinkedList<UserRoleModel>();
        for (AclRecord aclRecord : response) {
            userRoles.add(new UserRoleModel(aclRecord, cacheProvider));
        }

        WizardView wizardView = getWizardView(Page.RESERVATION_REQUEST_DETAIL, "wizardReservationRequestDetail.jsp");
        wizardView.addObject("reservationRequest", reservationRequest);
        wizardView.addObject("userRoles", userRoles);
        wizardView.getCurrentPage().setTitleDescription(reservationRequest.getDescription());

        return wizardView;
    }

    /**
     * Handle deletion of reservation request view.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_RESERVATION_REQUEST_DELETE, method = RequestMethod.GET)
    public ModelAndView handleDeleteView(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        WizardView wizardView = getWizardView(Page.RESERVATION_REQUEST_DELETE, "wizardReservationRequestDelete.jsp");
        AbstractReservationRequest reservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        List<ReservationRequestSummary> dependencies =
                ReservationRequestModel.getDeleteDependencies(reservationRequestId, reservationService, securityToken);
        wizardView.addObject("reservationRequest", reservationRequest);
        wizardView.addObject("dependencies", dependencies);
        if (dependencies.size() == 0) {
            wizardView.setPreviousPage(null);
            wizardView.addAction(ClientWebUrl.getWizardReservationRequestDeleteConfirm(reservationRequestId),
                    "views.button.yes").setPrimary(true);
            wizardView.addAction(ClientWebUrl.WIZARD_RESERVATION_REQUEST_LIST, "views.button.no");
        }
        else {
            wizardView.addAction(ClientWebUrl.getWizardReservationRequestDeleteConfirm(reservationRequestId)
                    + "?dependencies=true", "deleteAll");
        }
        wizardView.getCurrentPage().setTitleDescription(reservationRequest.getDescription());

        return wizardView;
    }

    /**
     * Handle confirmation for deletion of reservation request.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_RESERVATION_REQUEST_DELETE_CONFIRM, method = RequestMethod.GET)
    public String handleDeleteConfirm(
            SecurityToken securityToken,
            @RequestParam(value = "dependencies", required = false, defaultValue = "false") boolean dependencies,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        if (dependencies) {
            List<ReservationRequestSummary> reservationRequestDependencies =
                    ReservationRequestModel.getDeleteDependencies(
                            reservationRequestId, reservationService, securityToken);
            for (ReservationRequestSummary reservationRequestSummary : reservationRequestDependencies) {
                reservationService.deleteReservationRequest(securityToken, reservationRequestSummary.getId());
            }
        }
        reservationService.deleteReservationRequest(securityToken, reservationRequestId);
        return "redirect:" + ClientWebUrl.WIZARD_RESERVATION_REQUEST_LIST;
    }
}
