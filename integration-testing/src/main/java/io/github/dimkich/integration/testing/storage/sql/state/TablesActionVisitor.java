package io.github.dimkich.integration.testing.storage.sql.state;

import io.github.dimkich.integration.testing.initialization.SqlStorageSetup;
import lombok.Getter;

import java.util.*;

@Getter
public class TablesActionVisitor {
    private final Set<String> tablesToAllow = new LinkedHashSet<>();
    private final Set<String> tablesToDeny = new LinkedHashSet<>();
    private final Set<String> tablesToClear = new HashSet<>();
    private final Set<String> tablesToLoad = new HashSet<>();
    private final Deque<String> sqls = new ArrayDeque<>();
    private final Set<String> noHookSqls = new LinkedHashSet<>();
    private final Set<SqlStorageSetup.TableHook> hooks = new LinkedHashSet<>();

    public boolean isAnyChanges() {
        return !tablesToAllow.isEmpty() || !tablesToDeny.isEmpty() || !tablesToClear.isEmpty()
                || !tablesToLoad.isEmpty() || !sqls.isEmpty() || !noHookSqls.isEmpty() || !hooks.isEmpty();
    }

    public void clear() {
        tablesToAllow.clear();
        tablesToDeny.clear();
        tablesToClear.clear();
        tablesToLoad.clear();
        sqls.clear();
        noHookSqls.clear();
        hooks.clear();
    }
}
