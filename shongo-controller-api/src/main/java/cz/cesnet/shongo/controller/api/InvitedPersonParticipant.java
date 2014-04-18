package cz.cesnet.shongo.controller.api;

/**
 * {@link PersonParticipant} who should be invited to a meeting.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class InvitedPersonParticipant extends PersonParticipant
{
    /**
     * Constructor.
     */
    public InvitedPersonParticipant()
    {
    }

    /**
     * Constructor.
     *
     * @param name  sets the {@link AnonymousPerson#name} for the {@link #PERSON}
     * @param email sets the {@link AnonymousPerson#email} for the {@link #PERSON}
     */
    public InvitedPersonParticipant(String name, String email)
    {
        setPerson(new AnonymousPerson(name, email));
    }
}
