package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.*;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.Breadcrumb;
import cz.cesnet.shongo.client.web.support.BreadcrumbProvider;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.client.web.support.NavigationPage;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for displaying detail of reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class ReservationRequestDetailController implements BreadcrumbProvider
{
    @Resource
    private ReservationService reservationService;

    @Resource
    private ExecutableService executableService;

    @Resource
    private Cache cache;

    @Resource
    private MessageSource messageSource;

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
        if (ClientWebNavigation.RESERVATION_REQUEST_DETAIL.isNavigationPage(navigationPage)) {
            breadcrumb = new Breadcrumb(navigationPage.getParentNavigationPage(), requestURI);
            return breadcrumb;
        }
        return new Breadcrumb(navigationPage, requestURI);
    }

    /**
     * Handle detail of reservation request view.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_DETAIL, method = RequestMethod.GET)
    public String handleDetailView(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String id,
            Model model)
    {
        Locale locale = userSession.getLocale();
        DateTimeZone timeZone = userSession.getTimeZone();
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        MessageProvider messageProvider = new MessageProvider(messageSource, locale, timeZone);

        // Get reservation request
        AbstractReservationRequest abstractReservationRequest =
                reservationService.getReservationRequest(securityToken, id);
        String reservationRequestId = abstractReservationRequest.getId();

        // Determine whether reservation request is child reservation request
        boolean isChildReservationRequest = false;
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            isChildReservationRequest = reservationRequest.getParentReservationRequestId() != null;
        }

        // Reservation request is active (e.g., it can be modified and deleted)
        boolean isActive = true;
        // Reservation request has visible allocated reservation
        boolean hasVisibleReservation = true;

        // Get history of reservation request (only if it is not child reservation request)
        if (!isChildReservationRequest && userSession.isAdvancedUserInterface()) {
            Map<String, Object> currentHistoryItem = null;
            List<Map<String, Object>> history = new LinkedList<Map<String, Object>>();
            for (ReservationRequestSummary historyItem :
                    reservationService.getReservationRequestHistory(securityToken, id)) {
                Map<String, Object> item = new HashMap<String, Object>();
                String historyItemId = historyItem.getId();
                item.put("id", historyItemId);
                item.put("dateTime", historyItem.getDateTime());
                UserInformation user = cache.getUserInformation(securityToken, historyItem.getUserId());
                item.put("user", user.getFullName());
                item.put("type", historyItem.getType());

                if (historyItemId.equals(reservationRequestId)) {
                    currentHistoryItem = item;
                    isActive = !historyItem.getType().equals(ReservationRequestType.DELETED);
                }

                AllocationState allocationState = historyItem.getAllocationState();
                if (allocationState != null) {
                    item.put("allocationState", allocationState);

                    // Reservation is visible only for reservation requests until first allocated reservation request
                    if (allocationState.equals(AllocationState.ALLOCATED) && currentHistoryItem == null) {
                        hasVisibleReservation = false;
                    }
                }

                ReservationRequestState state = ReservationRequestState.fromApi(historyItem);
                item.put("state", state);

                history.add(item);
            }
            if (currentHistoryItem == null) {
                throw new RuntimeException("Reservation request " + reservationRequestId + " should exist in history.");
            }
            currentHistoryItem.put("selected", true);

            model.addAttribute("history", history);
            if (isActive) {
                isActive = currentHistoryItem == history.get(0);
            }
        }
        else {
            // Child reservation requests don't have history
            // and thus they are automatically active and have visible reservation
        }

        // Get reservation
        Reservation reservation = null;
        if (hasVisibleReservation) {
            reservation = abstractReservationRequest.getLastReservation(reservationService, securityToken);
        }

        // Create reservation request model
        ReservationRequestDetailModel reservationRequestModel = new ReservationRequestDetailModel(
                abstractReservationRequest, reservation, cacheProvider,
                messageProvider, executableService, userSession);

        model.addAttribute("reservationRequest", reservationRequestModel);
        model.addAttribute("isActive", isActive);

        // Initialize breadcrumb
        if (breadcrumb != null) {
            breadcrumb.addItems(reservationRequestModel.getBreadcrumbItems(ClientWebUrl.RESERVATION_REQUEST_DETAIL));
        }

        return "reservationRequestDetail";
    }

    /**
     * Handle state for detail of reservation request view.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_DETAIL_STATE, method = RequestMethod.GET)
    @ResponseBody
    public Map handleDetailState(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {

        final Locale locale = userSession.getLocale();
        DateTimeZone timeZone = userSession.getTimeZone();

        AbstractReservationRequest abstractReservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        Reservation reservation = abstractReservationRequest.getLastReservation(reservationService, securityToken);

        final MessageProvider messageProvider = new MessageProvider(messageSource, locale, timeZone);
        final ReservationRequestDetailModel reservationRequestModel = new ReservationRequestDetailModel(
                abstractReservationRequest, reservation, new CacheProvider(cache, securityToken),
                messageProvider, executableService, userSession);
        final SpecificationType specificationType = reservationRequestModel.getSpecificationType();
        final RoomModel roomModel = reservationRequestModel.getRoom();

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("state", new HashMap<String, Object>(){{
            ReservationRequestState state = reservationRequestModel.getState();
            put("code", state);
            put("label", state.getMessage(messageSource, locale, specificationType));
            put("help", reservationRequestModel.getStateHelp());
        }});
        data.put("allocationState", new HashMap<String, Object>(){{
            AllocationState allocationState = reservationRequestModel.getAllocationState();
            put("code", allocationState);
            put("label", messageProvider.getMessage("views.reservationRequest.allocationState." + allocationState));
            put("help", reservationRequestModel.getAllocationStateHelp());
        }});
        if (roomModel != null) {
            DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.LONG, locale, timeZone);
            data.put("roomId", roomModel.getId());
            data.put("roomSlot", formatter.formatIntervalMultiLine(roomModel.getSlot()));
            data.put("roomName", roomModel.getName());
            data.put("roomLicenseCount", roomModel.getLicenseCount());
            data.put("roomAliases", roomModel.getAliases());
            data.put("roomAliasesDescription", roomModel.getAliasesDescription());
            data.put("roomState", new HashMap<String, Object>(){{
                RoomState roomState = roomModel.getState();
                put("code", roomState);
                put("started", roomState.isStarted());
                put("report", roomModel.getStateReport());
                put("label", roomState.getMessage(messageSource, locale, roomModel.getType()));
                put("help", roomState.getHelp(messageSource, locale, roomModel.getType()));
            }});
        }
        return data;
    }

    /**
     * Handle data request for children of reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_DETAIL_CHILDREN, method = RequestMethod.GET)
    @ResponseBody
    public Map handleDetailChildren(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false, defaultValue = "SLOT") ReservationRequestListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending)
    {
        // List reservation requests
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setParentReservationRequestId(reservationRequestId);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);

        ReservationListRequest reservationListRequest = new ReservationListRequest();
        reservationListRequest.setSecurityToken(securityToken);
        for (ReservationRequestSummary reservationRequest : response.getItems()) {
            String lastReservationId = reservationRequest.getLastReservationId();
            if (lastReservationId != null) {
                reservationListRequest.addReservationId(lastReservationId);
            }
        }
        Map<String, Reservation> reservationById = new HashMap<String, Reservation>();
        if (reservationListRequest.getReservationIds().size() > 0) {
            ListResponse<Reservation> reservations = reservationService.listReservations(reservationListRequest);
            for (Reservation reservation : reservations) {
                reservationById.put(reservation.getId(), reservation);
            }
        }

        // Build response
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, locale, timeZone);
        List<Map<String, Object>> children = new LinkedList<Map<String, Object>>();
        for (ReservationRequestSummary reservationRequest : response.getItems()) {
            Map<String, Object> child = new HashMap<String, Object>();
            child.put("id", reservationRequest.getId());

            Interval slot = reservationRequest.getEarliestSlot();
            child.put("slot", formatter.formatInterval(slot));

            ReservationRequestState state = ReservationRequestState.fromApi(reservationRequest);
            if (state != null) {
                SpecificationType specificationType =
                        SpecificationType.fromReservationRequestSummary(reservationRequest);
                String lastReservationId = reservationRequest.getLastReservationId();
                child.put("state", state);
                child.put("stateMessage", state.getMessage(messageSource, locale, specificationType));
                child.put("stateHelp", state.getHelp(messageSource, locale, specificationType, lastReservationId));
            }

            String reservationId = reservationRequest.getLastReservationId();
            Reservation reservation = reservationById.get(reservationId);
            if (reservation != null) {
                // Reservation should contain allocated room
                AbstractRoomExecutable room = (AbstractRoomExecutable) reservation.getExecutable();
                if (room != null) {
                    child.put("roomId", room.getId());

                    // Set room state available
                    ExecutableState roomState = room.getState();
                    child.put("roomStateAvailable", roomState.isAvailable());

                    // Set room aliases
                    List<Alias> aliases = room.getAliases();
                    child.put("roomAliases", RoomModel.formatAliases(aliases, roomState.isAvailable()));
                    child.put("roomAliasesDescription", RoomModel.formatAliasesDescription(
                            aliases, roomState.isAvailable(), new MessageProvider(messageSource, locale)));
                }
            }

            children.add(child);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("sort", sort);
        data.put("sort-desc", sortDescending);
        data.put("items", children);
        return data;
    }

    /**
     * Handle data request for usages of reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_DETAIL_USAGES, method = RequestMethod.GET)
    @ResponseBody
    public Map handleDetailUsages(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false, defaultValue = "SLOT") ReservationRequestListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending)
    {
        // List reservation requests
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        request.setReusedReservationRequestId(reservationRequestId);
        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);

        // Build response
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, locale, timeZone);
        List<Map<String, Object>> usages = new LinkedList<Map<String, Object>>();
        for (ReservationRequestSummary reservationRequest : response.getItems()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", reservationRequest.getId());
            item.put("description", reservationRequest.getDescription());
            item.put("purpose", reservationRequest.getPurpose());
            usages.add(item);

            ReservationRequestState state = ReservationRequestState.fromApi(reservationRequest);
            if (state != null) {
                SpecificationType specificationType =
                        SpecificationType.fromReservationRequestSummary(reservationRequest);
                String lastReservationId = reservationRequest.getLastReservationId();
                item.put("state", state);
                item.put("stateMessage", state.getMessage(messageSource, locale, specificationType));
                item.put("stateHelp", state.getHelp(messageSource, locale, specificationType, lastReservationId));
            }

            UserInformation user = cache.getUserInformation(securityToken, reservationRequest.getUserId());
            item.put("user", user.getFullName());

            Interval earliestSlot = reservationRequest.getEarliestSlot();
            if (earliestSlot != null) {
                item.put("slot", formatter.formatInterval(earliestSlot));
            }
            Integer futureSlotCount = reservationRequest.getFutureSlotCount();
            if (futureSlotCount != null) {
                item.put("futureSlotCount", futureSlotCount);
            }

            ReservationRequestSummary.RoomSpecification roomSpecification =
                    (ReservationRequestSummary.RoomSpecification) reservationRequest.getSpecification();
            if (roomSpecification != null) {
                item.put("roomParticipantCount", roomSpecification.getParticipantCount());
            }
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("sort", sort);
        data.put("sort-desc", sortDescending);
        data.put("items", usages);
        return data;
    }

    /**
     * Handle detail of reservation request view.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_DETAIL_REVERT, method = RequestMethod.GET)
    public String handleDetailRevert(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        // Get reservation request
        reservationRequestId = reservationService.revertReservationRequest(securityToken, reservationRequestId);
        return "redirect:" + ClientWebUrl.getReservationRequestDetail(reservationRequestId);
    }
}
