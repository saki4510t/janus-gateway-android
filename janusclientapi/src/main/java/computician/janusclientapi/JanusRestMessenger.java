package computician.janusclientapi;

import android.net.Uri;
import android.util.Log;

import java.math.BigInteger;

import com.koushikdutta.async.*;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.*;
import com.koushikdutta.async.http.body.*;
import com.koushikdutta.async.http.callback.HttpConnectCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by ben.trent on 5/7/2015.
 */

//TODO big todo...it would be good to use androidasync as we already utilize that for the websocket endpoint
public class JanusRestMessenger implements IJanusMessenger {
	private static final boolean DEBUG = true;    // set false on  production
	private static final String TAG = JanusRestMessenger.class.getSimpleName();
	
	private final IJanusMessageObserver handler;
	private final String uri;
	private BigInteger session_id;
	private BigInteger handle_id;
	private String resturi;
	private final JanusMessengerType type = JanusMessengerType.restful;
	
	public void longPoll() {
		if (DEBUG) Log.v(TAG, "longPoll:");
		if (resturi.isEmpty())
			resturi = uri;
		
		final AsyncHttpGet get = new AsyncHttpGet(uri + "/" + session_id.toString() + "&maxev=1");
		
		AsyncHttpClient.getDefaultInstance().executeJSONObject(get, new AsyncHttpClient.JSONObjectCallback() {
			@Override
			public void onCompleted(Exception e, AsyncHttpResponse source, JSONObject result) {
				if (e == null) {
					receivedMessage(result.toString());
				} else {
					handler.onError(e);
				}
			}
		});
	}
	
	public JanusRestMessenger(final String uri, final IJanusMessageObserver handler) {
		this.handler = handler;
		this.uri = uri;
		resturi = "";
	}
	
	@Override
	public JanusMessengerType getMessengerType() {
		return type;
	}
	
	@Override
	public void connect() {
		if (DEBUG) Log.v(TAG, "connect:");
		AsyncHttpClient.getDefaultInstance().execute(uri, new HttpConnectCallback() {
			@Override
			public void onConnectCompleted(final Exception e, final AsyncHttpResponse response) {
				if (e == null) {
					handler.onOpen();
				} else {
					handler.onError(e);
				}
			}
		});
		
		//todo
	}
	
	@Override
	public void disconnect() {
		if (DEBUG) Log.v(TAG, "disconnect:");
		//todo
	}
	
	@Override
	public void sendMessage(final String message) {
		//todo
		if (DEBUG) Log.d("message", "Sent: \n\t" + message);
		if (resturi.isEmpty()) {
			resturi = uri;
		}
		final AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(resturi), "post");
		final AsyncHttpPost post = new AsyncHttpPost(resturi);
		
		JSONObject obj = null;
		try {
			obj = new JSONObject(message);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
		
		post.setBody(new JSONObjectBody(obj));
		
		AsyncHttpClient.getDefaultInstance().executeJSONObject(post, new AsyncHttpClient.JSONObjectCallback() {
			@Override
			public void onCompleted(final Exception e,
				final AsyncHttpResponse source, final JSONObject result) {
				
				if (e == null) {
					receivedMessage(result.toString());
				} else {
					handler.onError(e);
				}
			}
		});
		
		
	}
	
	@Override
	public void sendMessage(final String message, final BigInteger session_id) {
		//todo
		this.session_id = session_id;
		resturi = "";
		resturi = uri + "/" + session_id.toString();
		sendMessage(message);
	}
	
	@Override
	public void sendMessage(final String message,
		final BigInteger session_id, final BigInteger handle_id) {
		
		// todo
		this.session_id = session_id;
		this.handle_id = handle_id;
		resturi = "";
		resturi = uri + "/" + session_id.toString() + "/" + handle_id.toString();
		sendMessage(message);
	}
	
	//todo
	private void handleNewMessage(final String message) {
		if (DEBUG) Log.v(TAG, "handleNewMessage:" + message);
	}
	
	@Override
	public void receivedMessage(final String msg) {
		try {
			Log.d("message", "Recv: \n\t" + msg);
			final JSONObject obj = new JSONObject(msg);
			handler.receivedNewMessage(obj);
		} catch (final JSONException ex) {
			handler.onError(ex);
		}
	}
}
