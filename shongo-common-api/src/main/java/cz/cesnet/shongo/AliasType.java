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
     * Represents {@link cz.cesnet.shongo.Technology#H323} URI (like an "<number>@<ip-address>").
     */
    H323_URI(Technology.H323, AliasValueType.URI),

    /**
     * Represents {@link cz.cesnet.shongo.Technology#H323} IP (like an "<ip-address> <number>#").
     */
    H323_IP(Technology.H323, AliasValueType.IP),

    /**
     * @see cz.cesnet.shongo.Technology#SIP
     * @see cz.cesnet.shongo.AliasValueType#URI
     */
    SIP_URI(Technology.SIP, AliasValueType.URI),

    /**
     * Represents {@link cz.cesnet.shongo.Technology#SIP} IP (like an "<ip-address> <number>#").
     */
    SIP_IP(Technology.H323, AliasValueType.IP),

    /**
     * Represents guest dial string for SW client LifeSize ClearSea
     */
    CS_DIAL_STRING(Technology.H323, AliasValueType.STRING),

    /**
     * Represents room URL for {@link cz.cesnet.shongo.Technology#ADOBE_CONNECT}.
     */
    ADOBE_CONNECT_URI(Technology.ADOBE_CONNECT, AliasValueType.URI),

    /**
     * Represents conference (room) number for {@link cz.cesnet.shongo.Technology#FREEPBX}.
     */
    FREEPBX_CONFERENCE_NUMBER(Technology.FREEPBX, AliasValueType.E164),

    SKYPE_URI(Technology.SKYPE_FOR_BUSINESS, AliasValueType.URI),

    RTMP_NAME(Technology.RTMP, AliasValueType.STRING),

    WEB_RTC_NAME(Technology.WEBRTC, AliasValueType.STRING),

    /**
     * Represents URL for the Pexip web client.
     */
    WEB_CLIENT_URI(Technology.WEBRTC, AliasValueType.URI),

    ROOM_NUMBER(Technology.H323, AliasValueType.STRING),

    PEXIP_ROOM_NUMBER_URI(Technology.H323, AliasValueType.URI),

    PEXIP_PHONE_NUMBER_URI(Technology.H323, AliasValueType.URI);


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
     * @param valueType  sets the {@link #valueType}
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
     * @return {@link AliasValueType#callable} for {@link #valueType}
     */
    public boolean isCallable()
    {
        return valueType.isCallable();
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
