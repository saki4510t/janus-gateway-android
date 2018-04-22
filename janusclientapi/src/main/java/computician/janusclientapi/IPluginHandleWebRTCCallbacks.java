package computician.janusclientapi;

import org.json.JSONObject;

/**
 * Created by ben.trent on 6/25/2015.
 */
public interface IPluginHandleWebRTCCallbacks extends IJanusCallbacks {
    void onSuccess(final JSONObject obj);

    JSONObject getJsep();

    JanusMediaConstraints getMedia();

    boolean getTrickle();
}
