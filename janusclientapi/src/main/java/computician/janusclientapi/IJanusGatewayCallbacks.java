package computician.janusclientapi;

import org.webrtc.PeerConnection;

import java.util.List;

/**
 * Created by ben.trent on 5/7/2015.
 * Modified by t_saki t_saki@serenegiant.com on 2018
 */
public interface IJanusGatewayCallbacks extends IJanusCallbacks {
    public void onSuccess();

    public void onDestroy();

    public String getServerUri();

    public List<PeerConnection.IceServer> getIceServers();

    public boolean getIpv6Support();

    public int getMaxPollEvents();
}
