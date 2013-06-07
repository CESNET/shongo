package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.editors.PeriodEditor;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor());
        binder.registerCustomEditor(Period.class, new PeriodEditor());
    }

    @RequestMapping(value = {"", "/list"}, method = RequestMethod.GET)
    public String getList(HttpServletRequest request)
    {
        request.getSession().setAttribute("back", "/");
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

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String getCreate(Model model)
    {
        ReservationRequestModel reservationRequest = new ReservationRequestModel();
        reservationRequest.setId("-- none --");
        model.addAttribute("reservationRequest", reservationRequest);
        return "reservationRequestCreate";
    }

    @RequestMapping(value = "/create/confirmed", method = {RequestMethod.POST, RequestMethod.GET})
    public String getCreateConfirmed(@ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel,
            BindingResult result)
    {
        reservationRequestModel.validate(result);
        if (result.hasErrors()) {
            return "reservationRequestCreate";
        }
        reservationRequestModel.setId("shongo:cz.cesnet:req:33");
        return "reservationRequestDetail";
    }

    @RequestMapping(value = "/delete/{id:.+}", method = RequestMethod.GET)
    public String getDelete(@PathVariable(value = "id") String id, Model model)
    {
        Map<String, Object> reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("id", id);
        reservationRequest.put("description", "test");
        if (id.endsWith("0")) {
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

    @RequestMapping(value = "/delete/{id:.+}/confirmed", method = RequestMethod.GET)
    public String getDeleteConfirmed(@PathVariable(value = "id") String id)
    {
        return "redirect:/reservation-request";
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
