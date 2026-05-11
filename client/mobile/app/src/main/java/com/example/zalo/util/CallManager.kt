package com.example.zalo.util

import android.content.Context
import android.util.Log
import com.example.zalo.data.remote.WebSocketManager
import com.example.zalo.data.remote.dto.CallSignalDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallManager @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val tokenManager: TokenManager
) {
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    private var peerConnection: PeerConnection? = null
    private var factory: PeerConnectionFactory? = null
    
    private val _incomingCall = MutableStateFlow<CallSignalDto?>(null)
    val incomingCall: StateFlow<CallSignalDto?> = _incomingCall

    fun init(context: Context) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
    }

    private fun createPeerConnection(): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        return factory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                // Send candidate over websocket
                webSocketManager.sendCallSignal(CallSignalDto(
                    type = "ice-candidate",
                    candidate = candidate.sdp,
                    fromUserId = tokenManager.getUserId()
                ))
            }
            override fun onAddStream(stream: MediaStream) { /* Handle remote video/audio */ }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                Log.d("CallManager", "ICE State: $state")
            }
            override fun onDataChannel(dc: DataChannel) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {}
        })
    }

    fun initiateCall(targetUserId: String, callType: String) {
        peerConnection = createPeerConnection()
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(this, desc)
                webSocketManager.sendCallSignal(CallSignalDto(
                    type = "call-offer",
                    targetUserId = targetUserId,
                    callType = callType,
                    sdp = desc.description,
                    fromUserId = tokenManager.getUserId()
                ))
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(err: String?) {}
            override fun onSetFailure(err: String?) {}
        }, constraints)
    }

    fun handleSignal(signal: CallSignalDto) {
        when (signal.type) {
            "call-offer" -> {
                _incomingCall.value = signal
            }
            "call-answer" -> {
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() { Log.d("CallManager", "Answer Set") }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, SessionDescription(SessionDescription.Type.ANSWER, signal.sdp))
            }
            "ice-candidate" -> {
                val candidate = IceCandidate("sdpMid", 0, signal.candidate)
                peerConnection?.addIceCandidate(candidate)
            }
            "call-end", "call-reject", "call-cancel" -> endCall()
        }
    }

    fun endCall() {
        peerConnection?.close()
        peerConnection = null
        _incomingCall.value = null
    }
}
