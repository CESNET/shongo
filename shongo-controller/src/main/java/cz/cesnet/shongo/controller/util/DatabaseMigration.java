/*
package cz.cesnet.shongo.controller.util;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.tool.hbm2ddl.ConnectionHelper;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

*/
/**
 * Utility class for performing database migrations.
 * <p/>
 * HyperSQL doesn't support transactional DDL so the {@link DatabaseMigration} cannot be used for now.
 * <p/>
 * In future use PostgreSQL which support it:
 * <property name="hibernate.connection.url" value="jdbc:postgresql:shongo"/>
 * <property name="javax.persistence.jdbc.driver" value="org.postgresql.Driver"/>
 * <property name="hibernate.connection.username" value="shongo"/>
 * <property name="hibernate.connection.password" value="shongo"/>
 * <property name="hibernate.dialect" value="org.hibernate.dialect.PostgreSQL82Dialect"/>
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 *//*

public class DatabaseMigration
{
    private static Logger logger = LoggerFactory.getLogger(DatabaseMigration.class);

    */
/**
     * Persistence unit name for creating {@link EntityManagerFactory}.
     *//*

    private String persistenceUnitName;

    */
/**
     * Name of package which contains {@link Migration} classes.
     *//*

    private String migrationPackageName;

    */
/**
     * Source root directory which contains specified package and where should be
     * new {@link Migration} classes generated.
     *//*

    private String sourceDirectory;

    */
/**
     * List of loaded {@link Migration}s.
     *//*

    private List<Migration> migrations;

    */
/**
     * Constructor.
     *
     * @param persistenceUnitName  for {@link EntityManagerFactory} creation
     * @param migrationPackageName for package which contains {@link Migration} classes
     * @param sourceDirectory      source root directory which contains specified package and where should be
     *                             new {@link Migration} classes generated
     *//*

    public DatabaseMigration(String persistenceUnitName, String migrationPackageName, String sourceDirectory)
    {
        this.persistenceUnitName = persistenceUnitName;
        this.migrationPackageName = migrationPackageName;
        this.sourceDirectory = sourceDirectory;
    }

    */
/**
     * Load migration classes from package with {@link #migrationPackageName}.
     *//*

    private void loadMigrations()
    {
        if (migrations != null) {
            return;
        }
        migrations = new ArrayList<Migration>();
        int index = 1;
        while (true) {
            String typeName = String.format("%s.Migration%d", migrationPackageName, index);
            try {
                Class type = Class.forName(typeName);
                Migration migration = (Migration) type.newInstance();
                migrations.add(migration);
            }
            catch (ClassNotFoundException exception) {
                break;
            }
            catch (Exception exception) {
                logger.error(String.format("Cannot instantiate class '%s'.", typeName), exception);
            }
            index++;
        }
    }

    */
/**
     * @return current application version of schema
     *//*

    private int getCurrentVersion()
    {
        loadMigrations();
        return migrations.size();
    }

    */
/**
     * @param entityManager
     * @return current version of database version of schema
     *//*

    private int getDatabaseVersion(EntityManager entityManager)
    {
        try {
            List resultList = entityManager.createNativeQuery("SELECT version FROM migration_version")
                    .getResultList();
            if (resultList.size() > 0) {
                return (Integer) resultList.get(0);
            }
            else {
                return 0;
            }
        }
        catch (Exception selectException) {
            try {
                entityManager.createNativeQuery("CREATE TABLE public.migration_version(version INTEGER NOT NULL)")
                        .executeUpdate();
                entityManager.createNativeQuery("INSERT INTO public.migration_version(version) VALUES(0)")
                        .executeUpdate();
                return getDatabaseVersion(entityManager);
            }
            catch (Exception createException) {
                throw new RuntimeException("Failed to create table 'migration_version'.", createException);
            }
        }
    }

    */
/**
     * Perform all migrations which hasn't been performed yet.
     *
     * @param entityManagerFactory which should be used
     * @return number of migrations which has been performed
     * @throws Exception
     *//*

    public int performMigration(EntityManagerFactory entityManagerFactory) throws Exception
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        int databaseVersion = getDatabaseVersion(entityManager);
        int currentVersion = getCurrentVersion();
        if (databaseVersion < currentVersion) {
            logger.debug("Database migration from {} to {}...", databaseVersion, currentVersion);
            try {
                // Run each migration in separate transaction
                for (int version = databaseVersion; version < currentVersion; version++) {
                    entityManager.getTransaction().begin();

                    // Run migration
                    Migration migration = migrations.get(version);
                    migration.migrate(entityManager);

                    // Run migration query
                    entityManager.createNativeQuery(String.format(
                            "UPDATE public.migration_version SET version = %d;", version + 1)).executeUpdate();

                    entityManager.getTransaction().commit();
                }
            }
            catch (Exception exception) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                throw exception;
            }
            finally {
                entityManager.close();
            }
            return currentVersion - databaseVersion;
        }
        else if (databaseVersion > currentVersion) {
            logger.warn("Database version is ahead current, going back from {} to {}...",
                    databaseVersion, currentVersion);
            entityManager.getTransaction().begin();
            entityManager.createNativeQuery(
                    String.format("UPDATE public.migration_version SET version = %d", currentVersion))
                    .executeUpdate();
            entityManager.getTransaction().commit();
        }
        entityManager.close();
        return 0;
    }

    */
