package cz.cesnet.shongo.client.web.support.interceptors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for controller handler methods which should not invoke date/time zone detection.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreDateTimeZone
{
}
