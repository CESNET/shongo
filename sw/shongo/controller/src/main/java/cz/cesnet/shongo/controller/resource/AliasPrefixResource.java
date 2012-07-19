package cz.cesnet.shongo.controller.resource;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Represents a special type of {@link AliasResource} which
 * can be allocated as aliases from a single prefix.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasPrefixResource extends AliasResource
{
    /**
     * Prefix of aliases.
     */
    private String prefix;

    /**
     * @return {@link #prefix}
     */
    @Column
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * @param prefix sets the {@link #prefix}
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }
}
