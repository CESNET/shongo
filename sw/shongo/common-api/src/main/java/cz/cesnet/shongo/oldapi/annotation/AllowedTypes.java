package cz.cesnet.shongo.oldapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used for properties to restrict allowed types.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface AllowedTypes
{
    /**
     * @return array of allowed types for the property
     */
    Class[] value();

    /**
     * @return default value for {@link #value()}
     */
    Class[] defaultValue() default {};
}
