package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * Represents a configuration for a {@link RoomConfiguration}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class RoomSetting extends PersistentObject
{
}
