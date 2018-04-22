package computician.janusclientapi;

import org.json.JSONObject;

/**
 * Created by ben.trent on 6/25/2015.
 */
interface IJanusAttachPluginCallbacks extends IJanusCallbacks {
    void attachPluginSuccess(final JSONObject obj,
        final JanusSupportedPluginPackages plugin,
        final IJanusPluginCallbacks callbacks);
}
