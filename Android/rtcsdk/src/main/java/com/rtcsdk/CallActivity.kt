package com.rtcsdk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.fragment.app.FragmentActivity
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.engineio.client.transports.WebSocket
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Manager
import com.github.nkzawa.socketio.client.Socket
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.*
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import kotlin.coroutines.CoroutineContext


class CallActivity: AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
    }

    private val sdpObserver = object : AppSdpObserver() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            val jsonString = JSONObject("{\n" +
                    "  \"roomId\": \"$roomId\",\n" +
                    "  \"fromComputerId\": \"$computerId\",\n" +
                    "  \"toComputerId\": \"$peerId\",\n" +
                    "  \"signal\": { \"type\": \"${p0!!.type.toString().toLowerCase()}\", \"sdp\": \"${p0!!.description}\"  }\n" +
                    "}")
            mSocket!!.emit("sendSignal", jsonString)
        }

        override fun onCreateFailure(p0: String?) {
            super.onCreateFailure(p0)
            Log.i("","onCreateFailure");
        }

        override fun onSetSuccess() {
            super.onSetSuccess()
            Log.i("","onSetSuccess");
        }

        override fun onSetFailure(p0: String?) {
            super.onSetFailure(p0)
            Log.i("","onSetFailure");
        }
    }


    private lateinit var rtcClient: RTCClient

    private var baseApiUri : String? = null
    private var roomId : String? = null
    private var peerId: String? = null

    private var mSocket : Socket? = null;
    private var socketId : String? = null

    private val parentJob = Job()
    private var computerId: UUID = UUID.randomUUID();

    private var isMuted:Boolean  = false
    private var isVideoDisabled:Boolean  = false

    private val scope = CoroutineScope(coroutineContext)


    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Default



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        checkCameraPermission()
        checkAudioPermission()


        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0) //I believe this should set call volume to max?
        audioManager.isSpeakerphoneOn = true

        var bundle : Bundle?=intent.extras
        var clientId: String ?= intent.getStringExtra("clientId")
        var clientSecret: String ?= intent.getStringExtra("clientSecret")
        baseApiUri = intent.getStringExtra("baseApiUri")
        roomId = intent.getStringExtra("roomId")
        initRoom();
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
        } else {
            onCameraPermissionGranted()
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, AUDIO_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermission()
        } else {
            onAudioPermissionGranted()
        }
    }

    private fun onCameraPermissionGranted() {

    }

    private fun onAudioPermissionGranted() {

    }

    private fun requestCameraPermission(dialogShown: Boolean = false) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION) && !dialogShown) {
            showPermissionRationaleDialog()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private fun requestAudioPermission(dialogShown: Boolean = false) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, AUDIO_PERMISSION) && !dialogShown) {
            showPermissionRationaleDialog()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(AUDIO_PERMISSION), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("This app need the camera to function")
            .setPositiveButton("Grant") { dialog, _ ->
                dialog.dismiss()
                requestCameraPermission(true)
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
                onCameraPermissionDenied()
            }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            onCameraPermissionGranted()
        } else {
            onCameraPermissionDenied()
        }
    }

    private fun onCameraPermissionDenied() {
        Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun initRoom() {
        scope.launch {
            val mOptions =
                IO.Options()
            mOptions.transports = arrayOf(WebSocket.NAME);
            mOptions.timeout = -1
            var manager: Manager = Manager(URI("https://meet-dev.ciadaconsulta.com.br"), mOptions)
            mSocket = manager.socket("/public")
//            mSocket = IO.socket(, mOptions);
            mSocket!!.on("iceServers", onIceServers)
            mSocket!!.on("joinRoom", onJoinRoom)
            mSocket!!.on("signal", onSignal)
            mSocket!!.on(Socket.EVENT_CONNECT, onSocketConnected)
            mSocket!!.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
            mSocket!!.on(Socket.EVENT_ERROR, onConnectError)
            mSocket!!.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError)
            mSocket!!.on(Socket.EVENT_DISCONNECT, onDisconnect)

            mSocket!!.connect()

        }
    }

    private val onDisconnect = Emitter.Listener { args ->
        Log.i("","Disconnect");
    }

    private val onSignal = Emitter.Listener { args ->
        Log.i("","Signal");
        val gson: Gson =
            GsonBuilder().setFieldNamingStrategy { f -> f.name.toLowerCase() }.create()
        val jsonObject:JSONObject = args[0] as JSONObject
        peerId = jsonObject["fromComputerId"].toString()
        val signalObject:JSONObject = jsonObject["signal"] as JSONObject
        this@CallActivity.runOnUiThread(java.lang.Runnable {
            if(signalObject.has("candidate")) {
                val candidateObject = signalObject["candidate"] as JSONObject
                val sdpMid:String = candidateObject["sdpMid"].toString()
                val sdpMLineIndex:Int = candidateObject["sdpMLineIndex"] as Int
                val sdp:String = candidateObject["candidate"].toString()
                var candidate:IceCandidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                rtcClient.addIceCandidate(candidate);
            }
            else if (signalObject.has("type"))
            {

                if(signalObject["type"].toString().equals("offer", true)) {
                    val sessionDescription:SessionDescription = SessionDescription(SessionDescription.Type.OFFER,signalObject["sdp"].toString())
                    rtcClient.onRemoteSessionReceived(sessionDescription)
                    rtcClient.answer(sdpObserver)
                    remote_view_loading.isGone = true
                } else {
                    val sessionDescription:SessionDescription = SessionDescription(SessionDescription.Type.ANSWER,signalObject["sdp"].toString())
                    rtcClient.onRemoteSessionReceived(sessionDescription)
                    remote_view_loading.isGone = true
                }
            }
        })

    }

    private val onJoinRoom = Emitter.Listener { args ->
        Log.i("","JoinRoom");
        val peers:JSONArray = (args[0] as JSONObject)["peers"] as JSONArray
        if(peers.length() == 0) {
            this@CallActivity.runOnUiThread(java.lang.Runnable {

            })
        } else {
            peerId = (peers[0] as JSONObject).getString("computerId")
            this@CallActivity.runOnUiThread(java.lang.Runnable {
                rtcClient.call(sdpObserver)
            })
        }
    }

    private val onIceServers = Emitter.Listener { args ->
        Log.i("","IceServers");
        val userName = (args[0] as JSONObject)["username"].toString();
        val urls = (args[0] as JSONObject)["urls"] as JSONArray;
        val credential = (args[0] as JSONObject)["credential"].toString();
        val iceServers: MutableList<PeerConnection.IceServer> = mutableListOf()

        for (i in 0 until urls.length()) {
            val item = urls.getString(i)
            val iceServerBuilder = PeerConnection.IceServer.builder(item)

            iceServerBuilder.setUsername(userName);
            iceServerBuilder.setPassword(credential);

            iceServers.add(iceServerBuilder.createIceServer())
            // Your code here
        }

        this@CallActivity.runOnUiThread(java.lang.Runnable {
            rtcClient = RTCClient(
                application,
                object : PeerConnectionObserver() {
                    override fun onIceCandidate(p0: IceCandidate?) {
                        super.onIceCandidate(p0)
                        val jsonString = JSONObject("{\n" +
                                "  \"roomId\": \"$roomId\",\n" +
                                "  \"fromComputerId\": \"$computerId\",\n" +
                                "  \"toComputerId\": \"$peerId\",\n" +
                                "  \"signal\": {  type: 'candidate', \"candidate\": { \"sdpMid\": \"${p0!!.sdpMid.toString()}\", \"sdpMLineIndex\": \"${p0!!.sdpMLineIndex}\", \"candidate\": \"${p0!!.sdp}\"  } }\n" +
                                "}")
                        mSocket!!.emit("sendSignal", jsonString)
                        rtcClient.addIceCandidate(p0)
                    }

                    override fun onDataChannel(p0: DataChannel?) {
                        super.onDataChannel(p0)
                        p0!!.registerObserver(DcObserver())
                    }

                    override fun onAddStream(p0: MediaStream?) {
                        super.onAddStream(p0)
                        p0?.videoTracks?.get(0)?.addSink(remote_view)
                    }

                },
                iceServers
            )
            rtcClient.initSurfaceView(remote_view)
            rtcClient.initSurfaceView(local_view)
            rtcClient.startLocalVideoCapture(local_view)
            call_button.setOnClickListener {
                finish()
            }
            mic_button.setOnClickListener {
                if(isMuted)
                {
                    rtcClient.unMuteCall()
                } else {
                    rtcClient.muteCall()
                }
            }
            video_button.setOnClickListener {
                if(isVideoDisabled)
                {
                    rtcClient.startVideo()
                } else {
                    rtcClient.stopVideo()
                }
            }
        })

        this.mSocket!!.emit("joinRoom", JSONObject("""{"roomId":"$roomId", "computerId":$computerId}"""))
    }

    private val onConnectError = Emitter.Listener { args ->
        Log.i("","Error Connecting socket");
    }

    private val onSocketConnected = Emitter.Listener { args ->
        Log.i("","Socket Connected");
        if(socketId == null)
        {
            socketId = mSocket!!.id()
            mSocket!!.emit("iceServers")
        } else {
            this.mSocket!!.emit("updateSocketId", JSONObject("{ \"oldSocketId\":\"$socketId\", \"newSocketId\":\"${mSocket!!.id()}\"   }"))
            this.mSocket!!.emit("joinRoom", JSONObject("""{"roomId":"$roomId", "computerId":$computerId}"""))
            socketId = mSocket!!.id()
        }


    }


}