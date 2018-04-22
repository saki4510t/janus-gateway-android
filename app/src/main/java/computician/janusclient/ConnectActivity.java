package computician.janusclient;

/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *  Modified 2018 t_saki t_saki@serenegiant.com for janus-gateway-android
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
 
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import static computician.janusclient.Const.*;

public class ConnectActivity extends BaseActivity {
	private static final boolean DEBUG = true;    // XXX set false on production
	private static final String TAG = ConnectActivity.class.getSimpleName();
	
	private static final int CONNECTION_REQUEST = 1;

	private ImageButton addFavoriteButton;
	private EditText roomEditText;
	private ListView roomListView;
	private SharedPreferences sharedPref;
	private ArrayList<String> roomList;
	private ArrayAdapter<String> adapter;
	
	private String keyprefRoomServerUrl;
	private String keyprefResolution;
	private String keyprefFps;
	private String keyprefVideoBitrateType;
	private String keyprefVideoBitrateValue;
	private String keyprefAudioBitrateType;
	private String keyprefAudioBitrateValue;
	private String keyprefRoom;
	private String keyprefRoomList;
	
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Get setting keys.
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		
		setContentView(R.layout.activity_connect);
		
		roomEditText = findViewById(R.id.room_edittext);
		roomEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
				if (i == EditorInfo.IME_ACTION_DONE) {
					addFavoriteButton.performClick();
					return true;
				}
				return false;
			}
		});
		roomEditText.requestFocus();
		
		roomListView = findViewById(R.id.room_listview);
		roomListView.setEmptyView(findViewById(android.R.id.empty));
		roomListView.setOnItemClickListener(roomListClickListener);
		registerForContextMenu(roomListView);
		ImageButton connectButton = findViewById(R.id.connect_button);
		connectButton.setOnClickListener(connectListener);
		addFavoriteButton = findViewById(R.id.add_favorite_button);
		addFavoriteButton.setOnClickListener(addFavoriteListener);
		
		setupPrefKeyName();
	}

	@Override
	public void onResume() {
		super.onResume();
		final String room = sharedPref.getString(keyprefRoom, "");
		roomEditText.setText(room);
		roomList = new ArrayList<>();
		final String roomListJson = sharedPref.getString(keyprefRoomList, null);
		if (!TextUtils.isEmpty(roomListJson)) {
			try {
				final JSONArray jsonArray = new JSONArray(roomListJson);
				for (int i = 0; i < jsonArray.length(); i++) {
					roomList.add(jsonArray.get(i).toString());
				}
			} catch (JSONException e) {
				Log.e(TAG, "Failed to load room list: " + e.toString());
			}
		}
		adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, roomList);
		roomListView.setAdapter(adapter);
		if (adapter.getCount() > 0) {
			roomListView.requestFocus();
			roomListView.setItemChecked(0, true);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		final String room = roomEditText.getText().toString();
		final String roomListJson = new JSONArray(roomList).toString();
		sharedPref.edit()
			.putString(keyprefRoom, room)
			.putString(keyprefRoomList, roomListJson)
			.apply();
	}

