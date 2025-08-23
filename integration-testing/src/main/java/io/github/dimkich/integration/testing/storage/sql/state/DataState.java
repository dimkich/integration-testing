package io.github.dimkich.integration.testing.storage.sql.state;

import io.github.dimkich.integration.testing.initialization.SqlStorageSetup;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DataState {
    public enum State {LOADED, CLEARED}

    private State state;

    private List<String> sqls = new ArrayList<>();
    private List<String> noHookSqls = new ArrayList<>();
    private SqlStorageSetup.TableHook tableHook;

    private boolean dirty;

    public void merge(DataState state) {
        merge(state, null, null);
    }

    public void merge(DataState state, String tableName, TablesActionVisitor visitor) {
        if (state.dirty) {
            throw new IllegalStateException("Target state can not be dirty");
        }
        if (state.state != null && this.state != state.state || dirty) {
            if (state.state != null && this.state != state.state) {
                this.state = state.state;
            }
            this.sqls.clear();
            this.noHookSqls.clear();
            if (state.state != null && visitor != null) {
                if (tableHook != null) {
                    visitor.getHooks().add(tableHook);
                }
                visitor.getTablesToRestartIdentity().add(tableName);
                switch (this.state) {
                    case LOADED:
                        visitor.getTablesToLoad().add(tableName);
                        break;
                    case CLEARED:
                        visitor.getTablesToClear().add(tableName);
                        break;
                }
            }
        }
        if (!state.sqls.isEmpty()) {
            this.sqls.addAll(this.noHookSqls);
            this.noHookSqls.clear();
        }
        for (int i = intersectTailAndHead(this.sqls, state.sqls); i < state.sqls.size(); i++) {
            this.sqls.add(state.sqls.get(i));
            if (visitor != null) {
                if (tableHook != null) {
                    visitor.getHooks().add(tableHook);
                }
                visitor.getSqls().add(state.sqls.get(i));
            }
        }
        for (int i = intersectTailAndHead(this.noHookSqls, state.noHookSqls); i < state.noHookSqls.size(); i++) {
            this.noHookSqls.add(state.noHookSqls.get(i));
            if (visitor != null) {
                visitor.getNoHookSqls().add(state.noHookSqls.get(i));
            }
        }
        this.dirty = false;
    }

    private int intersectTailAndHead(List<String> tail, List<String> head) {
        if (tail.isEmpty() || head.isEmpty()) {
            return 0;
        }
        for (int i = Math.max(0, tail.size() - head.size()); i < tail.size(); i++) {
            boolean equals = true;
            for (int j = i; j < tail.size() && j - i < head.size(); j++) {
                if (!tail.get(j).equals(head.get(j - i))) {
                    equals = false;
                    break;
                }
            }
            if (equals) {
                return head.size() - i + 1;
            }
        }
        return 0;
    }
}
