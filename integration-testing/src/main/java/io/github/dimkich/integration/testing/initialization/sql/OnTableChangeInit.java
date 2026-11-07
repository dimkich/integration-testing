package io.github.dimkich.integration.testing.initialization.sql;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.initialization.TestInit;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Refreshes the application cache of a SQL storage: invokes the configured bean methods for every
 * table whose database revision is newer than the acknowledged cache revision.
 *
 * <p>Unlike {@link SqlStorageSetup}, which only configures the storage, this is an <b>action</b>: it
 * runs at the moment it is applied and does nothing when no table needs synchronisation. The
 * declaration is scoped by {@code applyTo}, so it can be placed once on a container and inherited by
 * every test case, and an inner declaration (possibly with an empty {@code onTableChange} list)
 * replaces the inherited one for its subtree.
 *
 * <p>Example XML configuration:
 * <pre>{@code
 * <init type="OnTableChangeInit" name="dataSource" applyTo="TestCase">
 *     <onTableChange tableName="users" beanName="cacheHook" beanMethod="reloadUsers"/>
 *     <onTableChange tableName="orders" beanName="cacheHook" beanMethod="reloadOrders"/>
 * </init>
 * }</pre>
 *
 * @author dimkich
 * @see SqlStorageSetup
 */
@Getter
@Setter
@ToString
public class OnTableChangeInit extends TestInit {
    /**
     * The name of the SQL storage whose cache should be refreshed. This must match a configured
     * {@link io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService}.
     */
    @JacksonXmlProperty(isAttribute = true)
    private String name;

    /**
     * Actions that associate a table with a bean method invoked when the table's cache is stale. An
     * empty list disables the actions inherited from an outer {@code OnTableChangeInit}.
     */
    private List<TableChangeAction> onTableChange;

    /**
     * Associates a database table with a no-argument bean method invoked to refresh the cache of
     * that table.
     */
    @Data
    public static class TableChangeAction {
        @JacksonXmlProperty(isAttribute = true)
        private String tableName;

        @JacksonXmlProperty(isAttribute = true)
        private String beanName;

        @JacksonXmlProperty(isAttribute = true)
        private String beanMethod;
    }
}