//================================================================================
	
	/**
	 * set key strings for SharedPreferences
	 */
	private void setupPrefKeyName() {
		keyprefRoomServerUrl = getString(R.string.pref_room_server_url_key);
		keyprefResolution = getString(R.string.pref_resolution_key);
		keyprefFps = getString(R.string.pref_fps_key);
		keyprefVideoBitrateType = getString(R.string.pref_maxvideobitrate_key);
		keyprefVideoBitrateValue = getString(R.string.pref_maxvideobitratevalue_key);
		keyprefAudioBitrateType = getString(R.string.pref_startaudiobitrate_key);
		keyprefAudioBitrateValue = getString(R.string.pref_startaudiobitratevalue_key);
		keyprefRoom = getString(R.string.pref_room_key);
		keyprefRoomList = getString(R.string.pref_room_list_key);
	}

	private boolean validateUrl(final String url) {
		if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
			return true;
		}

		new AlertDialog.Builder(this)
			.setTitle(getText(R.string.invalid_url_title))
			.setMessage(getString(R.string.invalid_url_text, url))
			.setCancelable(false)
			.setNeutralButton(R.string.ok,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			})
		.create()
		.show();
		return false;
	}

	private final AdapterView.OnItemClickListener roomListClickListener =
		new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> adapterView, final View view, int position, long id) {
				String roomId = ((TextView) view).getText().toString();
				connectToRoom(roomId);
			}
		};
	
	private final View.OnClickListener addFavoriteListener = new View.OnClickListener() {
		@Override
		public void onClick(final View view) {
			String newRoom = roomEditText.getText().toString();
			if (newRoom.length() > 0 && !roomList.contains(newRoom)) {
				adapter.add(newRoom);
				adapter.notifyDataSetChanged();
			}
		}
	};
	
	private final View.OnClickListener connectListener = new View.OnClickListener() {
		@Override
		public void onClick(final View view) {
			connectToRoom(roomEditText.getText().toString());
		}
	};
	
	private void connectToRoom(@Nullable final String roomId) {
		
		if (TextUtils.isEmpty(roomId)) return;
		if (!checkPermissionNetwork()) return;
		if (!checkPermissionAudio()) return;
		if (!checkPermissionCamera()) return;
		
		final String roomUrl = sharedPref.getString(
			keyprefRoomServerUrl, getString(R.string.pref_room_server_url_default));
		
		// Video call enabled flag.
		final boolean videoCallEnabled = sharedPrefGetBoolean(sharedPref,
			R.string.pref_videocall_key, R.string.pref_videocall_default);
		
		// Use screencapture option.
		final boolean useScreencapture = sharedPrefGetBoolean(sharedPref,
			R.string.pref_screencapture_key, R.string.pref_screencapture_default);
		
		// Use Camera2 option.
		final boolean useCamera2 = sharedPrefGetBoolean(sharedPref,
			R.string.pref_camera2_key, R.string.pref_camera2_default);
		
		// Get default codecs.
		final String videoCodec = sharedPrefGetString(sharedPref,
			R.string.pref_videocodec_key, R.string.pref_videocodec_default);
		final String audioCodec = sharedPrefGetString(sharedPref,
			R.string.pref_audiocodec_key, R.string.pref_audiocodec_default);
		
		// Check HW codec flag.
		final boolean hwCodec = sharedPrefGetBoolean(sharedPref,
			R.string.pref_hwcodec_key, R.string.pref_hwcodec_default);
		
		// Check Capture to texture.
		final boolean captureToTexture = sharedPrefGetBoolean(sharedPref,
			R.string.pref_capturetotexture_key, R.string.pref_capturetotexture_default);
		
		// Check FlexFEC.
		final boolean flexfecEnabled = sharedPrefGetBoolean(sharedPref,
			R.string.pref_flexfec_key, R.string.pref_flexfec_default);
		
		// Check Disable Audio Processing flag.
		final boolean noAudioProcessing = sharedPrefGetBoolean(sharedPref,
			R.string.pref_noaudioprocessing_key, R.string.pref_noaudioprocessing_default);
		
		final boolean aecDump = sharedPrefGetBoolean(sharedPref,
			R.string.pref_aecdump_key, R.string.pref_aecdump_default);
		
		final boolean saveInputAudioToFile = sharedPrefGetBoolean(sharedPref,
				R.string.pref_enable_save_input_audio_to_file_key,
				R.string.pref_enable_save_input_audio_to_file_default);
		
		// Check OpenSL ES enabled flag.
		final boolean useOpenSLES = sharedPrefGetBoolean(sharedPref,
			R.string.pref_opensles_key, R.string.pref_opensles_default);
		
		// Check Disable built-in AEC flag.
		final boolean disableBuiltInAEC = sharedPrefGetBoolean(sharedPref,
			R.string.pref_disable_built_in_aec_key, R.string.pref_disable_built_in_aec_default);
		
		// Check Disable built-in AGC flag.
		final boolean disableBuiltInAGC = sharedPrefGetBoolean(sharedPref,
			R.string.pref_disable_built_in_agc_key, R.string.pref_disable_built_in_agc_default);
		
		// Check Disable built-in NS flag.
		final boolean disableBuiltInNS = sharedPrefGetBoolean(sharedPref,
			R.string.pref_disable_built_in_ns_key, R.string.pref_disable_built_in_ns_default);
		
		// Check Disable gain control
		final boolean disableWebRtcAGCAndHPF = sharedPrefGetBoolean(sharedPref,
			R.string.pref_disable_webrtc_agc_and_hpf_key, R.string.pref_disable_webrtc_agc_and_hpf_key);
		
		// Get video resolution from settings.
		int videoWidth = 0;
		int videoHeight = 0;
		if (videoWidth == 0 && videoHeight == 0) {
			String resolution =
				sharedPref.getString(keyprefResolution, getString(R.string.pref_resolution_default));
			String[] dimensions = resolution.split("[ x]+");
			if (dimensions.length == 2) {
				try {
					videoWidth = Integer.parseInt(dimensions[0]);
					videoHeight = Integer.parseInt(dimensions[1]);
				} catch (NumberFormatException e) {
					videoWidth = 0;
					videoHeight = 0;
					Log.e(TAG, "Wrong video resolution setting: " + resolution);
				}
			}
		}
		
		// Get camera fps from settings.
		int cameraFps = 0;
		final String fps = sharedPref.getString(keyprefFps, getString(R.string.pref_fps_default));
		final String[] fpsValues = fps.split("[ x]+");
		if (fpsValues.length == 2) {
			try {
				cameraFps = Integer.parseInt(fpsValues[0]);
			} catch (NumberFormatException e) {
				cameraFps = 0;
				Log.e(TAG, "Wrong camera fps setting: " + fps);
			}
		}
		
		// Check capture quality slider flag.
		final boolean captureQualitySlider = sharedPrefGetBoolean(sharedPref,
			R.string.pref_capturequalityslider_key, R.string.pref_capturequalityslider_default);
		
		// Get video and audio start bitrate.
		int videoStartBitrate = 0;
		{
			final String bitrateTypeDefault = getString(R.string.pref_maxvideobitrate_default);
			final String bitrateType = sharedPref.getString(keyprefVideoBitrateType, bitrateTypeDefault);
			if (!bitrateType.equals(bitrateTypeDefault)) {
				final String bitrateValue = sharedPref.getString(
					keyprefVideoBitrateValue, getString(R.string.pref_maxvideobitratevalue_default));
				videoStartBitrate = Integer.parseInt(bitrateValue);
			}
		}
		
		int audioStartBitrate = 0;
		{
			final String bitrateTypeDefault = getString(R.string.pref_startaudiobitrate_default);
			final String bitrateType = sharedPref.getString(keyprefAudioBitrateType, bitrateTypeDefault);
			if (!bitrateType.equals(bitrateTypeDefault)) {
				final String bitrateValue = sharedPref.getString(
					keyprefAudioBitrateValue, getString(R.string.pref_startaudiobitratevalue_default));
				audioStartBitrate = Integer.parseInt(bitrateValue);
			}
		}
		
		// Check statistics display option.
		final boolean displayHud = sharedPrefGetBoolean(sharedPref,
			R.string.pref_displayhud_key, R.string.pref_displayhud_default);
		
		final boolean tracing = sharedPrefGetBoolean(sharedPref,
			R.string.pref_tracing_key, R.string.pref_tracing_default);
		
		// Check Enable org.appspot.apprtc.RtcEventLog.
		final boolean rtcEventLogEnabled = sharedPrefGetBoolean(sharedPref,
			R.string.pref_enable_rtceventlog_key, R.string.pref_enable_rtceventlog_default);
		
		final boolean useLegacyAudioDevice = sharedPrefGetBoolean(sharedPref,
			R.string.pref_use_legacy_audio_device_key, R.string.pref_use_legacy_audio_device_default);
		
		// Get datachannel options
		final boolean dataChannelEnabled = sharedPrefGetBoolean(sharedPref,
			R.string.pref_enable_datachannel_key, R.string.pref_enable_datachannel_default);
		final boolean ordered = sharedPrefGetBoolean(sharedPref,
			R.string.pref_ordered_key, R.string.pref_ordered_default);
		final boolean negotiated = sharedPrefGetBoolean(sharedPref,
			R.string.pref_negotiated_key, R.string.pref_negotiated_default);
		final int maxRetrMs = sharedPrefGetInteger(sharedPref,
			R.string.pref_max_retransmit_time_ms_key, R.string.pref_max_retransmit_time_ms_default);
		final int maxRetr = sharedPrefGetInteger(sharedPref,
			R.string.pref_max_retransmits_key, R.string.pref_max_retransmits_default);
		final int id = sharedPrefGetInteger(sharedPref,
			R.string.pref_data_id_key, R.string.pref_data_id_default);
		final String protocol = sharedPrefGetString(sharedPref,
			R.string.pref_data_protocol_key, R.string.pref_data_protocol_default);
		
		// Start AppRTCMobile activity.
		Log.d(TAG, "Connecting to room " + roomId + " at URL " + roomUrl);
		if (validateUrl(roomUrl)) {
			final Uri uri = Uri.parse(roomUrl);
			final Intent intent = new Intent(this, JanusActivity.class);
			intent.setData(uri);
			intent.putExtra(EXTRA_ROOMID, roomId);
			intent.putExtra(EXTRA_LOOPBACK, false);
			intent.putExtra(EXTRA_VIDEO_CALL, videoCallEnabled);
			intent.putExtra(EXTRA_SCREENCAPTURE, useScreencapture);
			intent.putExtra(EXTRA_CAMERA2, useCamera2);
			intent.putExtra(EXTRA_VIDEO_WIDTH, videoWidth);
			intent.putExtra(EXTRA_VIDEO_HEIGHT, videoHeight);
			intent.putExtra(EXTRA_VIDEO_FPS, cameraFps);
			intent.putExtra(EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED, captureQualitySlider);
			intent.putExtra(EXTRA_VIDEO_BITRATE, videoStartBitrate);
			intent.putExtra(EXTRA_VIDEOCODEC, videoCodec);
			intent.putExtra(EXTRA_HWCODEC_ENABLED, hwCodec);
			intent.putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, captureToTexture);
			intent.putExtra(EXTRA_FLEXFEC_ENABLED, flexfecEnabled);
			intent.putExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, noAudioProcessing);
			intent.putExtra(EXTRA_AECDUMP_ENABLED, aecDump);
			intent.putExtra(EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED, saveInputAudioToFile);
			intent.putExtra(EXTRA_OPENSLES_ENABLED, useOpenSLES);
			intent.putExtra(EXTRA_DISABLE_BUILT_IN_AEC, disableBuiltInAEC);
			intent.putExtra(EXTRA_DISABLE_BUILT_IN_AGC, disableBuiltInAGC);
			intent.putExtra(EXTRA_DISABLE_BUILT_IN_NS, disableBuiltInNS);
			intent.putExtra(EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, disableWebRtcAGCAndHPF);
			intent.putExtra(EXTRA_AUDIO_BITRATE, audioStartBitrate);
			intent.putExtra(EXTRA_AUDIOCODEC, audioCodec);
			intent.putExtra(EXTRA_DISPLAY_HUD, displayHud);
			intent.putExtra(EXTRA_TRACING, tracing);
			intent.putExtra(EXTRA_ENABLE_RTCEVENTLOG, rtcEventLogEnabled);
			intent.putExtra(EXTRA_USE_LEGACY_AUDIO_DEVICE, useLegacyAudioDevice);
			
			intent.putExtra(EXTRA_DATA_CHANNEL_ENABLED, dataChannelEnabled);
			
			if (dataChannelEnabled) {
				intent.putExtra(EXTRA_ORDERED, ordered);
				intent.putExtra(EXTRA_MAX_RETRANSMITS_MS, maxRetrMs);
				intent.putExtra(EXTRA_MAX_RETRANSMITS, maxRetr);
				intent.putExtra(EXTRA_PROTOCOL, protocol);
				intent.putExtra(EXTRA_NEGOTIATED, negotiated);
				intent.putExtra(EXTRA_ID, id);
			}
			
			if (getIntent().hasExtra(EXTRA_VIDEO_FILE_AS_CAMERA)) {
				final String videoFileAsCamera =
					getIntent().getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA);
				intent.putExtra(EXTRA_VIDEO_FILE_AS_CAMERA, videoFileAsCamera);
			}
			
			if (getIntent().hasExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE)) {
				final String saveRemoteVideoToFile =
					getIntent().getStringExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE);
				intent.putExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE, saveRemoteVideoToFile);
			}
			
			if (getIntent().hasExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH)) {
				final int videoOutWidth =
					getIntent().getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, 0);
				intent.putExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, videoOutWidth);
			}
			
			if (getIntent().hasExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT)) {
				final int videoOutHeight =
					getIntent().getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, 0);
				intent.putExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, videoOutHeight);
			}
			
			startActivityForResult(intent, CONNECTION_REQUEST);
		}
	}
}
