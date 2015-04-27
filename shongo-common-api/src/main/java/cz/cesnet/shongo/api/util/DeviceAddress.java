package cz.cesnet.shongo.api.util;

import jade.content.Concept;

/**
 * A utility class for holding an address of a device together with port specification.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DeviceAddress implements Concept
{
    private final boolean ssl;
    private final String host;
    private int port;

    /**
     * A special value for the port field telling that the default port should be used.
     */
    public static final int DEFAULT_PORT = 0;

    /**
     * Parses an address (host and possibly a port) from the given input string.
     * <p/>
     * The part of the input string after the last colon is interpreted as a port, everything else as host.
     * The port might not be present, in which case the address returned tells the default port should be used.
     *
     * @param input input string
     * @return an address object containing the address from the input string
     */
    public static DeviceAddress parseAddress(String input)
    {
        return parseAddress(input, DEFAULT_PORT);
    }

    /**
     * Parses an address (host and possibly a port) from the given input string.
     * <p/>
     * The part of the input string after the last colon is interpreted as a port, everything else as host.
     * The port might not be present, in which case the given default port is used.
     *
     * @param input       input string
     * @param defaultPort default port to be used when the input string does not contain the port specification
     * @return an address object containing the address from the input string
     */
    public static DeviceAddress parseAddress(String input, int defaultPort)
    {
        String host = input;
        int port = defaultPort;

        int colonPos = host.lastIndexOf(':');
        if (colonPos != -1) {
            try {
                port = Integer.parseInt(host.substring(colonPos + 1));
                host = host.substring(0, colonPos);
            }
            catch (NumberFormatException ignored) {
                // host remains the same, port default
            }
        }

        return new DeviceAddress(host, port);
    }

    /**
     * Constructs an address to the host on its default port.
     * <p/>
     * What is the default port depends on the usage of the address. This just tells that the default port should be used.
     *
     * @param host device host
     */
    public DeviceAddress(String host)
    {
        this(host, DEFAULT_PORT);
    }

    public DeviceAddress(String host, int port)
    {
        if (host.startsWith("http://")) {
            host = host.substring(7);
            this.ssl = false;
        }
        else {
            if (host.startsWith("https://")) {
                host = host.substring(8);
            }
            this.ssl = true;
        }
        if (host.endsWith("/")) {
            host = host.substring(0,host.length() - 1);
        }
        this.host = host;
        this.port = port;
    }

    public boolean isSsl()
    {
        return ssl;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String toString()
    {
        if (port != DEFAULT_PORT) {
            return String.format("%s:%d", host, port);
        }
        else {
            return host;
        }
    }

    public String getUrl()
    {
        if (isSsl()) {
            return "https://" + host;
        }
        else {
            return "http://" + host;
        }
    }

    public String getFullUrl()
    {
        String host = this.host;
        while (host.endsWith("/")) {
            host = host.substring(0,host.length() - 1);
        }
        host += ":" + port;
        if (isSsl()) {
            return "https://" + host;
        }
        else {
            return "http://" + host;
        }
    }
}
