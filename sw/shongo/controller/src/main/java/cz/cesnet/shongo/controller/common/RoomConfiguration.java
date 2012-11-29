package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.PersistentObject;
import org.joda.time.DateTime;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

/**
 * Represents a configuration for a {@link Room}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class RoomConfiguration extends PersistentObject
{
}
