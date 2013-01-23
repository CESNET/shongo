package cz.cesnet.shongo;

import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration for available types of aliases.
 */
public enum AliasType
{
    /**
     * Represents an unique room name.
     */
    ROOM_NAME(Technology.ALL, AliasValueType.STRING),

    /**
     * @see cz.cesnet.shongo.Technology#H323
     * @see cz.cesnet.shongo.AliasValueType#E164
     */
    H323_E164(Technology.H323, AliasValueType.E164),

    /**
     * Represents {@link cz.cesnet.shongo.Technology#H323} URI (like an IP address).
     */
    H323_URI(Technology.H323, AliasValueType.URI),

    /**
     * @see cz.cesnet.shongo.Technology#SIP
     * @see cz.cesnet.shongo.AliasValueType#URI
     */
    SIP_URI(Technology.SIP, AliasValueType.URI),

    /**
     * Represents room URL for {@link cz.cesnet.shongo.Technology#ADOBE_CONNECT}.
     */
    ADOBE_CONNECT_URI(Technology.ADOBE_CONNECT, AliasValueType.URI);

    /**
     * @see cz.cesnet.shongo.Technology
     */
    private Technology technology;

    /**
     * @see cz.cesnet.shongo.AliasType
     */
    private AliasValueType valueType;

    /**
     * Constructor.
     *
     * @param technology sets the {@link #technology}
     * @param valueType sets the {@link #valueType}
     */
    private AliasType(Technology technology, AliasValueType valueType)
    {
        this.technology = technology;
        this.valueType = valueType;
    }

    /**
     * @return {@link #technology}
     */
    public Technology getTechnology()
    {
        return technology;
    }

    /**
     * @return {@link #valueType}
     */
    public AliasValueType getValueType()
    {
        return valueType;
    }

    /**
     * @return formatted given {@code aliasTypes} as string
     */
    public static String formatAliasTypes(Set<AliasType> aliasTypes)
    {
        StringBuilder builder = new StringBuilder();
        for (AliasType aliasType : aliasTypes) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(aliasType.toString());
        }
        return builder.toString();
    }

    /**
     * @param technologies
     * @return set of {@link AliasType}s for given {@link #technology}s
     */
    public static Set<AliasType> getAliasTypesForTechnologies(Set<Technology> technologies)
    {
        Set<AliasType> aliasTypes = new HashSet<AliasType>();
        for (AliasType aliasType : AliasType.class.getEnumConstants()) {
            if (technologies.contains(aliasType.getTechnology())) {
                aliasTypes.add(aliasType);
            }
        }
        return aliasTypes;
    }
}
