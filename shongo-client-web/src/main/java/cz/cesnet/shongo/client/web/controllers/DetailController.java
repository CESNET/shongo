package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.*;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.Breadcrumb;
import cz.cesnet.shongo.client.web.support.BreadcrumbProvider;
import cz.cesnet.shongo.client.web.support.NavigationPage;
import cz.cesnet.shongo.client.web.support.interceptors.NavigationInterceptor;
import cz.cesnet.shongo.controller.api.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Controller for displaying detail.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class DetailController extends AbstractDetailController implements BreadcrumbProvider
{
    @Override
    public Breadcrumb createBreadcrumb(NavigationPage navigationPage, HttpServletRequest request)
    {
        if (navigationPage == null) {
            return null;
        }
        String requestURI = request.getRequestURI();
        if (ClientWebNavigation.DETAIL.isNavigationPage(navigationPage)) {
            return new Breadcrumb(navigationPage.getParentNavigationPage(), requestURI);
        }
        return new Breadcrumb(navigationPage, requestURI);
    }

    /**
     * Handle detail view.
     */
    @RequestMapping(value = ClientWebUrl.DETAIL_VIEW, method = RequestMethod.GET)
    public ModelAndView handleDetailView(
            HttpServletRequest request,
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
        //TODO:MR
        if (reservationId != null && reservationRequest.getExecutableState() != null) {
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
        modelAndView.addObject("objectId", reservationRequest.getId());
        modelAndView.addObject("isActive", !reservationRequest.getType().equals(ReservationRequestType.DELETED));
        modelAndView.addObject("titleDescription", titleDescription);
        modelAndView.addObject("tab", tab);
        modelAndView.addObject("specificationType", specificationType);
        modelAndView.addObject("technology",
                TechnologyModel.find(reservationRequest.getSpecificationTechnologies()));
        modelAndView.addObject("allocationState", reservationRequest.getAllocationState());
        modelAndView.addObject("reservationId", reservationId);
        modelAndView.addObject("roomState", roomState);
        modelAndView.addObject("isPeriodic", reservationRequest.getFutureSlotCount() != null);
        modelAndView.addObject("isPeriodicEvent", parentReservationRequestId != null);
        modelAndView.addObject("roomHasRecordingService", reservationRequest.hasRoomRecordingService());
        modelAndView.addObject("roomHasRecordings", reservationRequest.hasRoomRecordings());
        if (SpecificationType.PERMANENT_ROOM_CAPACITY.equals(specificationType)) {
            String permanentRoomId = reservationRequest.getReusedReservationRequestId();
            if (permanentRoomId != null) {
                ReservationRequestSummary permanentRoom =
                        cache.getReservationRequestSummary(securityToken, permanentRoomId);
                if (permanentRoom != null && AllocationState.ALLOCATED.equals(permanentRoom.getAllocationState())) {
                    modelAndView.addObject("permanentRoomId", permanentRoom.getId());
                    modelAndView.addObject("permanentRoomHasRecordings", permanentRoom.hasRoomRecordings());
                }
            }
        }

        // Initialize breadcrumb
        Breadcrumb breadcrumb = (Breadcrumb) request.getAttribute(NavigationInterceptor.BREADCRUMB_REQUEST_ATTRIBUTE);
        if (breadcrumb != null) {
            breadcrumb.addPages(ReservationRequestModel.getPagesForBreadcrumb(
                    reservationRequestId, specificationType, parentReservationRequestId,
                    reservationRequest.getReusedReservationRequestId()));
        }

        return modelAndView;
    }
}
