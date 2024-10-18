package cz.cesnet.shongo.controller.util;

import cz.cesnet.shongo.TodoImplementException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for native queries from resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NativeQuery
{
    private static Logger logger = LoggerFactory.getLogger(NativeQuery.class);

    /**
     * Native queries.
     */
    public static final String INIT = "init.sql";
    public static final String RESOURCE_LIST = "resource_list.sql";
    public static final String DOMAIN_RESOURCE_LIST = "domain_resource_list.sql";
    public static final String RESERVATION_REQUEST_LIST = "reservation_request_list.sql";
    public static final String RESERVATION_REQUEST_HISTORY = "reservation_request_history.sql";
    public static final String RESERVATION_LIST = "reservation_list.sql";
    public static final String EXECUTABLE_LIST = "executable_list.sql";
    public static final String ACL_ENTRY_LIST = "acl_entry_list.sql";
    public static final String REFERENCED_USER_LIST = "referenced_user_list.sql";
    public static final String MODIFY_USER_ID = "modify_user_id.sql";
    /**
     * Update queries for materialized views
     */
    public static final String EXECUTABLE_SUMMARY_DELETE = "executable_summary_delete.sql";
    public static final String EXECUTABLE_SUMMARY_INSERT = "executable_summary_insert.sql";
    public static final String SPECIFICATION_SUMMARY_DELETE = "specification_summary_delete.sql";
    public static final String SPECIFICATION_SUMMARY_INSERT = "specification_summary_insert.sql";

    public static final String EXECUTABLE_SUMMARY_CHECK = "executable_summary_check.sql";
    public static final String SPECIFICATION_SUMMARY_CHECK = "specification_summary_check.sql";

    /**
     * Cached native queries (not targeted for any specific {@link EntityManagerFactory}).
     */
    private static Map<String, String> cachedNativeQuery = new HashMap<String, String>();

    /**
     * Cached native queries for specific {@link EntityManagerFactory}.
     */
    private static Map<EntityManagerFactory, Map<String, String>> cachedNativeQueryByEntityManagerFactory =
            new HashMap<EntityManagerFactory, Map<String, String>>();

    /**
     * @param fileName
     * @return native query
     */
    public static synchronized String getNativeQuery(String fileName)
    {
        String nativeQuery = cachedNativeQuery.get(fileName);
        if (nativeQuery != null) {
            return nativeQuery;
        }
        nativeQuery = loadNativeQuery("sql/" + fileName);
        cachedNativeQuery.put(fileName, nativeQuery);
        return nativeQuery;
    }

    /**
     * @param fileName
     * @param parameters
     * @return native query
     */
    public static String getNativeQuery(String fileName, Map<String, String> parameters)
    {
        String nativeQuery = getNativeQuery(fileName);
        nativeQuery = applyParameters(nativeQuery, parameters);
        return nativeQuery;
    }

    /**
     * @param entityManagerFactory
     * @param fileName
     * @return native query
     */
    public static synchronized String getNativeQuery(EntityManagerFactory entityManagerFactory, String fileName)
    {
        // Try to read SQL from cache
        Map<String, String> cachedNativeQueryForEntityManagerFactory =
                cachedNativeQueryByEntityManagerFactory.get(entityManagerFactory);
        if (cachedNativeQueryForEntityManagerFactory == null) {
            cachedNativeQueryForEntityManagerFactory = new HashMap<String, String>();
            cachedNativeQueryByEntityManagerFactory.put(entityManagerFactory, cachedNativeQueryForEntityManagerFactory);
        }
        String nativeQuery = cachedNativeQueryForEntityManagerFactory.get(fileName);
        if (nativeQuery != null) {
            return nativeQuery;
        }

        // Determine SQL directory
        if (!(entityManagerFactory instanceof SessionFactoryImplementor)) {
            throw new TodoImplementException(entityManagerFactory.getClass());
        }

        SessionFactoryImplementor sessionFactoryImpl = (SessionFactoryImplementor) entityManagerFactory;
        Dialect dialect = sessionFactoryImpl.getJdbcServices().getDialect();;
        String directory;
        if (dialect instanceof HSQLDialect) {
            directory = "hsqldb";
        }
        else if (dialect instanceof PostgreSQLDialect) {
            directory = "postgresql";
        }
        else {
            throw new TodoImplementException(dialect.getClass());
        }

        nativeQuery = loadNativeQuery("sql/" + directory + "/" + fileName);

        // Add SQL to cache
        cachedNativeQueryForEntityManagerFactory.put(fileName, nativeQuery);
        return nativeQuery;
    }

    /**
     * @param entityManagerFactory
     * @param fileName
     * @return native query
     */
    public static String getNativeQuery(EntityManagerFactory entityManagerFactory, String fileName,
            Map<String, String> parameters)
    {
        String nativeQuery = getNativeQuery(entityManagerFactory, fileName);
        nativeQuery = applyParameters(nativeQuery, parameters);
        return nativeQuery;
    }

    /**
     * @param nativeQuery
     * @param parameters
     * @return given {@code nativeQuery} with applied {@code parameters}
     */
    private static String applyParameters(String nativeQuery, Map<String, String> parameters)
    {
        for(Map.Entry<String, String> entry : parameters.entrySet()) {
            nativeQuery = nativeQuery.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return nativeQuery;
    }

    /**
     * Load given {@code nativeQuery}.
     *
     * @param fileName
     * @return native query
     */
    private static String loadNativeQuery(String fileName)
    {
        logger.debug("Loading native SQL {}...", fileName);

        // Read SQL from proper file
        InputStream inputStream = NativeQuery.class.getClassLoader().getResourceAsStream(fileName);
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder sqlBuilder = new StringBuilder();
            try {
                String line = bufferedReader.readLine();
                while (line != null) {
                    line = line.replaceAll("/\\*.*?\\*/", "");
                    line = line.replaceAll("\\s*/\\*.*", "");
                    line = line.replaceAll("^\\s*\\*.*", "");
                    if (!line.isEmpty()) {
                        if (line.charAt(0) == 65279) {
                            line = line.substring(1);
                        }
                        if (!line.isEmpty()) {
                            sqlBuilder.append(line);
                            sqlBuilder.append("\n");
                        }
                    }
                    line = bufferedReader.readLine();
                }
            }
            finally {
                bufferedReader.close();
                inputStream.close();
            }
            return sqlBuilder.toString();
        }
        catch (IOException exception) {
            throw new RuntimeException("Failed to load native SQL " + fileName + ".", exception);
        }
    }

    /**
     * Execute update query (or multiple queries delimited by ";").
     *
     * @param entityManager
     * @param nativeQuery
     */
    public static void executeNativeUpdate(EntityManager entityManager, String nativeQuery)
    {
        entityManager.getTransaction().begin();

        List<String> statements = new LinkedList<String>();
        Pattern pattern = Pattern.compile("(;|\\$\\$)");
        Matcher matcher = pattern.matcher(nativeQuery);
        int index = 0;
        StringBuilder longStatement = null;
        boolean longStatementActive = false;
        while (matcher.find(index)) {
            int start = matcher.start();
            int end = matcher.end();
            String statement = nativeQuery.substring(index, start);
            statement = statement.replace(":", "\\:");
            String separator = nativeQuery.substring(start, end);
            if (longStatement != null) {
                longStatement.append(statement);
                if (separator.equals("$$")) {
                    longStatement.append(separator);
                    longStatementActive = false;
                }
                else if (longStatementActive) {
                    longStatement.append(separator);
                }
                else {
                    statements.add(longStatement.toString());
                    longStatement = null;
                }
            }
            else if (separator.equals("$$")) {
                longStatement = new StringBuilder();
                longStatement.append(statement);
                longStatement.append(separator);
                longStatementActive = true;
            }
            else {
                statements.add(statement);
            }
            index = end;
        }
        String lastStatement = nativeQuery.substring(index, nativeQuery.length()).trim();
        if (!lastStatement.isEmpty()) {
            statements.add(lastStatement);
        }

        for (String statement : statements) {
            statement = statement.trim();
            if (statement.isEmpty()) {
                continue;
            }
            if (statement.startsWith("SELECT")) {
                Object result = entityManager.createNativeQuery(statement).getSingleResult();
                if (result instanceof String) {
                    String message = (String) result;
                    message = message.replace("\\:", ":");
                    logger.info(message);
                }
            }
            else {
                entityManager.createNativeQuery(statement).executeUpdate();
            }
        }
        entityManager.getTransaction().commit();
    }
}
