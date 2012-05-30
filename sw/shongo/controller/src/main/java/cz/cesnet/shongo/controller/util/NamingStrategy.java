package cz.cesnet.shongo.controller.util;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.ImprovedNamingStrategy;

/**
 * A modified improved naming strategy for hibernate.
 * It uses rather table names instead of property names and it also appends
 * the referenced column name for foreign key column names
 *
 * @author Martin Srom
 * @see org.hibernate.cfg.ImprovedNamingStrategy the original improved naming strategy
 */
public class NamingStrategy extends ImprovedNamingStrategy
{
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



