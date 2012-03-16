
package cz.cesnet.shongo.rpctest.soapws.service;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the cz.cesnet.shongo.rpctest.soapws.service package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _DivResponse_QNAME = new QName("http://soapws.rpctest.shongo.cesnet.cz/", "divResponse");
    private final static QName _GetResource_QNAME = new QName("http://soapws.rpctest.shongo.cesnet.cz/", "getResource");
    private final static QName _Div_QNAME = new QName("http://soapws.rpctest.shongo.cesnet.cz/", "div");
    private final static QName _Add_QNAME = new QName("http://soapws.rpctest.shongo.cesnet.cz/", "add");
    private final static QName _GetResourceResponse_QNAME = new QName("http://soapws.rpctest.shongo.cesnet.cz/", "getResourceResponse");
    private final static QName _AddResponse_QNAME = new QName("http://soapws.rpctest.shongo.cesnet.cz/", "addResponse");
    private final static QName _GetMessageResponse_QNAME = new QName("http://soapws.rpctest.shongo.cesnet.cz/", "getMessageResponse");
    private final static QName _ApiException_QNAME = new QName("http://soapws.rpctest.shongo.cesnet.cz/", "ApiException");
    private final static QName _GetMessage_QNAME = new QName("http://soapws.rpctest.shongo.cesnet.cz/", "getMessage");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: cz.cesnet.shongo.rpctest.soapws.service
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetResource }
     * 
     */
    public GetResource createGetResource() {
        return new GetResource();
    }

    /**
     * Create an instance of {@link GetResourceResponse }
     * 
     */
    public GetResourceResponse createGetResourceResponse() {
        return new GetResourceResponse();
    }

    /**
     * Create an instance of {@link DivResponse }
     * 
     */
    public DivResponse createDivResponse() {
        return new DivResponse();
    }

    /**
     * Create an instance of {@link GetMessage }
     * 
     */
    public GetMessage createGetMessage() {
        return new GetMessage();
    }

    /**
     * Create an instance of {@link Add }
     * 
     */
    public Add createAdd() {
        return new Add();
    }

    /**
     * Create an instance of {@link GetMessageResponse }
     * 
     */
    public GetMessageResponse createGetMessageResponse() {
        return new GetMessageResponse();
    }

    /**
     * Create an instance of {@link ApiException }
     * 
     */
    public ApiException createApiException() {
        return new ApiException();
    }

    /**
     * Create an instance of {@link Div }
     * 
     */
    public Div createDiv() {
        return new Div();
    }

    /**
     * Create an instance of {@link Resource }
     * 
     */
    public Resource createResource() {
        return new Resource();
    }

    /**
     * Create an instance of {@link AddResponse }
     * 
     */
    public AddResponse createAddResponse() {
        return new AddResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DivResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soapws.rpctest.shongo.cesnet.cz/", name = "divResponse")
    public JAXBElement<DivResponse> createDivResponse(DivResponse value) {
        return new JAXBElement<DivResponse>(_DivResponse_QNAME, DivResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetResource }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soapws.rpctest.shongo.cesnet.cz/", name = "getResource")
    public JAXBElement<GetResource> createGetResource(GetResource value) {
        return new JAXBElement<GetResource>(_GetResource_QNAME, GetResource.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Div }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soapws.rpctest.shongo.cesnet.cz/", name = "div")
    public JAXBElement<Div> createDiv(Div value) {
        return new JAXBElement<Div>(_Div_QNAME, Div.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Add }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soapws.rpctest.shongo.cesnet.cz/", name = "add")
    public JAXBElement<Add> createAdd(Add value) {
        return new JAXBElement<Add>(_Add_QNAME, Add.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetResourceResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soapws.rpctest.shongo.cesnet.cz/", name = "getResourceResponse")
    public JAXBElement<GetResourceResponse> createGetResourceResponse(GetResourceResponse value) {
        return new JAXBElement<GetResourceResponse>(_GetResourceResponse_QNAME, GetResourceResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soapws.rpctest.shongo.cesnet.cz/", name = "addResponse")
    public JAXBElement<AddResponse> createAddResponse(AddResponse value) {
        return new JAXBElement<AddResponse>(_AddResponse_QNAME, AddResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetMessageResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soapws.rpctest.shongo.cesnet.cz/", name = "getMessageResponse")
    public JAXBElement<GetMessageResponse> createGetMessageResponse(GetMessageResponse value) {
        return new JAXBElement<GetMessageResponse>(_GetMessageResponse_QNAME, GetMessageResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ApiException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soapws.rpctest.shongo.cesnet.cz/", name = "ApiException")
    public JAXBElement<ApiException> createApiException(ApiException value) {
        return new JAXBElement<ApiException>(_ApiException_QNAME, ApiException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetMessage }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soapws.rpctest.shongo.cesnet.cz/", name = "getMessage")
    public JAXBElement<GetMessage> createGetMessage(GetMessage value) {
        return new JAXBElement<GetMessage>(_GetMessage_QNAME, GetMessage.class, null, value);
    }

}
