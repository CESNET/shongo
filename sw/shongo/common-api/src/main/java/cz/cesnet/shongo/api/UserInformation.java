package cz.cesnet.shongo.api;

import cz.cesnet.shongo.PersonInformation;
import jade.content.Concept;

/**
 * Represents an information about user.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserInformation implements PersonInformation, Concept
{
    /**
     * Shongo user-id.
     */
    private String userId;

    /**
     * eduPerson Principal Name.
     */
    private String eduPersonPrincipalName;

    /**
     * Full name of the user.
     */
    private String fullName;

    /**
     * Organization of the user.
     */
    private String organization;

    /**
     * Email of the user.
     */
    private String email;

    /**
     * Constructor.
     */
    public UserInformation()
    {
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
     * @return {@link #eduPersonPrincipalName}
     */
    public String getEduPersonPrincipalName()
    {
        return eduPersonPrincipalName;
    }

    /**
     * @param eduPersonPrincipalName sets the {@link #eduPersonPrincipalName}
     */
    public void setEduPersonPrincipalName(String eduPersonPrincipalName)
    {
        this.eduPersonPrincipalName = eduPersonPrincipalName;
    }

    /**
     * @return {@link #fullName}
     */
    @Override
    public String getFullName()
    {
        return fullName;
    }

    /**
     * @param fullName sets the {@link #fullName}
     */
    public void setFullName(String fullName)
    {
        this.fullName = fullName;
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
     * @param email sets the {@link #email}
     */
    public void setEmail(String email)
    {
        this.email = email;
    }

    @Override
    public String getRootOrganization()
    {
        return getOrganization();
    }

    @Override
    public String getPrimaryEmail()
    {
        return getEmail();
    }

    @Override
    public String toString()
    {
        return String.format("User (id: %s, name: %s)", getUserId(), getFullName());
    }
}
