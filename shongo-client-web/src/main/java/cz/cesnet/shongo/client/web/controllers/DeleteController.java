package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebNavigation;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.SpecificationType;
import cz.cesnet.shongo.client.web.support.*;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Controller for reverting and deleting reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class DeleteController implements BreadcrumbProvider
{
    @Resource
    private ReservationService reservationService;

    @Resource
    private Cache cache;

    /**
     * {@link cz.cesnet.shongo.client.web.support.Breadcrumb} for the {@link #handleDeleteView}
     */
    private Breadcrumb breadcrumb;

    @Override
    public Breadcrumb createBreadcrumb(NavigationPage navigationPage, String requestURI)
    {
        if (navigationPage == null) {
            return null;
        }
        if (ClientWebNavigation.RESERVATION_REQUEST_DELETE.isNavigationPage(navigationPage)) {
            breadcrumb = new Breadcrumb(navigationPage, requestURI);
            return breadcrumb;
        }
        return new Breadcrumb(navigationPage, requestURI);
    }

    /**
     * Handle revert of reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_REVERT, method = RequestMethod.GET)
    public String handleRevert(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        // Get reservation request
        reservationRequestId = reservationService.revertReservationRequest(securityToken, reservationRequestId);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, reservationRequestId);
    }

    /**
     * Handle deletion of reservation request view.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_DELETE, method = RequestMethod.GET)
    public String handleDeleteView(
            SecurityToken securityToken,
            MessageProvider messageProvider,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            Model model)
    {
        ReservationRequestSummary reservationRequest =
                cache.getReservationRequestSummary(securityToken, reservationRequestId);
        SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);
        String roomName = reservationRequest.getRoomName();
        String title = messageProvider.getMessage("views.reservationRequestDelete.title",
                messageProvider.getMessage("views.specificationType.forWithName." + specificationType,
                        roomName != null ? roomName : ""));
        List<ReservationRequestSummary> dependencies =
                ReservationRequestModel.getDeleteDependencies(reservationRequestId, reservationService, securityToken);

        model.addAttribute("titleDescription", title);
        model.addAttribute("specificationType", specificationType);
        model.addAttribute("reservationRequest", reservationRequest);
        model.addAttribute("dependencies", dependencies);

        // Initialize breadcrumb
        if (breadcrumb != null) {
            breadcrumb.addPages(breadcrumb.getPagesCount() - 1,
                    ReservationRequestModel.getPagesForBreadcrumb(reservationRequest));
        }

        return "reservationRequestDelete";
    }

    /**
     * Handle confirmation for deletion of reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_DELETE, method = RequestMethod.POST)
    public String handleDeleteConfirm(
            HttpServletRequest request,
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
        return "redirect:" + ClientWebUrl.HOME;
    }
}
