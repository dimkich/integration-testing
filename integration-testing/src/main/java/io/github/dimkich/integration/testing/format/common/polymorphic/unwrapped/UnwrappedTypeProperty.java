package io.github.dimkich.integration.testing.format.common.polymorphic.unwrapped;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the name of the attribute (field) that holds the polymorphic type id of an
 * unwrapped value.
 * <p>
 * Unwrapped values use {@code "utype"} by default: unlike a regular typed property, the
 * type attribute is written into the same XML element as the declaring object, so
 * {@code "type"} may already be occupied by the declaring object itself (for example
 * {@code <data type="MapAttrNotWrapped" key="k1" utype="Integer">1</data>}).
 * <p>
 * When there is no such collision the attribute may be renamed, which makes the syntax
 * more natural: {@code <property name="amount" type="PeriodDuration">PT0M</property>}.
 * <p>
 * The annotation is read both by {@code PolymorphicUnwrappedSerializerModifier} (the name
 * of the attribute to write) and by {@code PolymorphicUnwrappedDeserializerModifier} (the
 * name of the property the type deserializer looks for). It may be placed either on the
 * unwrapped field or on the declaring class.
 *
 * @see io.github.dimkich.integration.testing.format.xml.polymorphic.PolymorphicUnwrappedSerializerModifier
 * @see io.github.dimkich.integration.testing.format.xml.polymorphic.PolymorphicUnwrappedDeserializerModifier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface UnwrappedTypeProperty {
    /**
     * The name of the attribute that holds the type id of the unwrapped value.
     *
     * @return attribute name
     */
    String value();
}
