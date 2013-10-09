package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ParticipantContainer;
import cz.cesnet.shongo.client.web.models.ParticipantModel;
import cz.cesnet.shongo.client.web.support.BackUrl;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller for managing participants.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({
        ParticipantController.PARTICIPANT_SESSION_ATTRIBUTE,
        ParticipantController.PARTICIPANT_CONTAINER_SESSION_ATTRIBUTE})
public class ParticipantController
{
    public static final String PARTICIPANT_SESSION_ATTRIBUTE = "participant";
    public static final String PARTICIPANT_CONTAINER_SESSION_ATTRIBUTE = "participantContainer";


}
