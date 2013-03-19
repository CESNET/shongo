package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.CommonFaultSet;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

/**
 * Provides methods for performing authentication and authorization.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ServerAuthorization extends Authorization
{
    private static Logger logger = LoggerFactory.getLogger(ServerAuthorization.class);

    /**
     * Authentication service path in auth-server.
     */
    private static final String AUTHENTICATION_SERVICE_PATH = "/authn/oic";

    /**
     * Authorization service path in auth-server.
     */
    private static final String AUTHORIZATION_SERVICE_PATH = "/authz/rest";

    /**
     * User web service path in auth-server.
     */
    private static final String USER_SERVICE_PATH = "/perun/resource";

    /**
     * Access token which won't be verified and can be used for testing purposes.
     */
    private String testingAccessToken;

    /**
     * URL to authorization server.
     */
    private String authorizationServer;

    /**
     * {@link HttpClient} for performing auth-server requests.
     */
    private HttpClient httpClient;

    /**
     * Constructor.
     *
     * @param configuration to load authorization configuration from
     */
    private ServerAuthorization(Configuration configuration)
    {
        super(configuration);
        authorizationServer = configuration.getString(Configuration.SECURITY_AUTHORIZATION_SERVER);
        if (authorizationServer == null) {
            throw new IllegalStateException("Authorization server is not set in the configuration.");
        }
        testingAccessToken = configuration.getString(Configuration.SECURITY_TESTING_ACCESS_TOKEN);

        // Create http client
        httpClient = ConfiguredSSLContext.getInstance().createHttpClient();
    }

    /**
     * @param testingAccessToken sets the {@link #testingAccessToken}
     */
    public void setTestingAccessToken(String testingAccessToken)
    {
        this.testingAccessToken = testingAccessToken;
    }

    /**
     * @return url to authentication service in auth-server
     */
    private String getAuthenticationUrl()
    {
        return authorizationServer + AUTHENTICATION_SERVICE_PATH;
    }

    /**
     * @return url to authorization service in auth-server
     */
    private String getAuthorizationUrl()
    {
        return authorizationServer + AUTHORIZATION_SERVICE_PATH;
    }

    /**
     * @return url to user service in auth-server
     */
    private String getUserServiceUrl()
    {
        return authorizationServer + USER_SERVICE_PATH;
    }

    @Override
    protected String onValidate(SecurityToken securityToken) throws FaultException
    {
        // Always allow testing access token
        if (testingAccessToken != null && securityToken.getAccessToken().equals(testingAccessToken)) {
            logger.debug("Access token '{}' is valid for testing.", securityToken.getAccessToken());
            return ROOT_USER_ID;
        }
        return super.onValidate(securityToken);
    }

    private <T> T performRequest(URI uri, Map<String, String> headers, Class<T> resultType) throws Exception
    {
        try {
            // Prepare request
            HttpGet httpGet = new HttpGet(uri);
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    httpGet.setHeader(header.getKey(), header.getValue());
                }
            }

            logger.debug("Performing request {}...", uri);

            // Execute request and get response
            HttpResponse response = httpClient.execute(httpGet);
            int responseStatus = response.getStatusLine().getStatusCode();
            HttpEntity responseEntity = response.getEntity();

            // Handle success
            if (responseStatus == HttpStatus.SC_OK) {
                InputStream inputStream = responseEntity.getContent();
                ObjectMapper mapper = new ObjectMapper();
                T result = mapper.readValue(inputStream, resultType);
                inputStream.close();
                return result;
            }
            // Handle error
            else {
                InputStream inputStream = responseEntity.getContent();
                String error;
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map result = mapper.readValue(inputStream, Map.class);
                    inputStream.close();
                    error = String.format("Error: %s. %s", result.get("error"), result.get("error_description"));
                }
                catch (Exception exception) {
                    Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
                    error = scanner.hasNext() ? scanner.next() : "";
                    error = String.format("HTTP Error %d%s%s", responseStatus, (error.isEmpty() ? "" : ": "), error);
                }
                throw new Exception(error);
            }
        }
        catch (Exception exception) {
            throw  new Exception(String.format("Authorization request '%s' failed: %s",
                    uri.toString(), exception.getMessage()), exception);
        }
    }

    @Override
    protected UserInformation onGetUserInformationByAccessToken(String accessToken) throws FaultException
    {
        // Testing security token represents root user
        if (testingAccessToken != null && accessToken.equals(testingAccessToken)) {
            return ROOT_USER_INFORMATION;
        }

        try {
            URIBuilder uriBuilder = new URIBuilder(getAuthenticationUrl() + "/userinfo");
            uriBuilder.setParameter("schema", "openid");

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "Bearer " + accessToken);

            Map<String, Object> result = performRequest(uriBuilder.build(), headers, Map.class);
            return createUserInformationFromData(result);
        }
        catch (Exception exception) {
            throw new FaultException(exception, CommonFaultSet.createSecurityErrorFault(
                    String.format("Retrieving user information by access token failed: %s", exception.getMessage())));
        }
    }

    @Override
    protected UserInformation onGetUserInformationByUserId(String userId) throws FaultException
    {
        Map<String, Object> content = null;
        try {
            // Build url
            URIBuilder uriBuilder = new URIBuilder(getUserServiceUrl() + "/user/" + userId);

            Map<String, Object> result = performRequest(uriBuilder.build(), null, Map.class);
            return createUserInformationFromData(result);
        }

        catch (Exception exception) {
            throw new FaultException(exception, CommonFaultSet.createSecurityErrorFault(
                    String.format("Retrieving user information by user-id failed: %s", exception.getMessage())));
        }
    }

    @Override
    protected Collection<UserInformation> onListUserInformation() throws FaultException
    {
        try {
            // Build url
            URIBuilder uriBuilder = new URIBuilder(getUserServiceUrl() + "/user");

            List result = performRequest(uriBuilder.build(), null, List.class);
            List<UserInformation> userInformationList = new LinkedList<UserInformation>();
            if (result != null) {
                for (Object object : result) {
                    if (object instanceof Map) {
                        UserInformation userInformation = createUserInformationFromData((Map) object);
                        userInformationList.add(userInformation);
                    }
                }
            }
            return userInformationList;
        }
        catch (Exception exception) {
            return CommonFaultSet.throwSecurityErrorFault(
                    String.format("Retrieving user information failed. %s", exception.getMessage()));
        }
    }

    @Override
    protected AclRecord onCreateAclRecord(String userId, EntityIdentifier entityId, Role role) throws FaultException
    {
        // TODO: create ACL in authorization server

        return new AclRecord(userId, entityId, role);
    }

    @Override
    protected void onDeleteAclRecord(AclRecord aclRecord) throws FaultException
    {
        // TODO: delete ACL in authorization server
    }

    @Override
    protected AclRecord onGetAclRecord(String aclRecordId) throws FaultException
    {
        // TODO: get ACL from authorization server

        return CommonFaultSet.throwSecurityErrorFault("ACL not found.");
    }

    @Override
    protected Collection<AclRecord> onListAclRecords(String userId, EntityIdentifier entityId, Role role) throws FaultException
    {
        // ToDO: get ACL from authorization server

        List<AclRecord> aclRecords = new LinkedList<AclRecord>();
        for (AclRecord aclRecord : cache.getAclRecords()) {
            if (userId != null && !userId.equals(aclRecord.getUserId())) {
                continue;
            }
            if (entityId != null && !entityId.equals(aclRecord.getEntityId())) {
                continue;
            }
            if (role != null && !role.equals(aclRecord.getRole())) {
                continue;
            }
            aclRecords.add(aclRecord);
        }
        return aclRecords;
    }

    /**
     * @param data from authorization server
     * @return {@link UserInformation}
     */
    private static UserInformation createUserInformationFromData(Map<String, Object> data)
    {
        if (!data.containsKey("id")) {
            throw new IllegalStateException("User information must contains identifier.");
        }
        if (!data.containsKey("given_name") || !data.containsKey("family_name")) {
            throw new IllegalStateException("User information must contains given and family name.");
        }
        UserInformation userInformation = new UserInformation();
        userInformation.setUserId((String) data.get("id"));
        userInformation.setFirstName((String) data.get("given_name"));
        userInformation.setLastName((String) data.get("family_name"));

        if (data.containsKey("original_id")) {
            userInformation.setEduPersonPrincipalName((String) data.get("original_id"));
        }
        if (data.containsKey("organization")) {
            userInformation.setOrganization((String) data.get("organization"));
        }
        if (data.containsKey("email")) {
            String emails = (String) data.get("email");
            if (emails != null) {
                for (String email : emails.split(";")) {
                    if (!email.isEmpty()) {
                        userInformation.addEmail(email);
                    }
                }
            }
        }

        return userInformation;
    }

    /**
     * @return new instance of {@link ServerAuthorization}
     * @throws IllegalStateException when other {@link Authorization} already exists
     */
    public static ServerAuthorization createInstance(Configuration configuration) throws IllegalStateException
    {
        ServerAuthorization serverAuthorization = new ServerAuthorization(configuration);
        Authorization.setInstance(serverAuthorization);
        return serverAuthorization;
    }
}
