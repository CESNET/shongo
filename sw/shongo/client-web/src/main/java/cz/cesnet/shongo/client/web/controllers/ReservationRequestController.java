package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.annotations.AccessToken;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListResponse;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.Interval;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Controller for managing reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@RequestMapping("/reservation-request")
public class ReservationRequestController
{
    @Resource
    private ReservationService reservationService;

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
    public Object getDataList(
            @AccessToken String accessToken,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count)
    {
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(accessToken);
        request.setStart(start);
        request.setCount(count);
        ReservationRequestListResponse response = reservationService.listReservationRequestsNew(request);
        return response;
    }

    @RequestMapping(value = "/data/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Map getDataSingle(@PathVariable("id") String id)
    {
        Map<String, Object> reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("id", "shongo:cz.cesnet:req:" + id);
        reservationRequest.put("description", "test " + id);
        return reservationRequest;
    }
}
