package cz.cesnet.shongo;

/**
 * Information about person.
 */
public interface PersonInformation
{
    /**
     * @return full name of the person
     */
    public String getFullName();

    /**
     * @return root organization of the person
     */
    public String getRootOrganization();

    /**
     * @return primary email of the person
     */
    public String getPrimaryEmail();
}
