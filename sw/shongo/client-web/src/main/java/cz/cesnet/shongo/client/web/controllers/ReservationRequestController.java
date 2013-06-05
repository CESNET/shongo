package cz.cesnet.shongo.client.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
    @RequestMapping(value = {"", "/list"}, method = RequestMethod.GET)
    public String getList()
    {
        return "reservationRequestList";
    }

    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public String getDetail()
    {
        return "reservationRequestDetail";
    }

    @RequestMapping(value = "/delete", method = RequestMethod.GET)
    public String getDelete()
    {
        return "reservationRequestDelete";
    }

    @RequestMapping(value = "/data", method = RequestMethod.GET)
    @ResponseBody
    public Map getDataList(
            @RequestParam(value = "start", required = false, defaultValue = "0") Integer start,
            @RequestParam(value = "count", required = false) Integer count)
    {
        int totalCount = 21;
        if (start <= 0) {
            start = 0;
        }
        int end = ((count != null && count != -1) ? (start + count) : totalCount);
        if (end > totalCount) {
            end = totalCount;
        }

        List<Map<String, Object>> reservationRequests = new LinkedList<Map<String, Object>>();
        for (int index = start; index < end; index++) {
            Map<String, Object> reservationRequest = new HashMap<String, Object>();
            reservationRequest.put("id", "shongo:cz.cesnet:req:" + index);
            reservationRequest.put("description", "test " + index);
            reservationRequests.add(reservationRequest);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", start);
        data.put("count", totalCount);
        data.put("items", reservationRequests);
        return data;
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
