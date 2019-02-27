package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * {@link AbstractPerson} which is known to Shongo by user-id.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserPerson extends AbstractPerson
{
    /**
     * User-id of the person.
     */
    private String userId;

    /**
     * Name of the person.
     */
    private String name;

    /**
     * Organization of the person.
     */
    private String organization;

    /**
     * Email for the person.
     */
    private String email;

    /**
     * Constructor.
     */

    /**
     * Constructor.
     */
    public UserPerson()
    {
    }

    /**
     * Constructor.
     *
     * @param userId sets the {@link #userId}
     */
    public UserPerson(String userId)
    {
        this.userId = userId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static final String USER_ID = "userId";
    public static final String USER_ORGANIZATION = "organization";
    public static final String USER_EMAIL = "email";
    public static final String USER_NAME = "name";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        dataMap.set(USER_NAME, name);
        dataMap.set(USER_EMAIL, email);
        dataMap.set(USER_ORGANIZATION, organization);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userId = dataMap.getStringRequired(USER_ID, Controller.USER_ID_COLUMN_LENGTH);
        name = dataMap.getStringRequired(USER_NAME, DEFAULT_COLUMN_LENGTH);
        organization = dataMap.getString(USER_ORGANIZATION, DEFAULT_COLUMN_LENGTH);
        email = dataMap.getStringRequired(USER_EMAIL, DEFAULT_COLUMN_LENGTH);
    }
}
