package cz.cesnet.shongo.controller.util;

import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.dialect.unique.UniqueDelegate;


/**
 * Important for compatibility with older database naming conventions.
 */
public class CustomPostgres10Dialect extends PostgreSQL10Dialect {

    private final UniqueDelegate uniqueDelegate;

    public CustomPostgres10Dialect() {
        super();
        uniqueDelegate = new CustomUniqueDelegate(this);
    }

    @Override
    public UniqueDelegate getUniqueDelegate() {
        return uniqueDelegate;
    }
}
