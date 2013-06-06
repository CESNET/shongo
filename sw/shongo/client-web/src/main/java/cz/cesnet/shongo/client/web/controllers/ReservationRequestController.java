package cz.cesnet.shongo.client.web.controllers;

import org.joda.time.Interval;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
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
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setId("-- none --");
        model.addAttribute("reservationRequest", reservationRequest);
        return "reservationRequestCreate";
    }

    @RequestMapping(value = "/create/confirmed", method = RequestMethod.POST)
    public String getCreateConfirmed(@ModelAttribute("reservationRequest") ReservationRequest reservationRequest,
            BindingResult result)
    {
        reservationRequest.validate(result);
        if (result.hasErrors()) {
            return "reservationRequestCreate";
        }
        reservationRequest.setId("shongo:cz.cesnet:req:33");
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

    public static class ReservationRequest implements Validator
    {
        private String id;

        private String description;

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        @Override
        public boolean supports(Class<?> type)
        {
            return ReservationRequest.class.equals(type);
        }

        @Override
        public void validate(Object object, Errors errors)
        {
            ReservationRequest reservationRequest = (ReservationRequest) object;
            reservationRequest.validate(errors);
        }

        public void validate(Errors errors)
        {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "description", "validation.field.required");
        }
    }
}
