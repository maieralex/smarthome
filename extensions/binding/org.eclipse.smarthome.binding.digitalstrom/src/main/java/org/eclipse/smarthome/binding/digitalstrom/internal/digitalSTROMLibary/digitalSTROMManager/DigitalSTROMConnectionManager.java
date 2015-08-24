package org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager;

import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMListener.DigitalSTROMConnectionListener;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMServerConnection.DigitalSTROMAPI;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMServerConnection.HttpTransport;

//Therefor he create the --- and provide ???

/**
 * The {@link DigitalSTROMConnectionManager} manage the connection to a digitalSTROM-Server.
 *
 * @author Michael Ochel - Initial contribution
 * @author Mathias Siegele - Initial contribution
 *
 */
public interface DigitalSTROMConnectionManager {

    /**
     * Return the {@link HttpTransport} to execute queries or special commands on the digitalSTROM-Server.
     *
     * @return
     */
    public HttpTransport getHttpTransport();

    /**
     * Return the {@link DigitalSTROMAPI} to execute commands on the digitalSTROM-Server.
     *
     * @return
     */
    public DigitalSTROMAPI getDigitalSTROMAPI();

    /**
     * This method has to be called before each command to check the connection to the digitalSTROM-Server.
     * It examines the connection to the server, sets a new session token if it is expired and sets a new
     * ApplicationToken,
     * if none it set at the DigitalSTROM-Server. It also outputs the specific connection failure.
     *
     * @return true if the connection is established and false if not
     */
    public boolean checkConnection();

    /**
     * Returns the current session-token.
     *
     * @return session-token
     */
    public String getSessionToken();

    /**
     * Return the auto-generated or user defined application-token.
     *
     * @return application-token
     */
    public String getApplicationToken();

    /**
     * Checks the connection with {@link DigitalSTROMConnectionManager#checkConnection()} and returns the current
     * session-token.
     *
     * @return session-token
     */
    public String checkConnectionAndGetSessionToken();

    /**
     * Register a {@link DigitalSTROMConnectionListener} to this {@link DigitalSTROMConnectionManager}.
     *
     * @param connectionListener
     */
    public void registerConnectionListener(DigitalSTROMConnectionListener connectionListener);

    /**
     * Unregister the {@link DigitalSTROMConnectionListener} from this {@link DigitalSTROMConnectionManager}.
     */
    public void unregisterConnectionListener();

    boolean removeApplicationToken();

}
