package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * {@link Person} which is known to Shongo by user-id.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserPerson extends Person
{
    /**
     * User-id of the person.
     */
    private String userId;

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

    public static final String USER_ID = "userId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userId = dataMap.getStringRequired(USER_ID);
    }
}
