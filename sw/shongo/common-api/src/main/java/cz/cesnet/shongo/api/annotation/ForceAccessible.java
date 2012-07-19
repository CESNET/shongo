package cz.cesnet.shongo.api.annotation;

import cz.cesnet.shongo.api.util.Property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used for properties to make them always accessible by {@link Property} even without
 * {@code forceAccessible} argument in {@link Property#setValue(Object, Object, boolean)}.
 * <p/>
 * It is useful for properties that are read-only at client, but should be sent back to server (it used for example
 * for identification of objects sent back to server).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ForceAccessible
{
}