/**
     * Perform migration which hasn't been performed yet and return new {@link EntityManagerFactory}.
     *
     * @return {@link EntityManagerFactory} which can be used by application
     * @throws Exception
     *//*

    public EntityManagerFactory migrate() throws Exception
    {
        EntityManagerFactory entityManagerFactory;

        // Try to create normal entity manager factory
        try {
            entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
        }
        catch (Exception exception) {
            // Create entity manager factory without hbm2ddl
            Map<String, String> properties = new HashMap<String, String>();
            properties.put("hibernate.hbm2ddl.auto", "none");
            entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName, properties);

            // Migrate old migrations
            if (performMigration(entityManagerFactory) > 0) {
                // Try to create normal entity manager factory
                try {
                    entityManagerFactory.close();
                    entityManagerFactory = javax.persistence.Persistence
                            .createEntityManagerFactory(persistenceUnitName);
                    return entityManagerFactory;
                }
                // Fallback to entity manager factory without hbm2ddl
                catch (Exception createException) {
                    entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName, properties);
                }
            }

            // If source directory is filled we try to generate new database migration
            if (sourceDirectory != null) {
                // Generate new migration
                try {
                    logger.debug("Generating database migration....");
                    NewMigration newMigration = generateMigration();
                    if (newMigration != null) {
                        logger.debug("Migration script successfully generated.");
                        migrations.add(newMigration);
                        saveNewMigrationToFile(getCurrentVersion(), newMigration);
                    }
                    else {
                        logger.debug("Migration script is not needed (no schema change found).");
                    }
                }
                catch (Exception migrationException) {
                    entityManagerFactory.close();
                    throw new RuntimeException("Failed to generate database migration.", migrationException);
                }

                // Migrate new migration
                try {
                    performMigration(entityManagerFactory);
                }
                catch (Exception migrateException) {
                    throw migrateException;
                }
                finally {
                    entityManagerFactory.close();
                }
            }

            // Create new entity manager factory
            entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
        }

        // Migrate old migrations
        performMigration(entityManagerFactory);

        return entityManagerFactory;
    }

    */
/**
     * Generate {@link NewMigration}.
     *
     * @return {@link NewMigration}
     * @throws Exception
     *//*

    @SuppressWarnings("deprecation")
    public NewMigration generateMigration() throws Exception
    {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("hibernate.hbm2ddl.auto", "none");

        // Create hibernate configuration
        org.hibernate.ejb.Ejb3Configuration ejb3Configuration = new org.hibernate.ejb.Ejb3Configuration();
        ejb3Configuration = ejb3Configuration.configure(persistenceUnitName, properties);
        if (ejb3Configuration == null) {
            throw new IllegalStateException("EJB3 configuration is null");
        }
        org.hibernate.cfg.Configuration configuration = ejb3Configuration.getHibernateConfiguration();

        // Create dialect
        Properties configurationProperties = configuration.getProperties();
        Dialect dialect = Dialect.getDialect(configurationProperties);

        // Create connection
        Properties connectionProperties = new Properties();
        connectionProperties.putAll(dialect.getDefaultProperties());
        connectionProperties.putAll(configurationProperties);
        ManagedProviderConnectionHelper connectionHelper = new ManagedProviderConnectionHelper(configurationProperties);
        connectionHelper.prepare(true);
        Connection connection = connectionHelper.getConnection();

        // Generate update scripts
        Metadata metadata = new MetadataSources(
                new StandardServiceRegistryBuilder()
                        .applySetting("hibernate.dialect", dialect)
                        .applySetting("javax.persistence.schema-generation-connection", connection)
                        .build()).buildMetadata();
        new SchemaExport()
                .setFormat(true)
                .setDelimiter(";").
        String[] scripts = configuration.generateSchemaUpdateScript(dialect, new DatabaseMetadata(connection, dialect));
        if (scripts.length > 0) {
            return new NewMigration(scripts);
        }
        else {
            return null;
        }
    }

    */
