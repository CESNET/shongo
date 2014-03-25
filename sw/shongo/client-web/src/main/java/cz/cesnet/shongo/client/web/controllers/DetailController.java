package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.client.web.*;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.Breadcrumb;
import cz.cesnet.shongo.client.web.support.BreadcrumbProvider;
import cz.cesnet.shongo.client.web.support.NavigationPage;
import cz.cesnet.shongo.controller.api.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

/**
 * Controller for displaying detail.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class DetailController extends AbstractDetailController implements BreadcrumbProvider
{
    /**
     * {@link cz.cesnet.shongo.client.web.support.Breadcrumb} for the {@link #handleDetailView}
     */
    private Breadcrumb breadcrumb;

    @Override
    public Breadcrumb createBreadcrumb(NavigationPage navigationPage, String requestURI)
    {
        if (navigationPage == null) {
            return null;
        }
        if (ClientWebNavigation.DETAIL.isNavigationPage(navigationPage)) {
            breadcrumb = new Breadcrumb(navigationPage.getParentNavigationPage(), requestURI);
            return breadcrumb;
        }
        return new Breadcrumb(navigationPage, requestURI);
    }

    /**
     * Handle detail view.
     */
    @RequestMapping(value = ClientWebUrl.DETAIL_VIEW, method = RequestMethod.GET)
    public ModelAndView handleDetailView(
            SecurityToken securityToken,
            Locale locale,
            @PathVariable(value = "objectId") String objectId,
            @RequestParam(value = "tab", required = false) String tab)
    {
        String reservationRequestId = getReservationRequestId(securityToken, objectId);
        ReservationRequestSummary reservationRequest =
                cache.getReservationRequestSummaryNotCached(securityToken, reservationRequestId);
        String parentReservationRequestId = reservationRequest.getParentReservationRequestId();
        RoomState roomState = null;
        String roomName = reservationRequest.getRoomName();
        String reservationId = reservationRequest.getAllocatedReservationId();
        if (reservationId != null) {
            // We don't want to show allocated room name
            // AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, reservationId);
            // Alias roomNameAlias = roomExecutable.getAliasByType(AliasType.ROOM_NAME);
            // if (roomNameAlias != null) {
            //     roomName = roomNameAlias.getValue();
            // }
            roomState = RoomState.fromRoomState(reservationRequest.getExecutableState(),
                    reservationRequest.getRoomParticipantCount(), reservationRequest.getUsageExecutableState());
        }
        SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);
        String titleDescription = messageSource.getMessage(
                "views.detail.title." + specificationType, new Object[]{roomName != null ? roomName : ""}, locale);

        ModelAndView modelAndView = new ModelAndView("detail");
        modelAndView.addObject("titleDescription", titleDescription);
        modelAndView.addObject("tab", tab);
        modelAndView.addObject("specificationType", specificationType);
        modelAndView.addObject("allocationState", reservationRequest.getAllocationState());
        modelAndView.addObject("roomState", roomState);
        modelAndView.addObject("isPeriodic", reservationRequest.getFutureSlotCount() != null);
        modelAndView.addObject("isPeriodicEvent", parentReservationRequestId != null);
        modelAndView.addObject("isRoomRecordable", reservationRequest.isRoomRecordable());

        // Initialize breadcrumb
        if (breadcrumb != null) {
            breadcrumb.addItems(ReservationRequestModel.getBreadcrumbItems(
                    reservationRequestId, specificationType, parentReservationRequestId,
                    reservationRequest.getReusedReservationRequestId()));
        }

        return modelAndView;
    }
}
