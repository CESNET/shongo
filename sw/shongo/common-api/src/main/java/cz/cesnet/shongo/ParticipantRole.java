package cz.cesnet.shongo;

/**
 * Enumeration of all possible roles for users in meetings.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ParticipantRole
{
    /**
     * Represents a normal user who can join to a meeting.
     */
    PARTICIPANT,

    /**
     * Represents a user who can join to a meeting and
     * who can configure basic settings to allow him to run a presentation.
     */
    PRESENTER,

    /**
     * Represents a user who administrate the meeting and thus he can do all possible actions and configurations.
     */
    ADMINISTRATOR,
}

