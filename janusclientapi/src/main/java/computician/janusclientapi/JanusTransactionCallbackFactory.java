package computician.janusclientapi;

/**
 * Created by ben.trent on 6/25/2015.
 */
class JanusTransactionCallbackFactory {

    public static ITransactionCallbacks createNewTransactionCallback(
        final JanusServer server, final TransactionType type) {

        switch (type) {
            case create:
                return new JanusCreateSessionTransaction(server);
            default:
                return null;
        }
    }

    public static ITransactionCallbacks createNewTransactionCallback(
        final JanusServer server, TransactionType type,
        final JanusSupportedPluginPackages plugin,
        final IPluginHandleWebRTCCallbacks callbacks) {

        switch (type) {
            case plugin_handle_webrtc_message:
                return new JanusWebRtcTransaction(plugin, callbacks);
            default:
                return null;
        }
    }

    public static ITransactionCallbacks createNewTransactionCallback(
        final JanusServer server,
        final TransactionType type,
        final JanusSupportedPluginPackages plugin,
        final IPluginHandleSendMessageCallbacks callbacks) {

        switch (type) {
            case plugin_handle_message:
                return new JanusSendPluginMessageTransaction(plugin, callbacks);
            default:
                return null;
        }
    }

    public static ITransactionCallbacks createNewTransactionCallback(
        final JanusServer server,
        final TransactionType type,
        final JanusSupportedPluginPackages plugin,
        final IJanusPluginCallbacks callbacks) {

        switch (type) {
            case create:
                return new JanusCreateSessionTransaction(server);
            case attach:
                return new JanusAttachPluginTransaction(server, plugin, callbacks);
            default:
                return null;
        }
    }
}
