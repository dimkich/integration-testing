package io.github.dimkich.integration.testing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@State(name = "org.intellij.sdk.settings.AppSettings", storages = @Storage("IntegrationTestsSettingsPlugin.xml"))
public class ArgStorage implements PersistentStateComponent<PluginState> {
    private final Map<String, Arg> args = Stream.of(
            new ArgComboBox("assertion", "-Dintegration.testing.assertion=", "string - assert test cases as strings;<br /><br />saveActualData - no actual assertion is performed. Saving actual data from code;<br /><br />file - assert test cases as files, what allows transfer changes in convenient file compare dialog. After all transfers are done, press 'Save file assertion' button;<br /><br />singleFile - all test cases are compared in a single file compare dialog",
                    "string", "saveActualData", "file", "singleFile"),
            new ArgCheckBox("useMocks", "-Dintegration.testing.environment=mock", "Try to use less services started with docker. Speed up tests for local development."),
            new ArgCheckBox("mockAlwaysCallRealMethods", "-Dintegration.testing.mock.mockAlwaysCallRealMethods=true", "Allways calls real methods of mocks declared with @TestCaseBeanMocks. Used to fill test data from real service calls."),
            new ArgCheckBox("mockCallRealMethodsOnNoData", "-Dintegration.testing.mock.mockCallRealMethodsOnNoData=true", "Calls real method of mocks declared with @TestCaseBeanMocks, only when data for mock is not filled. Used to fill test data from real service calls."),
            new ArgCheckBox("mockReturnMockOnNoData", "-Dintegration.testing.mock.mockReturnMockOnNoData=true", "Returns Mokito deep mock when calling method of mock declared with @TestCaseBeanMocks, only when data for mock is not filled. Used to fill initial data."),
            new ArgCheckBox("spyCreateData", "-Dintegration.testing.mock.spyCreateData=true", "Allways fill data of spy method calls declared with @TestCaseBeanMocks. Used to fill initial data."),
            new ArgLabel("Hibernate"),
            new ArgCheckBox("hibernateShowSql", "-Dspring.jpa.properties.hibernate.show_sql=true", "Show hibernate sql"),
            new ArgCheckBox("hibernateUseSqlComments", "-Dspring.jpa.properties.hibernate.use_sql_comments=true", "Show hibernate sql comments"),
            new ArgCheckBox("hibernateFormatSql", "-Dspring.jpa.properties.hibernate.format_sql=true", "Format hibernate sql"),
            new ArgCheckBox("hibernateShowBindParams", "-Dlogging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE -Dlogging.level.org.hibernate.orm.jdbc.bind=TRACE", "Show parameters binded to sql")
    ).collect(Collectors.toMap(Arg::getText, Function.identity(), (x, y) -> y, LinkedHashMap::new));

    public static ArgStorage getInstance() {
        return ApplicationManager.getApplication().getService(ArgStorage.class);
    }

    public List<Arg> getAll() {
        return new ArrayList<>(args.values());
    }

    public Collection<String> getJvmArgs() {
        return args.values().stream()
                .map(Arg::getArg)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public @Nullable PluginState getState() {
        PluginState state = new PluginState();
        state.args = args.values().stream()
                .filter(a -> a.getValue() != null)
                .collect(Collectors.toMap(Arg::getText, Arg::getValue));
        return state;
    }

    @Override
    public void loadState(@NotNull PluginState state) {
        for (Map.Entry<String, String> entry : state.args.entrySet()) {
            if (args.containsKey(entry.getKey())) {
                args.get(entry.getKey()).setValue(entry.getValue());
            }
        }
    }
}
