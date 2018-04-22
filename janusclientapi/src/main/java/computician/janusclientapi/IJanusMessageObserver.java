package computician.janusclientapi;

import org.json.JSONObject;

/**
 * Created by ben.trent on 6/25/2015.
 */
interface IJanusMessageObserver {
    void receivedNewMessage(final JSONObject obj);

    void onClose();

    void onOpen();

    void onError(final Exception ex);
}
