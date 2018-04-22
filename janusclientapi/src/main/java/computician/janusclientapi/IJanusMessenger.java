package computician.janusclientapi;

import java.math.BigInteger;

/**
 * Created by ben.trent on 5/7/2015.
 */
public interface IJanusMessenger {
    void connect();

    void disconnect();

    void sendMessage(final String message);

    void sendMessage(final String message, final BigInteger session_id);

    void sendMessage(final String message, final BigInteger session_id, final BigInteger handle_id);

    void receivedMessage(final String message);

    JanusMessengerType getMessengerType();
}
