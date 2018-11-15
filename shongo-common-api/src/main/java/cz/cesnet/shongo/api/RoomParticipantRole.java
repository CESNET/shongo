package cz.cesnet.shongo.api;

import cz.cesnet.shongo.ParticipantRole;
import jade.content.Concept;

/**
 * Represents a configuration of a role for a participant. The {@link #role} defines the allowed access for the user
 * with given {@link #userId} to the {@link Room}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomParticipantRole extends AbstractComplexType implements Concept
{
    /**
     * Shongo-user-id of the participant.
     */
    private String userId;

    /**
     * {@link ParticipantRole} defining allowed access for the participant with {@link #userId}.
     */
    private ParticipantRole role;

    /**
     * Constructor.
     */
    public RoomParticipantRole()
    {
    }

    /**
     * Constructor.
     *
     * @param userId sets the {@link #userId}
     * @param role   sets the {@link #role}
     */
    public RoomParticipantRole(String userId, ParticipantRole role)
    {
        this.userId = userId;
        this.role = role;
    }

    /**
     * @return {@link #userId}
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #role}
     */
    public ParticipantRole getRole()
    {
        return role;
    }

    /**
     * @param role sets the {@link #role}
     */
    public void setRole(ParticipantRole role)
    {
        this.role = role;
    }

    public static final String USER_ID = "userId";
    public static final String ROLE = "role";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        dataMap.set(ROLE, role);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userId = dataMap.getString(USER_ID);
        role = dataMap.getEnum(ROLE, ParticipantRole.class);
    }

    @Override
    public String toString()
    {
        return String.format("ParticipantRole (userId: %s, role: %s)", userId, role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoomParticipantRole that = (RoomParticipantRole) o;

        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        return role == that.role;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }
}
