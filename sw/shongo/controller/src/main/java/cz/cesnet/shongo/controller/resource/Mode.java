package cz.cesnet.shongo.controller.resource;

import javax.persistence.*;

/**
 * Represents a mode of a device.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Mode
{
    /**
     * Unique identifier in a domain controller database.
     */
    private Long id;

    /**
     * @return {@link #id}
     */
    @Id
    @GeneratedValue
    @Access(AccessType.FIELD)
    public Long getId()
    {
        return id;
    }
}
