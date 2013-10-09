package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.models.ParticipantContainer;
import cz.cesnet.shongo.client.web.models.ParticipantModel;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.SessionStatus;

import javax.servlet.http.HttpSession;

/**
 * Controller for creating with participants.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractWizardCreateController extends AbstractWizardController
{
    protected static final String RESERVATION_REQUEST_ATTRIBUTE = "reservationRequest";
    protected static final String PARTICIPANT_ATTRIBUTE = "participant";

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
     * Show form for adding new participant for ad-hoc/permanent room.
     *
     * @param wizardPageId
     * @param reservationRequest
     */
    protected WizardView handleParticipantCreate(Object wizardPageId, ReservationRequestModel reservationRequest)
    {
        return handleParticipantCreate(wizardPageId, reservationRequest, new ParticipantModel());
    }

    /**
     * Show form for adding new participant for ad-hoc/permanent room.
     *
     * @param wizardPageId
     * @param reservationRequest
     * @param participant
     */
    protected WizardView handleParticipantCreate(Object wizardPageId, ReservationRequestModel reservationRequest,
            ParticipantModel participant)
    {
        WizardView wizardView = getWizardView(wizardPageId, "wizardCreateParticipant.jsp");
        wizardView.addObject(PARTICIPANT_ATTRIBUTE, participant);
        wizardView.setNextPageUrl(null);
        wizardView.setPreviousPageUrl(null);
        return wizardView;
    }

    /**
     * Show form for adding new participant for ad-hoc/permanent room.
     *
     * @param participantContainer
     * @param participant
     * @param participantBindingResult
     */
    protected boolean createParticipant(ParticipantContainer participantContainer, ParticipantModel participant,
            BindingResult participantBindingResult)
    {
        participant.validate(participantBindingResult);
        if (participantBindingResult.hasErrors()) {
            return false;
        }
        participantContainer.addParticipant(participant);
        return true;
    }
}
