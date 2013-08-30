/**
 * Package contains classes for persisting Joda Time classes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@TypeDefs({
        @TypeDef(name = "DateTime", typeClass = PersistentDateTime.class),
        @TypeDef(name = "DateTimeZone", typeClass = PersistentDateTimeZone.class),
        @TypeDef(name = "Period", typeClass = PersistentPeriod.class),
        @TypeDef(name = "Interval", typeClass = PersistentInterval.class),
        @TypeDef(name = "ReadablePartial", typeClass = PersistentReadablePartial.class)
}) package cz.cesnet.shongo.joda;

import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

