package cz.cesnet.shongo.oldapi.annotation;

import cz.cesnet.shongo.oldapi.util.Property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used for properties that should not be serialized (should not be returned in
 * {@link Property#getClassPropertyNames(Class)}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Transient
{
}
