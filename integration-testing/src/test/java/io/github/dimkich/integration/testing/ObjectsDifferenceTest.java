package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.storage.diff.ObjectsDifference;
import io.github.dimkich.integration.testing.storage.diff.model.DiffResult;
import io.github.dimkich.integration.testing.storage.dto.AttributedValue;
import io.github.dimkich.integration.testing.storage.dto.BaseValue;
import io.github.dimkich.integration.testing.storage.dto.CompactKey;
import io.github.dimkich.integration.testing.storage.dto.CustomId;
import io.github.dimkich.integration.testing.storage.dto.Item;
import io.github.dimkich.integration.testing.storage.dto.KeyWithValuePresenter;
import io.github.dimkich.integration.testing.storage.dto.PresentedList;
import io.github.dimkich.integration.testing.storage.dto.PresentedValue;
import io.github.dimkich.integration.testing.storage.dto.TypedPayload;
import io.github.dimkich.integration.testing.storage.presenter.AttributedValuePresenter;
import io.github.dimkich.integration.testing.storage.presenter.BaseValuePresenter;
import io.github.dimkich.integration.testing.storage.presenter.CompactKeyPresenter;
import io.github.dimkich.integration.testing.storage.presenter.ItemKeyPresenter;
import io.github.dimkich.integration.testing.storage.presenter.KeyWithValuePresenterValue;
import io.github.dimkich.integration.testing.storage.presenter.PresentedListKeyPresenter;
import io.github.dimkich.integration.testing.storage.presenter.PresentedListPresenter;
import io.github.dimkich.integration.testing.storage.presenter.PresentedValuePresenter;
import io.github.dimkich.integration.testing.storage.presenter.TypedPayloadPresenter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.net.URI;
import java.time.MonthDay;
import java.time.Period;
import java.util.ArrayDeque;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("unused")
@IntegrationTesting
@SpringBootTest(classes = ObjectsDifferenceTest.Config.class)
public class ObjectsDifferenceTest {
    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("objects-difference.xml");
    }

    @Configuration
    static class Config {
        @Bean
        TestSetupModule module() {
            return new TestSetupModule()
                    .addSubTypes(HashSet.class, HashMap.class, ArrayDeque.class)
                    .addSubTypes("io.github.dimkich.integration.testing.storage.dto")
                    .addDiffKeyPresenter(Item.class, new ItemKeyPresenter())
                    .addDiffKeyPresenter(CompactKey.class, new CompactKeyPresenter())
                    .addDiffKeyPresenter(PresentedList.class, new PresentedListKeyPresenter())
                    .addDiffPresenter(PresentedValue.class, new PresentedValuePresenter())
                    .addDiffPresenter(AttributedValue.class, new AttributedValuePresenter())
                    .addDiffPresenter(PresentedList.class, new PresentedListPresenter())
                    .addDiffPresenter(KeyWithValuePresenter.class, new KeyWithValuePresenterValue())
                    .addDiffPresenter(TypedPayload.class, new TypedPayloadPresenter())
                    .addDiffPresenter(BaseValue.class, new BaseValuePresenter())
                    .addEqualsForType(CustomId.class, (id1, id2) -> id1.getValue().equals(id2.getValue()))
                    .addEqualsForType(BaseValue.class,
                            (value1, value2) -> value1.getPayload().equals(value2.getPayload()))
                    .addSubTypes(Period.class, MonthDay.class, File.class,
                            URI.class, Locale.class, Currency.class);
        }

        @Bean
        ObjectsDifferenceTestFacade objectsDifferenceTestFacade(ObjectsDifference objectsDifference) {
            return new ObjectsDifferenceTestFacade(objectsDifference);
        }
    }

    @RequiredArgsConstructor
    @SuppressWarnings("unused")
    public static class ObjectsDifferenceTestFacade {
        private final ObjectsDifference difference;

        /**
         * Compares two storage snapshots exactly as the storage sees them: the records are the
         * keys of the given maps. A case that compares plain values puts each of them under the
         * record key {@code value}.
         */
        public Object diff(Map<?, ?> before, Map<?, ?> after) {
            DiffResult diff = new DiffResult();
            diff.put("test", difference.getDifference(before, after));
            return diff.isEmpty() ? null : diff;
        }
    }
}
