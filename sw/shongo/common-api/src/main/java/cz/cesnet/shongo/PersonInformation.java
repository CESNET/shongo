package cz.cesnet.shongo;

/**
 * Information about a person.
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

    /**
     * Formatter for {@link PersonInformation}.
     */
    public static class Formatter
    {
        /**
         * @param personInformation to be formatted
         * @return formatted {@code personInformation}
         */
        public static String format(PersonInformation personInformation)
        {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(personInformation.getFullName());
            String organization = personInformation.getRootOrganization();
            String email = personInformation.getPrimaryEmail();
            if (organization != null || email != null) {
                stringBuilder.append(" (");
                if (organization != null) {
                    stringBuilder.append(organization);
                }
                if (email != null) {
                    if (organization != null) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(email);
                }
                stringBuilder.append(")");
            }
            return stringBuilder.toString();
        }
    }
}
