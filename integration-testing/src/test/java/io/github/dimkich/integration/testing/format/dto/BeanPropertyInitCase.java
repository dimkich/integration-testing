package io.github.dimkich.integration.testing.format.dto;

import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.date.time.PeriodDuration;
import io.github.dimkich.integration.testing.initialization.bean.property.BeanPropertyInit;

import java.util.ArrayList;
import java.util.List;

/**
 * Test case with a {@link BeanPropertyInit} declaration, shared by the XML and JSON mapper tests.
 */
public final class BeanPropertyInitCase {
    private BeanPropertyInitCase() {
    }

    public static TestCase testCase() {
        TestCase testCase = new TestCase();
        testCase.setName("case");

        BeanPropertyInit beanPropertyInit = new BeanPropertyInit();
        beanPropertyInit.setApplyTo(Test.Type.TestCase);
        beanPropertyInit.getProperties().add(entry("fieldAccessBean", "activeFlag", true));
        beanPropertyInit.getProperties().add(entry("fieldAccessBean", "duration", PeriodDuration.valueOf("PT5M")));
        beanPropertyInit.getProperties().add(entry("fieldAccessBean", "text", "case"));
        beanPropertyInit.getProperties().add(entry("fieldAccessBean", "currencies",
                new ArrayList<>(List.of("RUB", "USD"))));
        beanPropertyInit.getProperties().add(entry("fieldAccessBean", "optional", null));
        beanPropertyInit.getProperties().add(entry("propertyAccessBean", "label", "case"));

        testCase.getInits().add(beanPropertyInit);
        return testCase;
    }

    /**
     * Test case with two sibling {@code <init>} elements: the first one names the bean on the
     * {@code <init>} element, the second one names it on every {@code <property>}.
     */
    public static TestCase testCaseWithTwoInits() {
        TestCase testCase = new TestCase();
        testCase.setName("twoInits");

        BeanPropertyInit first = new BeanPropertyInit();
        first.setBean("fieldAccessBean");
        first.getProperties().add(entry(null, "activeFlag", true));
        first.getProperties().add(entry(null, "duration", PeriodDuration.valueOf("PT5M")));
        first.getProperties().add(entry(null, "text", "case"));
        first.getProperties().add(entry(null, "optional", null));

        BeanPropertyInit second = new BeanPropertyInit();
        second.setApplyTo(Test.Type.TestCase);
        second.getProperties().add(entry("propertyAccessBean", "label", "case"));

        testCase.getInits().add(first);
        testCase.getInits().add(second);
        return testCase;
    }

    private static BeanPropertyInit.PropertyEntry entry(String bean, String name, Object value) {
        BeanPropertyInit.PropertyEntry entry = new BeanPropertyInit.PropertyEntry();
        entry.setBean(bean);
        entry.setName(name);
        entry.setValue(value);
        return entry;
    }
}
