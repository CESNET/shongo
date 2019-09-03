package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.support.editors.DateTimeEditor;
import cz.cesnet.shongo.controller.api.DeviceResource;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@Controller
public class RoomController {

    @Resource
    private ReservationService reservationService;

    @Resource
    private ResourceService resourceService;

    @Resource
    private Cache cache;

    /**
     * Initialize model editors for additional types.
     *
     * @param binder to be initialized
     */
    @InitBinder
    public void initBinder(WebDataBinder binder, DateTimeZone timeZone)
    {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor(timeZone));
    }

    /**
     * Handle data request for list of reservations.
     */
    @RequestMapping(value = ClientWebUrl.ROOM_RESERVATION_LIST_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleReservationListData(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false,
                    defaultValue = "SLOT") ReservationListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "false") boolean sortDescending,
            @RequestParam(value = "resource-id", required = false) String resourceId,
            @RequestParam(value = "interval-from", required = false) DateTime intervalFrom,
            @RequestParam(value = "interval-to", required = false) DateTime intervalTo)

    {

        // List reservations
        ReservationListRequest request = new ReservationListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        cz.cesnet.shongo.controller.api.Resource resource = resourceService.getResource(securityToken, resourceId);
        if (resource instanceof DeviceResource) {
            request.addReservationType(ReservationSummary.Type.ROOM);
        } else if (resource instanceof cz.cesnet.shongo.controller.api.Resource) {
            request.addReservationType(ReservationSummary.Type.RESOURCE);
        }


        if (intervalFrom != null || intervalTo != null) {
            if (intervalFrom == null) {
                intervalFrom = Temporal.DATETIME_INFINITY_START;
            }
            if (intervalTo == null) {
                intervalTo = Temporal.DATETIME_INFINITY_END;
            }
            if (!intervalFrom.isAfter(intervalTo)) {
                request.setInterval(new Interval(intervalFrom, intervalTo));
            }
        }
        if (resourceId != null) {
            request.addResourceId(resourceId);
        }

        ListResponse<ReservationSummary> response = reservationService.listReservations(request);

        // Build response
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, locale, timeZone);
        List<Map<String, Object>> items = new LinkedList<>();
        for (ReservationSummary reservation : response.getItems()) {
            Map<String, Object> item = new HashMap<>();
            String reservationId = reservation.getId();
            item.put("id", reservationId);
            item.put("description", reservation.getReservationRequestDescription());

            if (UserInformation.isLocal(reservation.getUserId())) {
                UserInformation user = cache.getUserInformation(securityToken, reservation.getUserId());
                item.put("ownerName", user.getFullName());
                item.put("ownerEmail", user.getPrimaryEmail());
            }
            else {
                Long domainId = UserInformation.parseDomainId(reservation.getUserId());
                String domainName = resourceService.getDomainName(securityToken, domainId.toString());
                item.put("foreignDomain", domainName);
            }

            Interval slot = reservation.getSlot();
            //CALENDAR
            item.put("start", slot.getStart().toLocalDateTime().toString());
            item.put("end", slot.getEnd().toLocalDateTime().toString());
            //LIST
            item.put("slot", formatter.formatInterval(slot));
            item.put("isDeprecated", slot != null && slot.getEnd().isBeforeNow());

            boolean isWritable = reservation.getIsWritableByUser();
            item.put("isWritable", isWritable);
            if (isWritable) {
                item.put("isPeriodic", reservation.getParentReservationRequestId() != null);
                // Reservation request id of whole request created by user
                item.put("requestId", reservation.getReservationRequestId());
                item.put("parentRequestId", reservation.getParentReservationRequestId());
            }
            items.add(item);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("sort", sort);
        data.put("sort-desc", sortDescending);
        data.put("items", items);
        return data;
    }
}
