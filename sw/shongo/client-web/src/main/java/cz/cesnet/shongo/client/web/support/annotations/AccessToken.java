package cz.cesnet.shongo.client.web.support.annotations;

import java.lang.annotation.*;

/**
 * Annotation which indicates that a method parameter should be bound to a
 * {@link cz.cesnet.shongo.controller.api.SecurityToken} for current user.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AccessToken
{
}
