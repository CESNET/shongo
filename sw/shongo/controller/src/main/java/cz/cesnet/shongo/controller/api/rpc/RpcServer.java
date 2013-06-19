package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.api.util.Options;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.AbstractRequest;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.report.*;
import cz.cesnet.shongo.util.Timer;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.ServerStreamConnection;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.XmlRpcNotAuthorizedException;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.metadata.Util;
import org.apache.xmlrpc.server.*;
import org.apache.xmlrpc.webserver.Connection;
import org.apache.xmlrpc.webserver.RequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Server for XML-RPC with improved type factory.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RpcServer extends org.apache.xmlrpc.webserver.WebServer
{
    private static Logger logger = LoggerFactory.getLogger(RpcServer.class);

    /**
     * Handler mapping, provide set of service instances
     * that will handler all XML-RPC requests.
     */
    private PropertyHandlerMapping handlerMapping;

    /**
     * Get host address by name
     *
     * @param host
     * @return host address
     */
    public static java.net.InetAddress getHostByName(String host)
    {
        if (host != null) {
            try {
                return InetAddress.getByName(host);
            }
            catch (UnknownHostException e) {
            }
        }
        return null;
    }

    /**
     * Construct XML-RPC web server
     *
     * @param host
     * @param pPort
     */
    public RpcServer(String host, int pPort)
    {
        super(pPort, getHostByName(host));

        handlerMapping = new HandlerMapping();
        handlerMapping.setTypeConverterFactory(
                new cz.cesnet.shongo.api.rpc.TypeConverterFactory(Options.SERVER));
        handlerMapping.setRequestProcessorFactoryFactory(new RequestProcessorFactory());
        handlerMapping.setVoidMethodEnabled(true);

        XmlRpcServer xmlRpcServer = getXmlRpcServer();
        xmlRpcServer.setHandlerMapping(handlerMapping);

        XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
    }

    @Override
    protected XmlRpcStreamServer newXmlRpcStreamServer()
    {
        XmlRpcStreamServer server = new ConnectionServer();
        server.setTypeFactory(new cz.cesnet.shongo.api.rpc.TypeFactory(server, Options.SERVER));
        return server;
    }

    /**
     * Set handlers from property file.
     * Each line in file should contain pair "handler_name=handler_class".
     *
     * @param mappingFile
     */
    public void setHandlerFromFile(String mappingFile)
    {
        try {
            handlerMapping.load(Thread.currentThread().getContextClassLoader(), mappingFile);
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to load handler mappings from file.", exception);
        }
    }

    /**
     * Add handler by instance
     *
     * @param name
     * @param handler
     */
    public void addHandler(String name, Object handler)
    {
        try {
            handlerMapping.addHandler(name, handler.getClass());
        }
        catch (XmlRpcException exception) {
            throw new RuntimeException("Failed to add new handler.", exception);
        }
        // Add instance to request processory factory
        RequestProcessorFactory factory = (RequestProcessorFactory) handlerMapping.getRequestProcessorFactoryFactory();
        factory.addInstance(handler);
    }

    @Override
    public void log(Throwable pError)
    {
        if (pError instanceof SocketException) {
            if (serverSocket == null) {
                return;
            }
        }
        super.log(pError);
    }

    /**
     * Stop XML-RPC server
     */
    public void stop()
    {
        shutdown();
        try {
            ServerSocket serverSocket = this.serverSocket;
            this.serverSocket = null;
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }

    }

    /**
     * Unique request identifier generator.
     */
    private static Long lastRequestId = Long.valueOf(0);

    /**
     * @return new generated request identifier
     */
    public synchronized static Long getNewRequestId()
    {
        return ++lastRequestId;
    }


    /**
     * {@link ReflectiveXmlRpcHandler} with conversion of {@link ApiFault} to {@link XmlRpcException}.
     */
    public static class Handler implements XmlRpcHandler
    {
        private static class MethodData
        {
            final Method method;
            final TypeConverter[] typeConverters;

            MethodData(Method pMethod, org.apache.xmlrpc.common.TypeConverterFactory pTypeConverterFactory)
            {
                method = pMethod;
                Class[] paramClasses = method.getParameterTypes();
                typeConverters = new TypeConverter[paramClasses.length];
                if (pTypeConverterFactory instanceof cz.cesnet.shongo.api.rpc.TypeConverterFactory) {
                    cz.cesnet.shongo.api.rpc.TypeConverterFactory typeConverterFactory =
                            (cz.cesnet.shongo.api.rpc.TypeConverterFactory) pTypeConverterFactory;
                    Type[] genericParamTypes = method.getGenericParameterTypes();
                    for (int i = 0; i < paramClasses.length; i++) {
                        typeConverters[i] = typeConverterFactory.getTypeConverter(paramClasses[i],
                                genericParamTypes[i]);
                    }
                }
                else {
                    for (int i = 0; i < paramClasses.length; i++) {
                        typeConverters[i] = pTypeConverterFactory.getTypeConverter(paramClasses[i]);
                    }
                }
            }
        }

        private final AbstractReflectiveHandlerMapping mapping;
        private final MethodData[] methods;
        private final Class clazz;
        private final RequestProcessorFactoryFactory.RequestProcessorFactory requestProcessorFactory;

        public Handler(AbstractReflectiveHandlerMapping pMapping,
                org.apache.xmlrpc.common.TypeConverterFactory pTypeConverterFactory,
                Class pClass, RequestProcessorFactoryFactory.RequestProcessorFactory pFactory, Method[] pMethods)
        {
            mapping = pMapping;
            clazz = pClass;
            methods = new MethodData[pMethods.length];
            requestProcessorFactory = pFactory;
            for (int i = 0; i < methods.length; i++) {
                methods[i] = new MethodData(pMethods[i], pTypeConverterFactory);
            }
        }

        private Object getInstance(XmlRpcRequest pRequest) throws XmlRpcException
        {
            return requestProcessorFactory.getRequestProcessor(pRequest);
        }

        public Object execute(XmlRpcRequest pRequest) throws XmlRpcException
        {
            AbstractReflectiveHandlerMapping.AuthenticationHandler authHandler = mapping.getAuthenticationHandler();
            if (authHandler != null && !authHandler.isAuthorized(pRequest)) {
                throw new XmlRpcNotAuthorizedException("Not authorized");
            }
            Object[] args = new Object[pRequest.getParameterCount()];
            for (int j = 0; j < args.length; j++) {
                args[j] = pRequest.getParameter(j);
            }
            Object instance = getInstance(pRequest);
            for (int i = 0; i < methods.length; i++) {
                MethodData methodData = methods[i];
                TypeConverter[] converters = methodData.typeConverters;
                if (args.length == converters.length) {
                    boolean matching = true;
                    for (int j = 0; j < args.length; j++) {
                        if (!converters[j].isConvertable(args[j])) {
                            matching = false;
                            break;
                        }
                    }
                    if (matching) {
                        for (int j = 0; j < args.length; j++) {
                            args[j] = converters[j].convert(args[j]);
                        }
                        return invoke(instance, methodData.method, args);
                    }
                }
            }
            throw new CommonReportSet.MethodNotDefinedException(
                    clazz.getSimpleName() + "." + methods[0].method.getName() + "(" + Util.getSignature(args) + ")");
        }

        private Object invoke(Object pInstance, Method pMethod, Object[] pArgs) throws XmlRpcException
        {
            Timer requestTimer = new Timer();
            requestTimer.start();

            // Prepare request context
            RequestContext requestContext = new RequestContext();
            requestContext.requestId = getNewRequestId();
            requestContext.methodName = ((Service)pInstance).getServiceName() + "." + pMethod.getName();
            requestContext.arguments = pArgs;
            requestContext.userInformation = null;

            // Get user information from the first SecurityToken argument
            if (pArgs.length > 0) {
                SecurityToken securityToken = null;
                if (pArgs[0] instanceof SecurityToken) {
                    securityToken = (SecurityToken) pArgs[0];
                }
                else if (pArgs[0] instanceof AbstractRequest) {
                    securityToken = ((AbstractRequest) pArgs[0]).getSecurityToken();
                }
                if (securityToken != null) {
                    try {
                        requestContext.userInformation = Authorization.getInstance().getUserInformation(securityToken);
                    }
                    catch (Exception exception) {
                        logger.warn("Get user information by access token '{}' failed.", securityToken.getAccessToken());
                    }
                }
            }

            // Log request start
            if (requestContext.userInformation != null) {
                Controller.loggerApi.info("Request:{} {} by {} (userId: {})",new Object[]{
                        requestContext.requestId, requestContext.methodName,
                        requestContext.userInformation.getFullName(), requestContext.userInformation.getUserId()});
            }
            else {
                Controller.loggerApi.info("Request:{} {}", new Object[]{
                        requestContext.requestId, requestContext.methodName});
            }

            // Execute request
            String requestState = "OK";
            try {
                pMethod.setAccessible(true);
                Object result = pMethod.invoke(pInstance, pArgs);
                return result;
            }
            catch (IllegalAccessException exception) {
                String message = "Illegal access to method " + pMethod.getName()
                        + " in class " + clazz.getName() + ".";
                requestState = "FAILED: " + message;
                throw new XmlRpcException(message, exception);
            }
            catch (IllegalArgumentException exception) {
                String message = "Illegal argument for method " + pMethod.getName()
                        + " in class " + clazz.getName() + ".";
                requestState = "FAILED: " + message;
                throw new XmlRpcException(message, exception);
            }
            catch (InvocationTargetException exception) {
                Throwable throwable = exception.getTargetException();
                if (throwable instanceof ApiFaultException) {
                    ApiFaultException apiFaultException = (ApiFaultException) throwable;
                    throw new ApiFaultXmlRpcException(requestContext, apiFaultException.getApiFault(), throwable);
                }
                else if (throwable instanceof ReportException) {
                    ReportException reportException = (ReportException) throwable;
                    throw new ReportXmlRpcException(requestContext, reportException.getReport(), throwable);
                }
                else if (throwable instanceof ReportRuntimeException) {
                    ReportRuntimeException reportRuntimeException = (ReportRuntimeException) throwable;
                    throw new ReportXmlRpcException(requestContext, reportRuntimeException.getReport(), throwable);
                }
                String message = "Failed to invoke " + ((Service) pInstance).getServiceName() + "." + pMethod.getName()
                        + ": " + throwable.getMessage();
                requestState = "FAILED: " + message;
                throw new XmlRpcException(message, exception);
            }
            finally {
                // Log request end
                long duration = requestTimer.stop();
                Controller.loggerApi.info("Request:{} Done in {} ms ({}).",
                        new Object[]{requestContext.requestId, duration, requestState});
            }
        }
    }

    /**
     * Represents attributes of XML-RPC request.
     */
    private static class RequestContext implements Reporter.ReportContext
    {
        /**
         * Unique identifier of the request.
         */
        private Long requestId;

        /**
         * Name of the method which is invoked by the request (e.g., "service.method")
         */
        private String methodName;

        /**
         * Method arguments.
         */
        private Object[] arguments;

        /**
         * {@link UserInformation} of user which requested the method.
         */
        private UserInformation userInformation;

        @Override
        public String getReportContextName()
        {
            if (userInformation != null) {
                return String.format("%s by %s",
                        methodName, userInformation.getFullName());
            }
            else {
                return String.format("%s", methodName);
            }
        }

        @Override
        public String getReportContextDetail()
        {
            StringBuilder reportDetail = new StringBuilder();
            reportDetail.append("REQUEST\n\n");

            reportDetail.append("      ID: ");
            reportDetail.append(requestId);
            reportDetail.append("\n");

            reportDetail.append("    User: ");
            reportDetail.append(userInformation.getFullName());
            reportDetail.append("(id: ");
            reportDetail.append(userInformation.getUserId());
            reportDetail.append(")");
            reportDetail.append("\n");

            reportDetail.append("  Method: ");
            reportDetail.append(methodName);
            reportDetail.append("\n");

            reportDetail.append("  Arguments:\n");
            for (Object argument : arguments) {
                reportDetail.append("   * "); reportDetail.append(argument.toString()); reportDetail.append("\n");
            }
            return reportDetail.toString();
        }
    }

    /**
     * Method invocation throws exception which is {@link ApiFault}.
     */
    private static class ApiFaultXmlRpcException extends XmlRpcException
    {
        private RequestContext requestContext;

        private ApiFault apiFault;

        public ApiFaultXmlRpcException(RequestContext requestContext, ApiFault apiFault, Throwable throwable)
        {
            super(null, throwable);
            this.requestContext = requestContext;
            this.apiFault = apiFault;
        }

        public RequestContext getRequestContext()
        {
            return requestContext;
        }

        public ApiFault getApiFault()
        {
            return apiFault;
        }
    }

    /**
     * Method invocation throws exception with {@link Report} which isn't {@link ApiFault}.
     */
    private static class ReportXmlRpcException extends XmlRpcException
    {
        private RequestContext requestContext;

        private Report report;

        public ReportXmlRpcException(RequestContext requestContext, Report report, Throwable throwable)
        {
            super(null, throwable);
            this.requestContext = requestContext;
            this.report = report;
        }

        public RequestContext getRequestContext()
        {
            return requestContext;
        }

        public Report getReport()
        {
            return report;
        }
    }

    /**
     * Handler mapping that is able to translate full class name (which extends {@link Service})
     * to added handler name.
     * <p/>
     * It is required to be able to do: <p/>
     * <pre>
     * ClientFactory factory = new ClientFactory(client);
     * FooService service = (FooService) factory.newInstance(FooService.class);
     * </pre>
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class HandlerMapping extends PropertyHandlerMapping
    {
        /**
         * Map of handler classes by name.
         */
        Map<String, Class> handlerClassMap = new HashMap<String, Class>();

        @Override
        protected boolean isHandlerMethod(Method pMethod)
        {
            boolean result = super.isHandlerMethod(pMethod);
            if (result) {
                Service.API api = AnnotationUtils.findAnnotation(pMethod, Service.API.class);
                return api != null;
            }
            return false;
        }

        @Override
        public void addHandler(String pKey, Class pClass) throws XmlRpcException
        {
            handlerClassMap.put(pKey, pClass);
            super.addHandler(pKey, pClass);
        }

        @Override
        @SuppressWarnings("unchecked")
        public XmlRpcHandler getHandler(String pHandlerName) throws XmlRpcException
        {
            XmlRpcHandler result = (XmlRpcHandler) handlerMap.get(pHandlerName);
            if (result == null) {
                Exception exception = null;
                try {
                    // When no handler is found try to find it by requested class
                    int pos = pHandlerName.lastIndexOf(".");
                    if (pos != -1) {
                        String requestedClassName = pHandlerName.substring(0, pos);
                        String requestedMethod = pHandlerName.substring(pos + 1, pHandlerName.length());
                        Class requestedClass = Class.forName(requestedClassName);
                        // Lookup only when requested class extends Service
                        if (Service.class.isAssignableFrom(requestedClass)) {
                            // Find handler name for requested class
                            for (String name : handlerClassMap.keySet()) {
                                Class possibleClass = handlerClassMap.get(name);
                                if (requestedClass.isAssignableFrom(possibleClass)) {
                                    // Get proper handler
                                    pHandlerName = String.format("%s.%s", name, requestedMethod);
                                    result = (XmlRpcHandler) handlerMap.get(pHandlerName);
                                    if (result != null) {
                                        // Cache the handler to not perform the lookup again
                                        handlerMap.put(pHandlerName, result);
                                        return result;
                                    }
                                }
                            }
                        }
                    }
                }
                catch (Exception e) {
                    exception = e;
                }
                throw new CommonReportSet.MethodNotDefinedException(exception, pHandlerName);
            }
            return result;
        }

        /**
         * Creates a new instance of {@link XmlRpcHandler}.
         *
         * @param pClass   The class, which was inspected for handler
         *                 methods. This is used for error messages only. Typically,
         *                 it is the same than <pre>pInstance.getClass()</pre>.
         * @param pMethods The method being invoked.
         */
        @Override
        protected XmlRpcHandler newXmlRpcHandler(final Class pClass,
                final Method[] pMethods) throws XmlRpcException
        {
            RequestProcessorFactoryFactory.RequestProcessorFactory factory = getRequestProcessorFactoryFactory()
                    .getRequestProcessorFactory(pClass);
            return new Handler(this, getTypeConverterFactory(),
                    pClass, factory, pMethods);
        }
    }

    /**
     * We need to override request processor factory which creates instances of handler objects.
     * Default implementation create new instance for each request which is not desireable for
     * our purposes and this class overrides this mechanism to return existing instances
     * for specified class.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class RequestProcessorFactory
            extends RequestProcessorFactoryFactory.RequestSpecificProcessorFactoryFactory
    {
        private Map<Class, Object> instances = new HashMap<Class, Object>();

        public void addInstance(Object object)
        {
            instances.put(object.getClass(), object);
        }

        @Override
        protected Object getRequestProcessor(Class pClass, XmlRpcRequest pRequest) throws XmlRpcException
        {
            if (instances.containsKey(pClass)) {
                return instances.get(pClass);
            }
            return super.getRequestProcessor(pClass, pRequest);
        }
    }

    /**
     * Connection server. Copied default implementation which overrides:
     * 1) convertThrowable method to allow use of cause from runtime exception
     * as fault.
     * 2) getRequest and writeResponse for logging of XML-RPC requests and response XMLs.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class ConnectionServer extends XmlRpcHttpServer
    {
        @Override
        protected Throwable convertThrowable(Throwable throwable)
        {
            ApiFault apiFault = null;

            // Report not API fault
            if (throwable instanceof ReportXmlRpcException) {
                ReportXmlRpcException reportXmlRpcException = (ReportXmlRpcException) throwable;
                Report report = reportXmlRpcException.getReport();
                apiFault = new CommonReportSet.UnknownErrorReport(report.getMessage());
                throwable = reportXmlRpcException.getCause();

                Reporter.report(reportXmlRpcException.getRequestContext(), report, throwable);
            }
            // Report API fault
            else {
                RequestContext requestContext = null;
                if (throwable instanceof ApiFaultXmlRpcException) {
                    ApiFaultXmlRpcException apiFaultXmlRpcException = (ApiFaultXmlRpcException) throwable;
                    apiFault = apiFaultXmlRpcException.getApiFault();
                    throwable = apiFaultXmlRpcException.getCause();
                    requestContext = apiFaultXmlRpcException.getRequestContext();
                }
                else if (throwable instanceof ApiFaultException) {
                    ApiFaultException apiFaultException = (ApiFaultException) throwable;
                    apiFault = apiFaultException.getApiFault();

                }
                else if (throwable instanceof RuntimeException || throwable instanceof SAXException) {
                    Throwable cause = throwable.getCause();
                    if (cause instanceof ApiFaultXmlRpcException) {
                        ApiFaultXmlRpcException apiFaultXmlRpcException = (ApiFaultXmlRpcException) cause;
                        apiFault = apiFaultXmlRpcException.getApiFault();
                        throwable = apiFaultXmlRpcException.getCause();
                    }
                    else if (cause instanceof ApiFaultException) {
                        ApiFaultException apiFaultException = (ApiFaultException) cause;
                        apiFault = apiFaultException.getApiFault();
                        throwable = cause;
                    }
                }
                if (apiFault == null) {
                    apiFault = new CommonReportSet.UnknownErrorReport(throwable.getMessage());
                }

                Reporter.reportApiFault(requestContext, apiFault, throwable);
            }

            ApiFaultString apiFaultString = new ApiFaultString();
            apiFaultString.setMessage(apiFault.getFaultString());
            apiFault.writeParameters(apiFaultString);
            XmlRpcException xmlRpcException = new XmlRpcException(apiFault.getFaultCode(), apiFaultString.toString(),
                    throwable.getCause());
            xmlRpcException.setStackTrace(throwable.getStackTrace());
            return xmlRpcException;
        }

        @Override
        protected XmlRpcRequest getRequest(XmlRpcStreamRequestConfig pConfig, InputStream pStream)
                throws XmlRpcException
        {
            if (RpcServerRequestLogger.isEnabled()) {
                pStream = RpcServerRequestLogger.logRequest(pStream);
            }
            return super.getRequest(pConfig, pStream);
        }

        @Override
        protected void writeResponse(XmlRpcStreamRequestConfig pConfig, OutputStream pStream, Object pResult)
                throws XmlRpcException
        {
            if (RpcServerRequestLogger.isEnabled()) {
                pStream = RpcServerRequestLogger.logResponse(pStream);
            }
            RequestData data = (RequestData) pConfig;
            try {
                if (data.isByteArrayRequired()) {
                    super.writeResponse(pConfig, pStream, pResult);
                    data.getConnection().writeResponse(data, pStream);
                }
                else {
                    data.getConnection().writeResponseHeader(data, -1);
                    super.writeResponse(pConfig, pStream, pResult);
                    pStream.flush();
                }
            }
            catch (IOException e) {
                throw new XmlRpcException(e.getMessage(), e);
            }
        }

        @Override
        protected void writeError(XmlRpcStreamRequestConfig pConfig, OutputStream pStream,
                Throwable pError) throws XmlRpcException
        {
            if (RpcServerRequestLogger.isEnabled()) {
                pStream = RpcServerRequestLogger.logResponse(pStream);
            }
            RequestData data = (RequestData) pConfig;
            try {
                if (data.isByteArrayRequired()) {
                    super.writeError(pConfig, pStream, pError);
                    data.getConnection().writeError(data, pError, (ByteArrayOutputStream) pStream);
                }
                else {
                    data.getConnection().writeErrorHeader(data, pError, -1);
                    super.writeError(pConfig, pStream, pError);
                    pStream.flush();
                }
            }
            catch (IOException e) {
                throw new XmlRpcException(e.getMessage(), e);
            }
        }

        @Override
        protected void setResponseHeader(ServerStreamConnection pConnection, String pHeader, String pValue)
        {
            ((Connection) pConnection).setResponseHeader(pHeader, pValue);
        }

        @Override
        public void execute(XmlRpcStreamRequestConfig pConfig, ServerStreamConnection pConnection)
                throws XmlRpcException
        {
            super.execute(pConfig, pConnection);
        }

        @Override
        protected void logError(Throwable t)
        {
        }
    }
}
