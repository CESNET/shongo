package cz.cesnet.shongo.common;

import cz.cesnet.shongo.common.util.Parser;

import java.util.UUID;

/**
 * Represents an unique identifier across whole Shongo.
 * <p/>
 * <identifier> = "shongo:" <type> ":" <domain> ":" <uuid>
 * <type>       = "reservation" | "resource"
 * <domain>     = <STRING> ("." <STRING>)*
 * <uuid>       = <HEX_DIGIT>{36}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public final class Identifier
{
    /**
     * Identifier type.
     */
    public static enum Type
    {
        /**
         * Identifier for a resource.
         */
        RESOURCE("resource"),
        /**
         * Identifier for a reservation.
         */
        RESERVATION("reservation");

        /**
         * Codeword for type.
         */
        private String code;

        /**
         * Construct identifier type.
         *
         * @param code
         */
        private Type(String code)
        {
            this.code = code;
        }

        /**
         * @return type converted to string.
         */
        public String toString()
        {
            return code;
        }

        /**
         * @param type
         * @return type converted from given string.
         */
        public static Type fromString(String type)
        {
            if (type.equals(RESOURCE.toString())) {
                return RESOURCE;
            }
            else if (type.equals(RESERVATION.toString())) {
                return RESERVATION;
            }
            else {
                throw new IllegalArgumentException("Failed to convert '" + type + "' to " + Type.class.getName() + "!");
            }
        }
    }

    /**
     * Type of identifier.
     */
    private final Type type;

    /**
     * Domain (with subdomains) to which the identifier belongs.
     */
    private final String domain;

    /**
     * Randomly generated UUID of identifier.
     */
    private final java.util.UUID uuid;

    /**
     * Construct a identifier parsed from string.
     *
     * @param identifier
     */
    public Identifier(String identifier)
    {
        try {
            IdentifierParser parser = new IdentifierParser(Parser.getTokenStream(identifier, IdentifierLexer.class));
            parser.parse();

            type = Type.fromString(parser.type);
            domain = parser.domain;
            uuid = UUID.fromString(parser.uuid);
        }
        catch (Exception exception) {
            throw new IllegalArgumentException(
                    String.format("Failed to parse identifier '%s': %s", identifier, exception.getMessage()));
        }
    }

    /**
     * Construct a new identifier with a random UUID.
     *
     * @param type
     * @param domain
     */
    public Identifier(Type type, String domain)
    {
        this.type = type;
        this.domain = domain;
        this.uuid = UUID.randomUUID();
    }

    /**
     * @return {@link #type}
     */
    public Type getType()
    {
        return type;
    }

    /**
     * @return {@link #domain}
     */
    public String getDomain()
    {
        return domain;
    }

    /**
     * @return {@link #uuid}
     */
    public UUID getUUID()
    {
        return uuid;
    }

    /**
     * @return identifier as string.
     */
    public String toString()
    {
        return String.format("shongo:%s:%s:%s", type, domain, uuid.toString());
    }
}
