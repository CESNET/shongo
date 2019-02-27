package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * {@link AbstractPerson} which is not known to Shongo.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AnonymousPerson extends AbstractPerson
{

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
    public AnonymousPerson()
    {
    }

    /**
     * Constructor.
     *
     * @param name  sets the {@link #name}
     * @param email sets the {@link #email}
     */
    public AnonymousPerson(String name, String email)
    {
        setName(name);
        setEmail(email);
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #organization}
     */
    public String getOrganization()
    {
        return organization;
    }

    /**
     * @param organization sets the {@link #organization}
     */
    public void setOrganization(String organization)
    {
        this.organization = organization;
    }

    /**
     * @return {@link #email}
     */
    public String getEmail()
    {
        return email;
    }

    /**
     * @param email sets the {@link #name}
     */
    public void setEmail(String email)
    {
        this.email = email;
    }

    public static final String NAME = "name";
    public static final String ORGANIZATION = "organization";
    public static final String EMAIL = "email";

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(NAME, name);
        dataMap.set(ORGANIZATION, name);
        dataMap.set(EMAIL, email);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        name = dataMap.getStringRequired(NAME, DEFAULT_COLUMN_LENGTH);
        organization = dataMap.getString(ORGANIZATION, DEFAULT_COLUMN_LENGTH);
        email = dataMap.getStringRequired(EMAIL, DEFAULT_COLUMN_LENGTH);
    }
}
