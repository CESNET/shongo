package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.UserInformation;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link AbstractPerson} which is not known to local Shongo domain.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class ForeignPerson extends AbstractPerson
{
    /**
     * Name of the person.
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
     * Set of user principal names.
     */
    private Set<String> principalNames = new HashSet<String>();

    /**
     * Constructor.
     */
    public ForeignPerson()
    {
    }

    /**
     * Constructor.
     *
     * @param name  sets the {@link #name}
     * @param email sets the {@link #email}
     */
    public ForeignPerson(String name, String email)
    {
        setName(name);
        setEmail(email);
    }

    /**
     * @return {@link #userId}
     */
    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
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
     * @return {@link #principalNames}
     */
    public Set<String> getPrincipalNames()
    {
        return principalNames;
    }

    /**
     * @param principalNames sets the {@link #principalNames}
     */
    public void setPrincipalNames(Set<String> principalNames)
    {
        this.principalNames = principalNames;
    }

    /**
     * @param principalName to be added to the {@link #principalNames}
     */
    public void addPrincipalName(String principalName)
    {
        this.principalNames.add(principalName);
    }

    /**
     * @return {@link UserInformation}
     */
    public UserInformation getUserInformation()
    {
        UserInformation userInformation = new UserInformation();
        userInformation.setUserId(userId);
        userInformation.setFirstName(getFirstName());
        userInformation.setLastName(getLastName());
        userInformation.setOrganization(organization);
        userInformation.setEmail(email);
        userInformation.setPrincipalNames(principalNames);
        return userInformation;
    }

    /**
     * @param email sets the {@link #name}
     */
    public void setEmail(String email)
    {
        this.email = email;
    }

    public static final String USER_ID = "userId";
    public static final String NAME = "name";
    public static final String ORGANIZATION = "organization";
    public static final String EMAIL = "email";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        dataMap.set(NAME, name);
        dataMap.set(ORGANIZATION, name);
        dataMap.set(EMAIL, email);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userId = dataMap.getStringRequired(USER_ID, Controller.USER_ID_COLUMN_LENGTH);
        name = dataMap.getStringRequired(NAME, DEFAULT_COLUMN_LENGTH);
        organization = dataMap.getString(ORGANIZATION, DEFAULT_COLUMN_LENGTH);
        email = dataMap.getStringRequired(EMAIL, DEFAULT_COLUMN_LENGTH);
    }

    public String getFirstName()
    {
        int index = name.lastIndexOf(" ");
        if (index != -1) {
            return name.substring(0, index);
        }
        else {
            return null;
        }
    }

    public String getLastName()
    {
        int index = name.lastIndexOf(" ");
        if (index != -1) {
            return name.substring(index + 1);
        }
        else {
            return name;
        }
    }
}
