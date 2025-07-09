package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.models.ParticipantModel;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.controller.api.SecurityToken;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * Controller for creating with participants.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class WizardParticipantsController extends AbstractWizardController
{
    protected static final String PARTICIPANT_ATTRIBUTE = "participant";

    @Resource
    private Cache cache;

    /**
     * @param httpSession
     * @return {@link ReservationRequestModel} from given {@code httpSession}
     */
    protected ReservationRequestModel getReservationRequest(HttpSession httpSession)
    {
        Object reservationRequestAttribute = httpSession.getAttribute(RESERVATION_REQUEST_ATTRIBUTE);
        if (reservationRequestAttribute instanceof ReservationRequestModel) {
            return (ReservationRequestModel) reservationRequestAttribute;
        }
        throw new IllegalStateException("Reservation request doesn't exist.");
    }

    /**
     * Show form for adding new participant.
     *
     * @param wizardPageId
     * @param reservationRequest
     */
    protected WizardView handleParticipantCreate(Object wizardPageId, ReservationRequestModel reservationRequest,
            SecurityToken securityToken)
    {
        return handleParticipantView(wizardPageId, reservationRequest,
                new ParticipantModel(new CacheProvider(cache, securityToken)));
    }

    /**
     * Show form for adding new participant.
     *
     * @param wizardPageId
     * @param reservationRequest
     * @param participant
     */
    protected WizardView handleParticipantView(Object wizardPageId, ReservationRequestModel reservationRequest,
            ParticipantModel participant)
    {
        WizardView wizardView = getWizardView(wizardPageId, "wizardRoomParticipant.jsp");
        wizardView.addObject(PARTICIPANT_ATTRIBUTE, participant);
        wizardView.setNextPageUrl(null);
        wizardView.setPreviousPageUrl(null);
        return wizardView;
    }

    /**
     * Show form for modifying existing participant.
     *
     * @param wizardPageId
     * @param reservationRequest
     * @param participantId
     */
    protected WizardView handleParticipantModify(Object wizardPageId, ReservationRequestModel reservationRequest,
            String participantId)
    {
        ParticipantModel participant = reservationRequest.getParticipant(participantId);
        return handleParticipantView(wizardPageId, reservationRequest, participant);
    }

    /**
     * Show form for modifying existing participant.
     *
     * @param wizardPageId
     * @param reservationRequest
     * @param participant
     */
    protected WizardView handleParticipantModify(Object wizardPageId, ReservationRequestModel reservationRequest,
            ParticipantModel participant)
    {
        WizardView wizardView = getWizardView(wizardPageId, "wizardRoomParticipant.jsp");
        wizardView.addObject(PARTICIPANT_ATTRIBUTE, participant);
        wizardView.setNextPageUrl(null);
        wizardView.setPreviousPageUrl(null);
        return wizardView;
    }
}
