package cz.cesnet.shongo.util;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * {@link Authenticator} for username and password.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PasswordAuthenticator extends Authenticator
{
    /**
     * Username.
     */
    private String userName;

    /**
     * Password for {@link #userName}.
     */
    private String password;

    /**
     * Constructor.
     *
     * @param userName sets the {@link #userName}
     * @param password sets the {@link #password}
     */
    public PasswordAuthenticator(String userName, String password)
    {
        this.userName = userName;
        this.password = password;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
        return new PasswordAuthentication(userName, password);
    }
}
