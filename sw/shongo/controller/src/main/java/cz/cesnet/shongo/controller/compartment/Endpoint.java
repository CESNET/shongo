package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.Entity;

/**
 * Represents an entity which can participate in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class Endpoint extends PersistentObject
{
}
