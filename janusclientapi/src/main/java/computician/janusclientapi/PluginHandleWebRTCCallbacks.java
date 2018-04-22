package computician.janusclientapi;

import android.util.Log;

import org.json.JSONObject;

/**
 * Created by ben.trent on 8/13/2015.
 */
public class PluginHandleWebRTCCallbacks implements IPluginHandleWebRTCCallbacks {
    private static final boolean DEBUG = true;	// set false on  production
   	private static final String TAG = PluginHandleWebRTCCallbacks.class.getSimpleName();

    private final JanusMediaConstraints constraints;
    private final JSONObject jsep;
    private final boolean trickle;

    public PluginHandleWebRTCCallbacks(final JanusMediaConstraints constraints,
        final JSONObject jsep, final boolean trickle) {

        this.constraints = constraints;
        this.jsep = jsep;
        this.trickle = trickle;
    }

    @Override
    public void onSuccess(final JSONObject obj) {

    }

    @Override
    public JSONObject getJsep() {
        return jsep;
    }

    @Override
    public JanusMediaConstraints getMedia() {
        return constraints;
    }

    @Override
    public boolean getTrickle() {
        return trickle;
    }

    @Override
    public void onCallbackError(final String error) {
        Log.w(TAG, error);
    }
}
