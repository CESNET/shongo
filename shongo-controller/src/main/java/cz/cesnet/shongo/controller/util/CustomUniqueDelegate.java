package cz.cesnet.shongo.controller.util;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.UniqueKey;


public class CustomUniqueDelegate extends DefaultUniqueDelegate {
    /**
     * Constructs DefaultUniqueDelegate
     *
     * @param dialect The dialect for which we are handling unique constraints
     */
    public CustomUniqueDelegate(Dialect dialect) {
        super(dialect);
    }

    @Override
    public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
        final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();

        final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
                uniqueKey.getTable().getQualifiedTableName(),
                dialect
        );


        final String constraintName = dialect.quote(getCorrectUniqueKeyName(uniqueKey));
        return dialect.getAlterTableString( tableName )
                + " add constraint " + constraintName + " " + uniqueConstraintSql( uniqueKey );
    }

    @Override
    public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
        final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();

        final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
                uniqueKey.getTable().getQualifiedTableName(),
                dialect
        );

        final StringBuilder buf = new StringBuilder(dialect.getAlterTableString(tableName));
        buf.append( getDropUnique() );
        if ( dialect.supportsIfExistsBeforeConstraintName() ) {
            buf.append( "if exists " );
        }
        buf.append( dialect.quote( getCorrectUniqueKeyName(uniqueKey) ) );
        if ( dialect.supportsIfExistsAfterConstraintName() ) {
            buf.append( " if exists" );
        }
        return buf.toString();
    }

    private String getCorrectUniqueKeyName(org.hibernate.mapping.UniqueKey uniqueKey) {
        StringBuilder buf = new StringBuilder(uniqueKey.getTable().getName());

        for (Column column : uniqueKey.getColumns()) {
            buf.append("_");
            buf.append(column.getName());
        }
        buf.append("_key");
        return buf.toString();
    }
}
