package cz.cesnet.shongo.api;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.util.StringHelper;
import jade.content.Concept;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents {@link PersonInformation} for a Shongo user.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserInformation extends AbstractComplexType implements PersonInformation, Concept
{
    /**
     * Shongo user-id.
     */
    private String userId;

    /**
     * Set of user principal names.
     */
    private Set<String> principalNames = new HashSet<String>();

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

    private String fullName;

    private String locale;

    private String zoneInfo;

    private Set<String> eduPersonEntitlement = new HashSet<String>();

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
     * @return {@link #principalNames}
     */
    public Set<String> getPrincipalNames()
    {
        return Collections.unmodifiableSet(principalNames);
    }

    /**
     * @param principalName
     * @return true whether {@link #principalNames} contains given {@code principalName}, false otherwise
     */
    public boolean hasPrincipalName(String principalName)
    {
        return principalNames.contains(principalName);
    }

    /**
     * @param principalNames sets the {@link #principalNames}
     */
    public void setPrincipalNames(Set<String> principalNames)
    {
        this.principalNames.clear();
        this.principalNames.addAll(principalNames);
    }

    /**
     * @param principalName to be added to the {@link #principalNames}
     */
    public void addPrincipalName(String principalName)
    {
        this.principalNames.add(principalName);
    }

    public void addEduPersonEntitlement(String eduPersonEntitlement){
        this.eduPersonEntitlement.add(eduPersonEntitlement);
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

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String getFullName()
    {
        if(fullName != null) {
            return fullName;
        }
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
        if (fullName.length() > 0) {
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
        return email;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getZoneInfo() {
        return zoneInfo;
    }

    public void setZoneInfo(String zoneInfo) {
        this.zoneInfo = zoneInfo;
    }

    public Set<String> getEduPersonEntitlement() {
        return eduPersonEntitlement;
    }

    public void setEduPersonEntitlement(Set<String> eduPersonEntitlement) {
        this.eduPersonEntitlement = eduPersonEntitlement;
    }

    @Override
    public String toString()
    {
        return String.format("User (id: %s, name: %s)", getUserId(), getFullName());
    }

    public static final String USER_ID = "userId";
    public static final String PRINCIPAL_NAMES = "principalNames";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String ORGANIZATION = "organization";
    public static final String EMAIL = "email";
    public static final String LOCALE = "locale";
    public static final String ZONE_INFO = "zoneInfo";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        dataMap.set(PRINCIPAL_NAMES, principalNames);
        dataMap.set(FIRST_NAME, firstName);
        dataMap.set(LAST_NAME, lastName);
        dataMap.set(ORGANIZATION, organization);
        dataMap.set(EMAIL, email);
        dataMap.set(LOCALE, locale);
        dataMap.set(ZONE_INFO, zoneInfo);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userId = dataMap.getString(USER_ID);
        principalNames = dataMap.getSet(PRINCIPAL_NAMES, String.class);
        firstName = dataMap.getString(FIRST_NAME);
        lastName = dataMap.getString(LAST_NAME);
        organization = dataMap.getString(ORGANIZATION);
        email = dataMap.getString(EMAIL);
        locale = dataMap.getString(LOCALE);
        zoneInfo = dataMap.getString(ZONE_INFO);
    }

    /**
     * Remove {@link UserInformation}s from given {@code users} which doesn't match given {@code search} criteria.
     *
     * @param users
     * @param search
     */
    public static void filter(List<UserInformation> users, String search)
    {
        search = StringHelper.removeAccents(search);
        for (Iterator<UserInformation> iterator = users.iterator(); iterator.hasNext(); ) {
            UserInformation userInformation = iterator.next();

            // Filter by data
            StringBuilder filterData = new StringBuilder();
            filterData.append(userInformation.getFirstName());
            filterData.append(" ");
            filterData.append(userInformation.getLastName());
            filterData.append(userInformation.getEmail());
            filterData.append(userInformation.getOrganization());
            filterData.append(userInformation.getLocale());
            filterData.append(userInformation.getZoneInfo());
            if (!StringUtils.containsIgnoreCase(StringHelper.removeAccents(filterData.toString()), search)) {
                iterator.remove();
            }
        }
    }

    /**
     * Local identifier pattern einfra.
     */
    private static Pattern LOCAL_IDENTIFIER_PATTERN_EINFRA = Pattern.compile("^(.+)@(.+)$");

    /**
     * Local identifier pattern perun.
     */
    private static Pattern LOCAL_IDENTIFIER_PATTERN_PERUN = Pattern.compile("^\\d+|\\*$");
    /**
     * Local identifier pattern with type (<domain-id>:<user-id>).
     */
    private static Pattern FOREIGN_IDENTIFIER_PATTERN = Pattern.compile("^(\\d+|\\*):(\\d+|\\*)$");


    public static boolean isLocal(String userId)
    {
        return LOCAL_IDENTIFIER_PATTERN_EINFRA.matcher(userId).matches() || LOCAL_IDENTIFIER_PATTERN_PERUN.matcher(userId).matches();
    }

    public static Long parseDomainId(String userId)
    {
        Matcher matcher = FOREIGN_IDENTIFIER_PATTERN.matcher(userId);
        if (matcher.matches()) {
            return Long.parseLong(matcher.group(0));
        }

        return null;
//        throw new IllegalArgumentException("Wrong format of userId '" + userId + "'.");
    }

    public static String parseUserId(String userId)
    {
        Matcher matcher = FOREIGN_IDENTIFIER_PATTERN.matcher(userId);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        else {
            //TODO: check if local?
            return userId;
        }
    }

    public static String formatForeignUserId(String userId, Long domainId)
    {
        if (!isLocal(userId)) {
            throw new IllegalArgumentException("Wrong format of userId '" + userId + "'.");
        }
        return formatForeignUnknownUserId(userId, domainId);
    }

    public static String formatForeignUnknownUserId(String userId, Long domainId)
    {
        return domainId + ":" + userId;
    }
}
