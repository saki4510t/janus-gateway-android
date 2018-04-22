package computician.janusclientapi;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ben.trent on 7/8/2015.
 */
public class JanusWebRtcTransaction implements ITransactionCallbacks {
    private static final boolean DEBUG = true;	// set false on  production
   	private static final String TAG = JanusWebRtcTransaction.class.getSimpleName();

    private final IPluginHandleWebRTCCallbacks callbacks;
    private final JanusSupportedPluginPackages plugin;

    public JanusWebRtcTransaction(final JanusSupportedPluginPackages plugin,
    	final IPluginHandleWebRTCCallbacks callbacks) {

        this.callbacks = callbacks;
        this.plugin = plugin;
    }

    public TransactionType getTransactionType() {
        return TransactionType.plugin_handle_webrtc_message;
    }

    public void reportSuccess(final JSONObject obj) {
        try {
            final JanusMessageType type = JanusMessageType.fromString(obj.getString("janus"));
            switch (type) {
                case success: {
                    break;
                }
                case ack: {

                }
                default: {
                    callbacks.onCallbackError(obj.getJSONObject("error").getString("reason"));
                    break;
                }
            }
        } catch (final JSONException ex) {
            callbacks.onCallbackError(ex.getMessage());
        }
    }
}
