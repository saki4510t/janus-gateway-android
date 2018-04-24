package computician.janusclientapi;

/**
 * Created by ben.trent on 6/25/2015.
 */
class JanusMessengerFactory {
    public static IJanusMessenger createMessenger(final String uri,
        final IJanusMessageObserver handler) {

        if (uri.indexOf("ws") == 0) {
            return new JanusWebsocketMessenger(uri, handler);
        } else {
            return new JanusRestMessenger(uri, handler);
        }
    }
}
