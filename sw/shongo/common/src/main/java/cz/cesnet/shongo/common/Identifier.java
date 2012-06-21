package cz.cesnet.shongo.common;

import cz.cesnet.shongo.common.util.Parser;

import java.util.UUID;

/**
 * Represents an unique identifier across whole Shongo.
 * <p/>
 * <identifier> = "shongo:" <type> ":" <domain> ":" <uuid>
 * <domain>     = <STRING> ("." <STRING>)*
 * <id>         = <LONG>{36}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public final class Identifier
{
    /**
     * Domain (with subdomains) to which the identifier belongs.
     */
    private final String domain;

    /**
     * Database id.
     */
    private final Long id;

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

            domain = parser.domain;
            id = Long.parseLong(parser.id);
        }
        catch (Exception exception) {
            throw new IllegalArgumentException(
                    String.format("Failed to parse identifier '%s': %s", identifier, exception.getMessage()));
        }
    }

    /**
     * Construct a new identifier from a domain and database id.
     *
     * @param domain
     */
    public Identifier(String domain, long id)
    {
        this.domain = domain;
        this.id = id;
    }

    /**
     * @return {@link #domain}
     */
    public String getDomain()
    {
        return domain;
    }

    /**
     * @return {@link #id}
     */
    public long getId()
    {
        return id;
    }

    /**
     * @return identifier as string.
     */
    public String toString()
    {
        return String.format("shongo:%s:%s", domain, id.toString());
    }
}
