package computician.janusclientapi;

/**
 * Created by ben.trent on 6/25/2015.
 */
class JanusMessagerFactory {
    public static IJanusMessenger createMessager(final String uri,
        final IJanusMessageObserver handler) {

        if (uri.indexOf("ws") == 0) {
            return new JanusWebsocketMessenger(uri, handler);
        } else {
            return new JanusRestMessenger(uri, handler);
        }
    }
}
