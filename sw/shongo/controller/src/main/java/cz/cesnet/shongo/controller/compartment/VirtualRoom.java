package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.Entity;

/**
 * Represents an {@link Endpoint} which is able to interconnect multiple other {@link Endpoint}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class VirtualRoom extends Endpoint
{
    /**
     * @return {@link #portCount}
     */
    public Integer getPortCount()
    {
        throw new TodoImplementException();
    }
}
