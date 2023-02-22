package cz.cesnet.shongo.controller.util;

import com.google.common.base.Strings;
import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.*;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import java.util.Locale;

public class CustomImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl
{
    /**
     * Singleton access
     */
    public static final CustomImplicitNamingStrategy INSTANCE = new CustomImplicitNamingStrategy();

    /**
     * Constructor.
     */
    public CustomImplicitNamingStrategy()
    {
    }

    /**
     * The determinePrimaryTableName.
     *
     * @param source the source.
     * @return the identifier.
     */
    @Override
    public Identifier determinePrimaryTableName(ImplicitEntityNameSource source)
    {
        if (source == null)
        {
            // should never happen, but to be defensive...
            throw new HibernateException("Entity naming information was not provided.");
        }

        String tableName = transformEntityName(source.getEntityNaming());

        if (tableName == null)
        {
            throw new HibernateException("Could not determine primary table name for entity");
        }
        return toIdentifier(tableName, source.getBuildingContext());
    }

    /**
     * The transformEntityName.
     *
     * @param entityNaming the source.
     * @return the identifier.
     */
    protected String transformEntityName(EntityNaming entityNaming)
    {
        // prefer the JPA entity name, if specified...
        if (StringHelper.isNotEmpty(entityNaming.getJpaEntityName()))
        {
            return entityNaming.getJpaEntityName();
        }
        else
        {
            // otherwise, use the Hibernate entity name
            return StringHelper.unqualify(entityNaming.getEntityName());
        }
    }

    /**
     * The determineJoinTableName.
     *
     * @param source the source.
     * @return the identifier.
     */
    @Override
    public Identifier determineJoinTableName(ImplicitJoinTableNameSource source)
    {
        final String ownerPortion = source.getOwningPhysicalTableName();
        final String ownedPortion;
        if (source.getAssociationOwningAttributePath() != null)
        {
            ownedPortion = transformAttributePath(source.getAssociationOwningAttributePath());
        }
        else
        {
            ownedPortion = source.getNonOwningPhysicalTableName();
        }

        return toIdentifier(ownerPortion + "_" + ownedPortion, source.getBuildingContext());
    }

    /**
     * The determineCollectionTableName.
     *
     * @param source the source.
     * @return the identifier.
     */
    @Override
    public Identifier determineCollectionTableName(ImplicitCollectionTableNameSource source)
    {
        final String owningEntityTable = transformEntityName(source.getOwningEntityNaming());
        final String name = transformAttributePath(source.getOwningAttributePath());
        final String entityName;
        if (!Strings.isNullOrEmpty(owningEntityTable))
        {
            entityName = owningEntityTable + "_" + name;
        }
        else
        {
            entityName = name;
        }
        return toIdentifier(entityName, source.getBuildingContext());
    }

    /**
     * The determineIdentifierColumnName.
     *
     * @param source the source.
     * @return the identifier.
     */
    @Override
    public Identifier determineIdentifierColumnName(ImplicitIdentifierColumnNameSource source)
    {
        return toIdentifier(transformAttributePath(source.getIdentifierAttributePath()), source.getBuildingContext());
    }

    /**
     * The determineDiscriminatorColumnName.
     *
     * @param source the source.
     * @return the identifier.
     */
    @Override
    public Identifier determineDiscriminatorColumnName(ImplicitDiscriminatorColumnNameSource source)
    {
        return toIdentifier(
                source.getBuildingContext().getMappingDefaults().getImplicitDiscriminatorColumnName(), source.getBuildingContext());
    }

    /**
     * The determineTenantIdColumnName.
     *
     * @param source the source.
     * @return the identifier.
     */
    @Override
    public Identifier determineTenantIdColumnName(ImplicitTenantIdColumnNameSource source)
    {
        return toIdentifier(source.getBuildingContext().getMappingDefaults().getImplicitTenantIdColumnName(), source.getBuildingContext());
    }

