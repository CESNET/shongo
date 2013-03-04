package cz.cesnet.shongo.api;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.annotation.Transient;
import jade.content.Concept;

/**
 * Represents information about a Shongo user.
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
     * First name of the use (e.g., given name).
     */
    private String firstName;

    /**
     * Last name of the user (e.g., family name).
     */
    private String lastName;

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
     * @return {@link #firstName}
     */
    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
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
    @Transient
    public String getFullName()
    {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null) {
            fullName.append(firstName);
        }
        if (lastName != null) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName);
        }
        if (fullName.length() > 0 ) {
            return fullName.toString();
        }
        return null;
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
