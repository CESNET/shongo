package cz.cesnet.shongo.client.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for managing reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@RequestMapping("/reservation-requests")
public class ReservationRequestController
{
    @RequestMapping(value = {"", "/list"}, method = RequestMethod.GET)
    public String index()
    {
        return "listReservationRequests";
    }
}
