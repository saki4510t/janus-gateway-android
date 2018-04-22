package computician.janusclientapi; /**
 * Created by ben.trent on 5/7/2015.
 */

import org.json.JSONObject;
import org.webrtc.MediaStream;

public interface IJanusPluginCallbacks extends IJanusCallbacks {
    void success(final JanusPluginHandle handle);

    void onMessage(final JSONObject msg, final JSONObject jsep);

    void onLocalStream(final MediaStream stream);

    void onRemoteStream(final MediaStream stream);

    void onDataOpen(final Object data);

    void onData(final Object data);

    void onCleanup();

    void onDetached();

    JanusSupportedPluginPackages getPlugin();
}
