package computician.janusclientapi;

import android.util.Log;

import org.json.JSONObject;

/**
 * Created by ben.trent on 8/12/2015.
 */
public class PluginHandleSendMessageCallbacks
    implements IPluginHandleSendMessageCallbacks {

    private static final boolean DEBUG = true;	// set false on  production
   	private static final String TAG = PluginHandleSendMessageCallbacks.class.getSimpleName();

    private final JSONObject message;

    public PluginHandleSendMessageCallbacks(final JSONObject message) {
        this.message = message;
    }

    @Override
    public void onSuccessSynchronous(final JSONObject obj) {
    }

    @Override
    public void onSuccesAsynchronous() {
    }

    @Override
    public JSONObject getMessage() {
        return message;
    }

    @Override
    public void onCallbackError(final String error) {
        Log.w(TAG, error);
    }
}
