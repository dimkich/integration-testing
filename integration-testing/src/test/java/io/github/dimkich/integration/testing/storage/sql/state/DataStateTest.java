package io.github.dimkich.integration.testing.storage.sql.state;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static io.github.dimkich.integration.testing.storage.sql.state.DataState.State.CLEARED;
import static io.github.dimkich.integration.testing.storage.sql.state.DataState.State.LOADED;
import static org.junit.jupiter.api.Assertions.*;

class DataStateTest {
    static Object[][] mergeData() {
        return new Object[][]{
                {new Object[]{CLEARED}, new Object[]{CLEARED}, new Object[]{CLEARED}},
                {new Object[]{CLEARED}, new Object[]{LOADED}, new Object[]{LOADED}},
                {new Object[]{CLEARED}, new Object[]{null}, new Object[]{CLEARED}},
                {new Object[]{LOADED}, new Object[]{LOADED}, new Object[]{LOADED}},
                {new Object[]{LOADED}, new Object[]{CLEARED}, new Object[]{CLEARED}},
                {new Object[]{LOADED}, new Object[]{null}, new Object[]{LOADED}},
                {new Object[]{LOADED, List.of("s1")}, new Object[]{null}, new Object[]{LOADED, List.of("s1")}},
                {new Object[]{LOADED, List.of("s1")}, new Object[]{null, List.of("s1")},
                        new Object[]{LOADED, List.of("s1")}},
                {new Object[]{LOADED}, new Object[]{null, List.of("s1")}, new Object[]{LOADED, List.of("s1")}},
                {new Object[]{LOADED}, new Object[]{null, List.of("s1")}, new Object[]{LOADED, List.of("s1")}},
                {new Object[]{LOADED, List.of("s1")}, new Object[]{null, List.of("s2")},
                        new Object[]{LOADED, List.of("s1", "s2")}},
                {new Object[]{LOADED, List.of("s1", "s2")}, new Object[]{null, List.of("s2")},
                        new Object[]{LOADED, List.of("s1", "s2")}},
                {new Object[]{LOADED, List.of("s2", "s1")}, new Object[]{LOADED, List.of("s2")},
                        new Object[]{LOADED, List.of("s2", "s1", "s2")}},
                {new Object[]{LOADED, List.of("s1", "s2", "s3")}, new Object[]{null, List.of("s2", "s3")},
                        new Object[]{LOADED, List.of("s1", "s2", "s3")}},
                {new Object[]{LOADED, List.of("s1", "s2", "s3", "s4")}, new Object[]{LOADED, List.of("s2", "s3")},
                        new Object[]{LOADED, List.of("s1", "s2", "s3", "s4", "s2", "s3")}},
                {new Object[]{LOADED, List.of("s1", "s2", "s1", "s2")}, new Object[]{LOADED, List.of("s1", "s2", "s3")},
                        new Object[]{LOADED, List.of("s1", "s2", "s1", "s2", "s3")}},
                //
                {new Object[]{LOADED, List.of("s1", "s2", "s3", "s4")}, new Object[]{CLEARED, List.of("s2", "s3")},
                        new Object[]{CLEARED, List.of("s2", "s3")}},
                {new Object[]{CLEARED, List.of("s1")}, new Object[]{LOADED, List.of("s2", "s3")},
                        new Object[]{LOADED, List.of("s2", "s3")}},
                {new Object[]{CLEARED, List.of("s1"), true}, new Object[]{LOADED, List.of("s2")},
                        new Object[]{LOADED, List.of("s2")}},
                {new Object[]{CLEARED, List.of("s1"), true}, new Object[]{null, List.of("s2")},
                        new Object[]{CLEARED, List.of("s2")}},
        };
    }

    @ParameterizedTest
    @MethodSource("mergeData")
    void merge(Object[] oldData, Object[] newData, Object[] resultData) {
        DataState oldState = createDataState(oldData);
        DataState newState = createDataState(newData);
        oldState.merge(newState);
        assertEquals(createDataState(resultData), oldState);
    }

    private DataState createDataState(Object[] data) {
        DataState ds = new DataState();
        ds.setState((DataState.State) data[0]);
        if (data.length > 1) {
            for (Object sql : (List<?>) data[1]) {
                ds.getSqls().add((String) sql);
            }
        }
        if (data.length > 2) {
            ds.setDirty((boolean) data[2]);
        }
        return ds;
    }
}