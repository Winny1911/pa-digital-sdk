package com.rtcsdk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.engineio.client.transports.WebSocket
import com.github.nkzawa.socketio.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext
import com.github.nkzawa.socketio.client.IO as SocketIO


class WaitingRoomActivity : AppCompatActivity() {

    private var txvWaitingMessage : TextView? = null;

    private val parentJob = Job()

    private val scope = CoroutineScope(coroutineContext)


    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Default


    private var tokenRequest: TokenRequest?= null;
    private var mSocket : Socket? = null;
    private var person : TicketNumberRequest? = null
    private var baseApiUri : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        txvWaitingMessage = findViewById(R.id.txvWaitingMessage)
        txvWaitingMessage!!.text = "Conectando..."

        var bundle :Bundle ?=intent.extras
        var clientId: String ?= intent.getStringExtra("clientId")
        var clientSecret: String ?= intent.getStringExtra("clientSecret")
        baseApiUri = intent.getStringExtra("baseApiUri")
        person = intent.getParcelableExtra("person")

        tokenRequest = TokenRequest(clientId!!,clientSecret!!);
        startJoinRoom()
    }

    private fun startJoinRoom() {
        var repository:EndpointRepository? = EndpointRepository(Apifactory.endpointApi("${baseApiUri}api/"))
        if(tokenRequest != null)
        {
            scope.launch {
                val token = repository?.getToken(tokenRequest!!)
                if(token!= null)
                {
                    val ticketNumber = repository?.generateTicket("Bearer ${token.token}", person!!)
                    if(ticketNumber != null) {
                        val ticketStatus =
                            repository?.getTicket("Bearer ${token.token}", ticketNumber!!.ticketId)

                            val mOptions =
                                SocketIO.Options()
                            mOptions.query = "ticketId=${ticketNumber.ticketId}"
                            mOptions.transports = arrayOf(WebSocket.NAME);
                            mSocket = SocketIO.socket("${baseApiUri}client", mOptions);
                            mSocket!!.io().timeout(-1);
                            mSocket!!.on("connect",onSocketConnected);
                            mSocket!!.on("client.connected",onClientConnected);
                            mSocket!!.on("client.queueInfo",onQueueInfo);
                            mSocket!!.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
                            mSocket!!.on("client.callTicket",onCallTicket);
                            mSocket!!.connect();

                    }
                }
            }
        }

    }

    private val onConnectError = Emitter.Listener { args ->
        Log.i("","Error Connecting socket");
    }

    private val onSocketConnected = Emitter.Listener { args ->
        Log.i("","Socket Connected");
    }

    private val onClientConnected = Emitter.Listener { args ->
        Log.i("","Client Connected");
        this@WaitingRoomActivity.runOnUiThread(java.lang.Runnable {
            txvWaitingMessage!!.text = "Conectado!"
        })
    }

    private val onQueueInfo = Emitter.Listener { args ->
        Log.i("","On Queue Info");

        val jsonObject: JSONObject = args[0] as JSONObject;
        val msg:String = "Aguardando ${jsonObject["ticketsBefore"]} pessoas  na sua frente"
        this@WaitingRoomActivity.runOnUiThread(java.lang.Runnable {
            txvWaitingMessage!!.text = msg
        })


    }

    private val onCallTicket = Emitter.Listener { args ->
        Log.i("","Ticket Call");
        val jsonObject: JSONObject = args[0] as JSONObject;

        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("baseApiUri", baseApiUri)
            putExtra("roomId", jsonObject["roomId"].toString())
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket!!.disconnect()
        mSocket!!.off("connect",onSocketConnected);
        mSocket!!.off("client.connected",onClientConnected);
        mSocket!!.off("client.queueInfo",onQueueInfo);
        mSocket!!.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket!!.off("client.callTicket",onCallTicket);
    }


}