package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.CommonFaultSet;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.AclEntityState;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.AclUserState;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.OwnedPersistentObject;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
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

import javax.persistence.EntityManager;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Provides methods for performing authentication and authorization.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ServerAuthorization extends Authorization
{
    private static Logger logger = LoggerFactory.getLogger(ServerAuthorization.class);

    /**
     * User web service.
     */
    private static final String USER_WEB_SERVICE = "https://hroch.cesnet.cz/perun-ws/resource/user";

    /**
     * Access token which won't be verified and can be used for testing purposes.
     */
    private String testingAccessToken;

    /**
     * URL to authorization server.
     */
    private String authorizationServer;

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
    }

    /**
     * @param testingAccessToken sets the {@link #testingAccessToken}
     */
    public void setTestingAccessToken(String testingAccessToken)
    {
        this.testingAccessToken = testingAccessToken;
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

    @Override
    protected UserInformation onGetUserInformationByAccessToken(String accessToken) throws FaultException
    {
        // Testing security token represents root user
        if (testingAccessToken != null && accessToken.equals(testingAccessToken)) {
            return ROOT_USER_INFORMATION;
        }

        Map<String, Object> content = null;
        try {
            // Build url
            URIBuilder uriBuilder = new URIBuilder(authorizationServer + "userinfo");
            uriBuilder.setParameter("schema", "openid");
            String url = uriBuilder.build().toString();

            // Perform request
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", "Bearer " + accessToken);
            HttpClient httpClient = ConfiguredSSLContext.getInstance().createHttpClient();
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                ObjectMapper mapper = new ObjectMapper();
                content = mapper.readValue(inputStream, Map.class);
                inputStream.close();
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                String error = "Error";
                if (content != null) {
                    error = String.format("Error: %s. %s", content.get("error"), content.get("error_description"));
                }
                throw new Exception(error);
            }
        }
        catch (Exception exception) {
            CommonFaultSet.throwSecurityErrorFault(
                    String.format("Retrieving user information for access token failed. %s", exception.getMessage()));
        }
        return createUserInformationFromData(content);
    }

    @Override
    protected UserInformation onGetUserInformationByUserId(String userId) throws FaultException
    {
        Map<String, Object> content = null;
        try {
            // Build url
            URIBuilder uriBuilder = new URIBuilder(USER_WEB_SERVICE + "/" + userId);
            String url = uriBuilder.build().toString();

            // Perform request
            HttpGet httpGet = new HttpGet(url);
            HttpClient httpClient = ConfiguredSSLContext.getInstance().createHttpClient();
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                ObjectMapper mapper = new ObjectMapper();
                content = mapper.readValue(inputStream, Map.class);
                inputStream.close();
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new Exception("Error while retrieving user information by id.");
            }
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return createUserInformationFromData(content);
    }

    @Override
    protected Collection<UserInformation> onListUserInformation() throws FaultException
    {
        List content = null;
        try {
            // Build url
            URIBuilder uriBuilder = new URIBuilder(USER_WEB_SERVICE);
            String url = uriBuilder.build().toString();

            // Perform request
            HttpGet httpGet = new HttpGet(url);
            HttpClient httpClient = ConfiguredSSLContext.getInstance().createHttpClient();
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                ObjectMapper mapper = new ObjectMapper();
                content = mapper.readValue(inputStream, List.class);
                inputStream.close();
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new Exception("Error while retrieving user information.");
            }
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        List<UserInformation> userInformationList = new LinkedList<UserInformation>();
        if (content != null) {
            for (Object object : content) {
                if (object instanceof Map) {
                    UserInformation userInformation = createUserInformationFromData((Map) object);
                    userInformationList.add(userInformation);
                }
            }
        }
        return userInformationList;
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
