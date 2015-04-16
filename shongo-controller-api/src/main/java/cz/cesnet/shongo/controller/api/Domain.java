package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Information about controlled or foreign domain.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Domain extends IdentifiedComplexType
{
    /**
     * Represents an unique domain name (e.g., "cz.cesnet")
     */
    private String name;

    /**
     * Represents a user-visible domain organization (e.g., "CESNET, z.s.p.o.").
     */
    private String organization;

    /**
     * Status of the domain.
     */
    private Status status;

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
     * @return {@link #status}
     */
    public Status getStatus()
    {
        return status;
    }

    /**
     * @param status sets the {@link #status}
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }

    private static final Pattern GLOBAL_ID_PATTERN = Pattern.compile("shongo:.*:(\\d+)");

    /**
     * @param globalId from which the local id should be returned
     * @return local id from given {@code globalId}
     */
    public static String getLocalId(String globalId)
    {
        Matcher matcher = GLOBAL_ID_PATTERN.matcher(globalId);

        if (matcher.matches() && matcher.groupCount() == 1) {
            String id = matcher.group(1);
            return id;
        }
        throw new IllegalArgumentException(String.format("The identifier '%s' isn't valid global identifier!",
                globalId));
    }

    private static final String NAME = "name";
    private static final String ORGANIZATION = "organization";
    private static final String STATUS = "status";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(NAME, name);
        dataMap.set(ORGANIZATION, organization);
        dataMap.set(STATUS, status);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        name = dataMap.getString(NAME);
        organization = dataMap.getString(ORGANIZATION);
        status = dataMap.getEnum(STATUS, Status.class);
    }

    /**
     * Status of a domain.
     *
     */
    @jade.content.onto.annotations.Element(name = "DomainStatus")
    public enum Status
    {
        /**
         * Means that domain is currently available to the controller.
         */
        AVAILABLE,

        /**
         * Means that domain is currently not available to the controller.
         */
        NOT_AVAILABLE
    }
}