    /**
     * The determineBasicColumnName.
     *
     * @param source the source.
     * @return the identifier.
     */
    @Override
    public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source)
    {
        return toIdentifier(source.getAttributePath().getFullPath().replace(".", "_"), source.getBuildingContext());
    }

    /**
     * The determineJoinColumnName.
     *
     * @param source the source.
     * @return identifier.
     */
    @Override
    public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source)
    {
        final String name;
        name = source.getReferencedTableName().getText()
                + '_'
                + source.getReferencedColumnName().getText();

        return toIdentifier( name, source.getBuildingContext() );
    }

    /**
     * The determinePrimaryKeyJoinColumnName.
     *
     * @param source the source.
     * @return the identifier.
     */
    @Override
    public Identifier determinePrimaryKeyJoinColumnName(ImplicitPrimaryKeyJoinColumnNameSource source)
    {
         return source.getReferencedPrimaryKeyColumnName();
    }

    /**
     * The determineAnyDiscriminatorColumnName.
     *
     * @param source the source.
     * @return the identifier.
     */
    @Override
    public Identifier determineAnyDiscriminatorColumnName(ImplicitAnyDiscriminatorColumnNameSource source)
    {
        return toIdentifier(
                transformAttributePath(source.getAttributePath()) + "_" +
                        source.getBuildingContext().getMappingDefaults().getImplicitDiscriminatorColumnName(),
                source.getBuildingContext());
    }

    /**
     * The determineAnyKeyColumnName.
     *
     * @param source the source.
     * @return the identifier.
     */
    @Override
    public Identifier determineAnyKeyColumnName(ImplicitAnyKeyColumnNameSource source)
    {
        return toIdentifier(
                transformAttributePath(source.getAttributePath()) + "_" +
                        source.getBuildingContext().getMappingDefaults().getImplicitIdColumnName(),
                source.getBuildingContext());
    }

    /**
     * The determineUniqueKeyName.
     *
     * @param source the source.
     * @return the identifier.
     */
    @Override
    public Identifier determineUniqueKeyName(ImplicitUniqueKeyNameSource source)
    {
        Identifier userProvidedIdentifier = source.getUserProvidedIdentifier();
        if (userProvidedIdentifier != null) {
            return userProvidedIdentifier;
        } else {
            StringBuilder name = new StringBuilder();
            name.append(source.getTableName().getText());
            for (Identifier identifier : source.getColumnNames()) {
                name.append("_");
                name.append(identifier.getText());
            }
            name.append("_key");
            return toIdentifier(name.toString(), source.getBuildingContext());
        }
    }

    /**
     * The determineIndexName.
     *
     * @param source the source.
     * @return the identifier.
     */
    @Override
    public Identifier determineIndexName(ImplicitIndexNameSource source)
    {
        Identifier userProvidedIdentifier = source.getUserProvidedIdentifier();
        if (userProvidedIdentifier != null) {
            return userProvidedIdentifier;
        } else {
            return toIdentifier(
                    source.getColumnNames().get(0).toString() + "_idx",
                    source.getBuildingContext()
            );
        }
    }

    /**
     * For JPA standards we typically need the unqualified name.  However, a more usable
     * impl tends to use the whole path.  This method provides an easy hook for subclasses
     * to accomplish that
     *
     * @param attributePath The attribute path
     * @return The extracted name
     */
    protected String transformAttributePath(AttributePath attributePath)
    {
        return attributePath.getProperty();
    }

    /**
     * Easy hook to build an Identifier using the keyword safe IdentifierHelper.
     *
     * @param stringForm The String form of the name
     * @param buildingContext Access to the IdentifierHelper
     * @return The identifier
     */
    @Override
    protected Identifier toIdentifier(String stringForm, MetadataBuildingContext buildingContext) {
        return super.toIdentifier(addUnderscores(stringForm).replace("$", "_"), buildingContext);
    }

    protected static String addUnderscores(String name) {
        final StringBuilder buf = new StringBuilder(name.replace('.', '_'));
        for (int i = 1; i < buf.length() - 1; i++) {
            if (Character.isLowerCase(buf.charAt(i - 1))
                    && Character.isUpperCase(buf.charAt(i))
                    && Character.isLowerCase(buf.charAt(i + 1))) {
                buf.insert(i++, '_');
            }
        }
        return buf.toString().toLowerCase(Locale.ROOT);
    }

}
