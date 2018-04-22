package computician.janusclientapi;

/**
 * Created by ben.trent on 5/8/2015.
 */
public enum JanusMessageType {
    message,
    trickle,
    detach,
    destroy,
    keepalive,
    create,
    attach,
    event,
    error,
    ack,
    success,
    webrtcup,
    hangup,
    detached,
    media;

    @Override
    public String toString() {
        return name();
    }

    public boolean EqualsString(final String type) {
        return this.toString().equals(type);
    }

    public static JanusMessageType fromString(final String string) {
        return valueOf(JanusMessageType.class, string.toLowerCase());
    }
}
