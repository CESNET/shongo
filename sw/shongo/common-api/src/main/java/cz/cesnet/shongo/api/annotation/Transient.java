package cz.cesnet.shongo.api.annotation;

import cz.cesnet.shongo.api.util.Property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used for properties that should not be serialized (should not be returned in
 * {@link Property#getPropertyNames(Class)}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Transient
{
}
