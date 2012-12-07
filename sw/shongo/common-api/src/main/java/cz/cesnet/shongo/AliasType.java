package cz.cesnet.shongo;

/**
 * Enumeration for available types of aliases.
 */
public enum AliasType
{
    /**
     * @see cz.cesnet.shongo.Technology#H323
     * @see cz.cesnet.shongo.AliasValueType#E164
     */
    H323_E164(Technology.H323, AliasValueType.E164),

    /**
     * Represents {@link cz.cesnet.shongo.Technology#H323} string identifiers.
     */
    H323_IDENTIFIER(Technology.H323, AliasValueType.STRING),

    /**
     * @see cz.cesnet.shongo.Technology#SIP
     * @see cz.cesnet.shongo.AliasValueType#URI
     */
    SIP_URI(Technology.SIP, AliasValueType.E164.URI),

    /**
     * Represents room name for {@link cz.cesnet.shongo.Technology#ADOBE_CONNECT}.
     */
    ADOBE_CONNECT_NAME(Technology.ADOBE_CONNECT, AliasValueType.STRING),

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
}