/**
     * Save given {@code newMigration} to file.
     *
     * @param version      which the given {@code newMigration} represents
     * @param newMigration which should be saved
     * @throws Exception when the saving fails
     *//*

    public void saveNewMigrationToFile(int version, NewMigration newMigration) throws Exception
    {
        String filePath = sourceDirectory + "/" + this.migrationPackageName.replace(".", "/");
        String fileName = String.format("%s/Migration%d.java", filePath, getCurrentVersion());

        StringBuilder fileContent = new StringBuilder();
        fileContent.append("package cz.cesnet.shongo.controller.migration;\n");
        fileContent.append("\n");
        fileContent.append("import cz.cesnet.shongo.controller.util.DatabaseMigration;\n");
        fileContent.append("\n");
        fileContent.append("import javax.persistence.EntityManager;\n");
        fileContent.append("\n");
        fileContent.append("public class Migration");
        fileContent.append(version);
        fileContent.append(" extends DatabaseMigration.Migration\n");
        fileContent.append("{\n");
        fileContent.append("    @Override\n");
        fileContent.append("    public void migrate(EntityManager entityManager) throws Exception\n");
        fileContent.append("    {\n");
        for (String query : newMigration.getQueries()) {
            fileContent.append("        execute(\"");
            fileContent.append(query);
            fileContent.append("\", entityManager);\n");
        }
        fileContent.append("    }\n");
        fileContent.append("}\n");

        File directory = new File(filePath);
        if (directory.exists()) {
            FileWriter fileWriter = new FileWriter(fileName);
            try {
                BufferedWriter file = new BufferedWriter(fileWriter);
                try {
                    file.write(fileContent.toString());
                }
                finally {
                    file.close();
                }
            }
            finally {
                fileWriter.close();
            }
        }
    }

    */
/**
     * Represents a database migration.
     *//*

    public static abstract class Migration
    {
        */
/**
         * Execute query when migrating.
         *
         * @param query         to be executed
         * @param entityManager which should be used for executing
         *//*

        protected void execute(String query, EntityManager entityManager)
        {
            entityManager.createNativeQuery(query).executeUpdate();
        }

        */
/**
         * Perform migration.
         *
         * @param entityManager which should be used
         * @throws Exception
         *//*

        public abstract void migrate(EntityManager entityManager) throws Exception;
    }

    */
/**
     * Represents newly generated {@link Migration}.
     *//*

    public static class NewMigration extends Migration
    {
        */
/**
         * List of generated sql update queries.
         *//*

        private List<String> queries = new ArrayList<String>();

        */
/**
         * Constructor.
         *
         * @param queries sets the {@link  #queries}
         *//*

        public NewMigration(String[] queries)
        {
            for (String query : queries) {
                this.queries.add(query);
            }
        }

        */
/**
         * @return {@link #queries}
         *//*

        public List<String> getQueries()
        {
            return queries;
        }

        @Override
        public void migrate(EntityManager entityManager) throws Exception
        {
            for (String query : queries) {
                execute(query, entityManager);
            }
        }
    }

    */
/**
     * Copied from {@link org.hibernate.tool.hbm2ddl.ManagedProviderConnectionHelper}.
     *//*

    private static class ManagedProviderConnectionHelper implements ConnectionHelper
    {
        private Properties cfgProperties;
        private StandardServiceRegistryImpl serviceRegistry;
        private Connection connection;

        public ManagedProviderConnectionHelper(Properties cfgProperties)
        {
            this.cfgProperties = cfgProperties;
        }

        public void prepare(boolean needsAutoCommit) throws SQLException
        {
            serviceRegistry = createServiceRegistry(cfgProperties);
            connection = serviceRegistry.getService(ConnectionProvider.class).getConnection();
            if (needsAutoCommit && !connection.getAutoCommit()) {
                connection.commit();
                connection.setAutoCommit(true);
            }
        }

        private static StandardServiceRegistryImpl createServiceRegistry(Properties properties)
        {
            Environment.verifyProperties(properties);
            ConfigurationHelper.resolvePlaceHolders(properties);
            return (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().applySettings(properties)
                    .build();
        }

        public Connection getConnection() throws SQLException
        {
            return connection;
        }

        public void release() throws SQLException
        {
            try {
                releaseConnection();
            }
            finally {
                releaseServiceRegistry();
            }
        }

        private void releaseConnection() throws SQLException
        {
            if (connection != null) {
                try {
                    new SqlExceptionHelper().logAndClearWarnings(connection);
                }
                finally {
                    try {
                        serviceRegistry.getService(ConnectionProvider.class).closeConnection(connection);
                    }
                    finally {
                        connection = null;
                    }
                }
            }
        }

        private void releaseServiceRegistry()
        {
            if (serviceRegistry != null) {
                try {
                    serviceRegistry.destroy();
                }
                finally {
                    serviceRegistry = null;
                }
            }
        }
    }
}
*/
