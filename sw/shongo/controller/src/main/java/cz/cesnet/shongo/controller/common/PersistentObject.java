package cz.cesnet.shongo.controller.common;

import javax.persistence.*;

/**
 * Represents an object that can be persisted to a database.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@MappedSuperclass
public abstract class PersistentObject
{
    /**
     * Persistent object must have an unique identifier.
     */
    private Long id;

    /**
     * @return {@link #id}
     */
    @Id
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    /**
     * @param id sets the {@link #id}
     */
    private void setId(Long id)
    {
        this.id = id;
    }
}
