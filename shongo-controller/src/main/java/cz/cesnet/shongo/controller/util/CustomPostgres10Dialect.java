package cz.cesnet.shongo.controller.util;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.unique.UniqueDelegate;


/**
 * Important for compatibility with older database naming conventions.
 */
public class CustomPostgres10Dialect extends PostgreSQLDialect {

    private final UniqueDelegate uniqueDelegate;

    public CustomPostgres10Dialect() {
        super(DatabaseVersion.make(10, 0));
        uniqueDelegate = new CustomUniqueDelegate(this);
    }

    @Override
    public UniqueDelegate getUniqueDelegate() {
        return uniqueDelegate;
    }
}
