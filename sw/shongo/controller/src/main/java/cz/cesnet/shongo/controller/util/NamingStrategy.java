package cz.cesnet.shongo.controller.util;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.internal.util.StringHelper;

/**
 * A modified improved naming strategy for hibernate.
 *
 * @author Martin Srom
 * @see org.hibernate.cfg.ImprovedNamingStrategy the original improved naming strategy
 */
public class NamingStrategy extends ImprovedNamingStrategy
{
    /**
     * Replace also $ signs
     */
    @Override
    public String classToTableName(String className)
    {
        return super.classToTableName(className.replace("$", "_"));
    }

    @Override
    public String tableName(String tableName)
    {
        return addUnderscores(tableName.replace("$", "_"));
    }

    @Override
    public String columnName(String columnName)
    {
        return addUnderscores(columnName.replace("$", "_"));
    }

    /**
     * Column name should be whole name not unqualified. (e.g., to keep embeddable name in column name)
     */
    @Override
    public String propertyToColumnName(String propertyName)
    {
        // Fix for [https://hibernate.onjira.com/browse/HHH-6005]
        propertyName = propertyName.replace(".collection&&element.", ".");
        return addUnderscores(propertyName);
    }

    /**
     * Property name should be "undescored".
     */
    @Override
    public String logicalColumnName(String columnName, String propertyName)
    {
        return StringHelper.isNotEmpty(columnName) ? columnName : addUnderscores(propertyName);
    }

    /**
     * Use rather table names instead of property names and it also appends
     * the referenced column name for foreign key column names.
     */
    @Override
    public String foreignKeyColumnName(String propertyName, String propertyEntityName, String propertyTableName,
            String referencedColumnName)
    {
        String header = propertyTableName;
        if (header == null) {
            throw new AssertionFailure("NamingStrategy not properly filled");
        }
        return columnName(header) + "_" + referencedColumnName;
    }
}





