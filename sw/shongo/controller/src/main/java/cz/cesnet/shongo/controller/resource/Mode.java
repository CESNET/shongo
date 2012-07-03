package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * Represents a mode of a device.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Mode extends PersistentObject
{
}
