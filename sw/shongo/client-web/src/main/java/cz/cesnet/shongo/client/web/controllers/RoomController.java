package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.SecurityToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for managing rooms.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@RequestMapping("/reservation-request")
public class RoomController
{
    @RequestMapping(value = "/room/{id:.+}", method = RequestMethod.GET)
    public String getRoom(
            SecurityToken securityToken,
            @PathVariable(value = "id") String roomId, Model model)
    {
        throw new TodoImplementException();
    }
}
