package com.rtcsdk

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class SocketListener : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.i("","Socket connection: onOpen: $response")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.i("","Socket connection:onMessage (Text): $text")
    }

    override fun onMessage(webSocket: WebSocket,
                           bytes: ByteString
    ) {
        Log.i("","Socket connection:onMessage (ByteString): $bytes")
    }

    override fun onClosing(webSocket: WebSocket,
                           code: Int, reason: String) {
        Log.i("","Socket connection:onClosing: Code: $code Reason:$reason")
    }

    override fun onClosed(webSocket: WebSocket,
                          code: Int, reason: String) {
        Log.i("","Socket connection:onClosed: Code: $code Reason:$reason")
    }

    override fun onFailure(webSocket: WebSocket,
                           throwable:Throwable,
                           response: Response?) {
        Log.e("","Socket connection:onFailure: Throwable:${throwable.message} Response: $response")
    }
}