package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.UserCache;
import cz.cesnet.shongo.client.web.annotations.AccessToken;
import cz.cesnet.shongo.controller.api.AliasSetSpecification;
import cz.cesnet.shongo.controller.api.AliasSpecification;
import cz.cesnet.shongo.controller.api.RoomSpecification;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListResponse;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Controller for managing reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@RequestMapping("/reservation-request")
public class ReservationRequestController
{
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forStyle("MM");

    @Resource
    private MessageSource messageSource;

    @Resource
    private ReservationService reservationService;

    @Resource
    private UserCache userCache;

    @RequestMapping(value = {"", "/list"}, method = RequestMethod.GET)
    public String getList()
    {
        return "reservationRequestList";
    }

    @RequestMapping(value = "/detail/{id:.+}", method = RequestMethod.GET)
    public String getDetail(@PathVariable(value = "id") String id, Model model)
    {
        Map<String, Object> reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("id", id);
        reservationRequest.put("description", "test");
        model.addAttribute("reservationRequest", reservationRequest);
        return "reservationRequestDetail";
    }

    @RequestMapping(value = "/delete/{id:.+}", method = RequestMethod.GET)
    public String getDelete(@PathVariable(value = "id") String reservationRequestId, Model model)
    {
        Map<String, Object> reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("id", reservationRequestId);
        reservationRequest.put("description", "test");
        if (reservationRequestId.endsWith("0")) {
            reservationRequest.put("dependencies", new LinkedList<Map>()
            {{
                    Map<String, Object> reservationRequest = new HashMap<String, Object>();
                    reservationRequest.put("id", "shongo:cz.cesnet:req:11");
                    reservationRequest.put("description", "test");
                    reservationRequest.put("earliestSlot", new Interval("2013-01-01T12:00/2013-01-01T14:00"));
                    add(reservationRequest);
                }});
        }
        model.addAttribute("reservationRequest", reservationRequest);
        return "reservationRequestDelete";
    }

    @RequestMapping(value = "/delete/confirmed", method = RequestMethod.POST)
    public String getDeleteConfirmed(HttpServletRequest request)
    {
        String reservationRequestId = request.getParameter("id");
        return "redirect:/reservation-request";
    }

    @RequestMapping(value = "/data", method = RequestMethod.GET)
    @ResponseBody
    public Map getDataList(
            Locale locale,
            @AccessToken String accessToken,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "type", required = false) ReservationRequestModel.SpecificationType specificationType)
    {
        SecurityToken securityToken = new SecurityToken(accessToken);
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        if (specificationType != null) {
            switch (specificationType) {
                case ALIAS:
                    request.addSpecificationClass(AliasSpecification.class);
                    request.addSpecificationClass(AliasSetSpecification.class);
                    break;
                case ROOM:
                    request.addSpecificationClass(RoomSpecification.class);
                    break;
            }
        }
        ReservationRequestListResponse response = reservationService.listReservationRequestsNew(request);

        DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.withLocale(locale);
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (ReservationRequestListResponse.Item responseItem : response.getItems()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", responseItem.getId());
            item.put("description", responseItem.getDescription());
            item.put("purpose", responseItem.getPurpose());
            item.put("created", dateTimeFormatter.print(responseItem.getCreated()));
            items.add(item);

            UserInformation userInformation = userCache.getUserInformation(securityToken, responseItem.getUserId());
            item.put("user", userInformation.getFullName());

            ReservationRequestModel.Technology technology =
                    ReservationRequestModel.Technology.find(responseItem.getTechnologies());
            if (technology != null) {
                item.put("technology", technology.getTitle());
            }

            ReservationRequestListResponse.AbstractType type = responseItem.getType();
            if (type instanceof ReservationRequestListResponse.RoomType) {
                ReservationRequestListResponse.RoomType roomType = (ReservationRequestListResponse.RoomType) type;
                item.put("type", messageSource.getMessage("views.reservationRequest.specification.room", null, locale));
                item.put("participantCount", roomType.getParticipantCount());
            }
            else if (type instanceof ReservationRequestListResponse.AliasType) {
                ReservationRequestListResponse.AliasType aliasType = (ReservationRequestListResponse.AliasType) type;
                item.put("type", messageSource.getMessage("views.reservationRequest.specification.alias", null, locale));
                if (aliasType.getType().equals(AliasType.ROOM_NAME)) {
                    item.put("roomName", aliasType.getValue());
                }
            }
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("items", items);
        return data;
    }
}
