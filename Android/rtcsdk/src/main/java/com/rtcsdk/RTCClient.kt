package com.rtcsdk

import android.R.attr
import android.app.Application
import android.content.Context
import org.webrtc.*


class RTCClient(
    context: Application,
    observer: PeerConnection.Observer,
    iceServer: List<PeerConnection.IceServer>
) {

    companion object {
        private const val LOCAL_VIDEO_TRACK_ID = "ARDAMSv0"
        private const val LOCAL_AUDIO_TRACK_ID = "ARDAMSa0"
        private const val LOCAL_STREAM_ID = "ARDAMS"
    }

    private val rootEglBase: EglBase = EglBase.create()

    init {
        initPeerConnectionFactory(context)

    }


    private val peerConnectionFactory by lazy { buildPeerConnectionFactory() }
    private val videoCapturer by lazy { getVideoCapturer(context) }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val peerConnection by lazy { buildPeerConnection(observer, iceServer) }

    private var localStream:MediaStream? = null



    private fun initPeerConnectionFactory(context: Application) {


        // Initialize field trials.
        val fieldTrials = ""

        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        val factory = PeerConnectionFactory
            .builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()

        return factory
    }

    private fun buildPeerConnection(observer: PeerConnection.Observer, iceServer:List<PeerConnection.IceServer>) : PeerConnection?  {


        val config = PeerConnection.RTCConfiguration(iceServer)
        config.enableDtlsSrtp = true
        val peerConnection =  peerConnectionFactory.createPeerConnection(
            config,
            observer)

        return peerConnection
    }


    private fun getVideoCapturer(context: Context) =
        Camera2Enumerator(context).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }



    fun initSurfaceView(view: SurfaceViewRenderer) = view.run {
        setMirror(true)
        setEnableHardwareScaler(true)
        init(rootEglBase.eglBaseContext, null)
    }

    fun startLocalVideoCapture(localVideoOutput: SurfaceViewRenderer) {
        val audioConstraints = MediaConstraints(). apply {
            mandatory.add(MediaConstraints.KeyValuePair("levelControl", "true"))
        }




        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        (videoCapturer as VideoCapturer).initialize(surfaceTextureHelper, localVideoOutput.context, localVideoSource.capturerObserver)
        videoCapturer.startCapture(320, 240, 60)
        val localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_VIDEO_TRACK_ID, localVideoSource)
        localVideoTrack.addSink(localVideoOutput)
        localVideoTrack.setEnabled(true)
        val audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        val localAudioTrack = peerConnectionFactory.createAudioTrack(LOCAL_AUDIO_TRACK_ID, audioSource)
        localAudioTrack.setEnabled(true)

        localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)
        localStream!!.addTrack(localVideoTrack)
        localStream!!.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }

    fun muteCall() {
        localStream!!.audioTracks[0].setEnabled(false)
    }

    fun unMuteCall() {
        localStream!!.audioTracks[0].setEnabled(true)
    }

    fun stopVideo() {
        localStream!!.videoTracks[0].setEnabled(false)
    }

    fun startVideo() {
        localStream!!.videoTracks[0].setEnabled(true)
    }

    private fun PeerConnection.call(sdpObserver: SdpObserver) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        createOffer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {

                setLocalDescription(object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                    }

                    override fun onSetSuccess() {
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                    }

                    override fun onCreateFailure(p0: String?) {
                    }
                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }
        }, constraints)
    }

    private fun PeerConnection.answer(sdpObserver: SdpObserver) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        createAnswer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                setLocalDescription(object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                    }

                    override fun onSetSuccess() {
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                    }

                    override fun onCreateFailure(p0: String?) {
                    }
                }, p0)
                sdpObserver.onCreateSuccess(p0)
            }
        }, constraints)
    }

    fun call(sdpObserver: SdpObserver) = peerConnection?.call(sdpObserver)

    fun answer(sdpObserver: SdpObserver) = peerConnection?.answer(sdpObserver)

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetFailure(p0: String?) {
            }

            override fun onSetSuccess() {
            }

            override fun onCreateSuccess(p0: SessionDescription?) {
            }

            override fun onCreateFailure(p0: String?) {
            }
        }, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate?) {
        peerConnection?.addIceCandidate(iceCandidate)
    }
}