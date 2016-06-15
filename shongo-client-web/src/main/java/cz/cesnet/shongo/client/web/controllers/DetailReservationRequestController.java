package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.client.web.support.MessageProviderImpl;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for displaying detail of reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class DetailReservationRequestController extends AbstractDetailController
{
    @Resource
    private AuthorizationService authorizationService;
    
    /**
     * Handle detail reservation request tab.
     */
    @RequestMapping(value = ClientWebUrl.DETAIL_RESERVATION_REQUEST_TAB, method = RequestMethod.GET)
    public ModelAndView handleDetailReservationRequestTab(
            SecurityToken securityToken,
            UserSession userSession,
            @PathVariable(value = "objectId") String objectId)
    {
        String reservationRequestId = getReservationRequestId(securityToken, objectId);

        ModelAndView modelAndView = new ModelAndView("detailReservationRequest");

        // Get reservation request
        AbstractReservationRequest abstractReservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        reservationRequestId = abstractReservationRequest.getId();

        // Determine whether reservation request is child reservation request
        boolean isChildReservationRequest = false;
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            isChildReservationRequest = reservationRequest.getParentReservationRequestId() != null;
        }

        // Specifies whether reservation request is last version which can be modified
        boolean isActive = true;
        // Specifies whether reservation should be visible for the reservation request
        boolean isLatestAllocated = true;

        // Get history of reservation request (only if it is not child reservation request)
        if (!isChildReservationRequest) {
            Map<String, Object> currentHistoryItem = null;
            List<Map<String, Object>> history = new LinkedList<Map<String, Object>>();
            boolean historyItemLatestAllocated = true;
            for (ReservationRequestSummary historyItem :
                    reservationService.getReservationRequestHistory(securityToken, reservationRequestId)) {
                String historyItemId = historyItem.getId();
                ReservationRequestType historyItemType = historyItem.getType();
                AllocationState historyItemAllocationState = historyItem.getAllocationState();
                ReservationRequestState historyItemState = ReservationRequestState.fromApi(historyItem);

                Map<String, Object> item = new HashMap<String, Object>();
                item.put("id", historyItemId);
                item.put("dateTime", historyItem.getDateTime());
                UserInformation user = cache.getUserInformation(securityToken, historyItem.getUserId());
                item.put("user", user.getFullName());
                item.put("type", historyItemType);
                item.put("allocationState", historyItemAllocationState);
                item.put("state", historyItemState);
                item.put("isActive", (history.size() == 0 && !historyItemType.equals(ReservationRequestType.DELETED)));
                item.put("isLatestAllocated", historyItemLatestAllocated);
                history.add(item);

                if (AllocationState.ALLOCATED.equals(historyItemAllocationState)) {
                    // Latest allocated reservation request is only until first allocated
                    historyItemLatestAllocated = false;
                }
                if (historyItemId.equals(reservationRequestId)) {
                    currentHistoryItem = item;
                }
            }
            if (currentHistoryItem == null) {
                throw new RuntimeException("Reservation request " + reservationRequestId + " should exist in history.");
            }
            modelAndView.addObject("history", history);
            isActive = (Boolean) currentHistoryItem.get("isActive");
            isLatestAllocated = (Boolean) currentHistoryItem.get("isLatestAllocated");
        }

        modelAndView.addObject("isActive", isActive);
        modelAndView.addObject("isLatestAllocated", isLatestAllocated);
        modelAndView.addObject("reservationRequest", getReservationRequestState(
                securityToken, userSession, abstractReservationRequest, isLatestAllocated));

        return modelAndView;
    }

    /**
      * Handle state for detail of reservation request view.
      */
    @RequestMapping(value = ClientWebUrl.DETAIL_RESERVATION_REQUEST_STATE, method = RequestMethod.GET)
    public ModelAndView handleDetailState(
            SecurityToken securityToken,
                UserSession userSession,
                @PathVariable(value = "objectId") String objectId,
                @PathVariable(value = "isLatestAllocated") boolean isLatestAllocated)
    {
        String reservationRequestId = getReservationRequestId(securityToken, objectId);
        AbstractReservationRequest abstractReservationRequest =
            reservationService.getReservationRequest(securityToken, reservationRequestId);

        ModelAndView modelAndView = new ModelAndView("detailReservationRequestState");
        modelAndView.addObject("reservationRequest", getReservationRequestState(
                securityToken, userSession, abstractReservationRequest, isLatestAllocated));
        return modelAndView;
    }

    /**
     * Handle data request for children of reservation request.
     */
    @RequestMapping(value = ClientWebUrl.DETAIL_RESERVATION_REQUEST_CHILDREN, method = RequestMethod.GET)
    @ResponseBody
    public Map handleDetailChildren(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false, defaultValue = "SLOT") ReservationRequestListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending)
    {
        String reservationRequestId = getReservationRequestId(securityToken, objectId);

        // List reservation requests
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setParentReservationRequestId(reservationRequestId);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);

        Set<String> reservationIds = new HashSet<String>();
        for (ReservationRequestSummary reservationRequest : response.getItems()) {
            String lastReservationId = reservationRequest.getLastReservationId();
            if (lastReservationId != null) {
                reservationIds.add(lastReservationId);
            }
        }
        Map<String, Reservation> reservationById = new HashMap<String, Reservation>();
        if (reservationIds.size() > 0) {
            List<Reservation> reservations = reservationService.getReservations(securityToken, reservationIds);
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
                child.put("parentReservationRequestId", reservationRequest.getParentReservationRequestId());
                child.put("reservationId", reservation.getId());
                if (room != null) {
                    child.put("roomId", room.getId());

                    // Set room state available
                    ExecutableState roomState = room.getState();
                    child.put("roomStateAvailable", roomState.isAvailable());

                    // Set room aliases
                    List<Alias> aliases = room.getAliases();
                    child.put("roomAliases", RoomModel.formatAliases(aliases, roomState.isAvailable()));
                    child.put("roomAliasesDescription", RoomModel.formatAliasesDescription(
                            aliases, roomState.isAvailable(), new MessageProviderImpl(messageSource, locale)));
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
    @RequestMapping(value = ClientWebUrl.DETAIL_RESERVATION_REQUEST_USAGES, method = RequestMethod.GET)
    @ResponseBody
    public Map handleDetailUsages(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false, defaultValue = "SLOT") ReservationRequestListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending)
    {
        String reservationRequestId = getReservationRequestId(securityToken, objectId);

        // List reservation requests
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        request.setReusedReservationRequestId(reservationRequestId);
        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);

        // Get permissions for reservation requests
        Map<String, Set<ObjectPermission>> permissionsByReservationRequestId =
                cache.getReservationRequestsPermissions(securityToken, response.getItems());

        // Build response
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, locale, timeZone);
        List<Map<String, Object>> usages = new LinkedList<Map<String, Object>>();
        for (ReservationRequestSummary reservationRequest : response.getItems()) {
            Map<String, Object> item = new HashMap<String, Object>();
            String usageId = reservationRequest.getId();
            item.put("id", usageId);
            item.put("description", reservationRequest.getDescription());
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

            Set<ObjectPermission> objectPermissions = permissionsByReservationRequestId.get(usageId);
            item.put("isWritable", objectPermissions.contains(ObjectPermission.WRITE));

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

            item.put("roomParticipantCount", reservationRequest.getRoomParticipantCount());
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
     * @param securityToken
     * @param userSession
     * @param reservationRequest
     * @param isLatestAllocated
     * @return {@link cz.cesnet.shongo.client.web.models.ReservationRequestDetailModel} state
     */
    public ReservationRequestDetailModel getReservationRequestState(SecurityToken securityToken,
            UserSession userSession, AbstractReservationRequest reservationRequest, boolean isLatestAllocated)
    {
        final Locale locale = userSession.getLocale();
        final DateTimeZone timeZone = userSession.getTimeZone();
        final CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        final MessageProvider messageProvider = new MessageProviderImpl(messageSource, locale, timeZone);

        // Get reservation
        Reservation reservation = null;
        if (isLatestAllocated) {
            reservation = reservationRequest.getLastReservation(reservationService, securityToken);
        }

        // Create reservation request model
        ReservationRequestDetailModel reservationRequestModel = new ReservationRequestDetailModel(
                reservationRequest, reservation, cacheProvider,
                messageProvider, executableService, userSession);
        reservationRequestModel.loadUserRoles(securityToken, authorizationService);
        return reservationRequestModel;
    }
}
