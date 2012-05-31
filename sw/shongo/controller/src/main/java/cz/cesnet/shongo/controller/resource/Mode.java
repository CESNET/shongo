package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.common.PersistentObject;

import javax.persistence.*;

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
