package org.appspot.apprtc;
/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *  modified st_saki t_saki@serenegian.com
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import static org.appspot.apprtc.Const.*;

/**
 * Peer connection parameters.
 * This class is a part of PeerConnectionClient
 * in official WebRTC sample app for Android
 */
public class PeerConnectionParameters {
	public final boolean videoCallEnabled;
	public final boolean loopback;
	public final boolean tracing;
	public final int videoWidth;
	public final int videoHeight;
	public final int videoFps;
	public final int videoMaxBitrate;
	public final String videoCodec;
	public final boolean videoCodecHwAcceleration;
	public final boolean videoFlexfecEnabled;
	public final int audioStartBitrate;
	public final String audioCodec;
	public final boolean noAudioProcessing;
	public final boolean aecDump;
	public final boolean saveInputAudioToFile;
	public final boolean useOpenSLES;
	public final boolean disableBuiltInAEC;
	public final boolean disableBuiltInAGC;
	public final boolean disableBuiltInNS;
	public final boolean disableWebRtcAGCAndHPF;
	public final boolean enableRtcEventLog;
	public final boolean useLegacyAudioDevice;
	public final DataChannelParameters dataChannelParameters;
	
	public PeerConnectionParameters(final Intent intent) {
		this(intent != null ? intent.getExtras() : (Bundle)null);
	}

	public PeerConnectionParameters(@Nullable final Bundle params) {
		this(params == null || params.getBoolean(EXTRA_VIDEO_CALL, true),
			params != null && params.getBoolean(EXTRA_LOOPBACK, false),
			params != null && params.getBoolean(EXTRA_TRACING, false),
			params != null ? params.getInt(EXTRA_VIDEO_WIDTH, 0) : 0,
			params != null ? params.getInt(EXTRA_VIDEO_HEIGHT, 0) : 0,
			params != null ? params.getInt(EXTRA_VIDEO_FPS, 0) : 0,
			params != null ? params.getInt(EXTRA_VIDEO_BITRATE, 0) : 0,
			params != null ? params.getString(EXTRA_VIDEOCODEC) : null,
			params == null || params.getBoolean(EXTRA_HWCODEC_ENABLED, true),
			params != null && params.getBoolean(EXTRA_FLEXFEC_ENABLED, false),
			params != null ? params.getInt(EXTRA_AUDIO_BITRATE, 0) : 0,
			params != null ? params.getString(EXTRA_AUDIOCODEC) : null,
			params != null && params.getBoolean(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
			params != null && params.getBoolean(EXTRA_AECDUMP_ENABLED, false),
			params != null && params.getBoolean(EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED),
			params != null && params.getBoolean(EXTRA_OPENSLES_ENABLED, false),
			params != null && params.getBoolean(EXTRA_DISABLE_BUILT_IN_AEC, false),
			params != null && params.getBoolean(EXTRA_DISABLE_BUILT_IN_AGC, false),
			params != null && params.getBoolean(EXTRA_DISABLE_BUILT_IN_NS, false),
			params != null && params.getBoolean(EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false),
			params != null && params.getBoolean(EXTRA_ENABLE_RTCEVENTLOG, false),
			params != null && params.getBoolean(EXTRA_USE_LEGACY_AUDIO_DEVICE, false),
			params != null ? (DataChannelParameters)params.getParcelable(EXTRA_DATACHANNEL) : null
		);
	}
	
	public PeerConnectionParameters(final boolean videoCallEnabled,
		final boolean loopback, final boolean tracing,
		final int videoWidth, final int videoHeight,
		final int videoFps, final int videoMaxBitrate, final String videoCodec,
		final boolean videoCodecHwAcceleration, final boolean videoFlexfecEnabled,
		final int audioStartBitrate, final String audioCodec,
		final boolean noAudioProcessing, final boolean aecDump,
		final boolean saveInputAudioToFile,
		final boolean useOpenSLES, final boolean disableBuiltInAEC,
		final boolean disableBuiltInAGC, final boolean disableBuiltInNS,
		final boolean disableWebRtcAGCAndHPF, final boolean enableRtcEventLog,
		final boolean useLegacyAudioDevice,
		final DataChannelParameters dataChannelParameters) {

		this.videoCallEnabled = videoCallEnabled;
		this.loopback = loopback;
		this.tracing = tracing;
		this.videoWidth = videoWidth;
		this.videoHeight = videoHeight;
		this.videoFps = videoFps;
		this.videoMaxBitrate = videoMaxBitrate;
		this.videoCodec = videoCodec;
		this.videoFlexfecEnabled = videoFlexfecEnabled;
		this.videoCodecHwAcceleration = videoCodecHwAcceleration;
		this.audioStartBitrate = audioStartBitrate;
		this.audioCodec = audioCodec;
		this.noAudioProcessing = noAudioProcessing;
		this.aecDump = aecDump;
		this.saveInputAudioToFile = saveInputAudioToFile;
		this.useOpenSLES = useOpenSLES;
		this.disableBuiltInAEC = disableBuiltInAEC;
		this.disableBuiltInAGC = disableBuiltInAGC;
		this.disableBuiltInNS = disableBuiltInNS;
		this.disableWebRtcAGCAndHPF = disableWebRtcAGCAndHPF;
		this.enableRtcEventLog = enableRtcEventLog;
		this.useLegacyAudioDevice = useLegacyAudioDevice;
		this.dataChannelParameters = dataChannelParameters;
	}
}
