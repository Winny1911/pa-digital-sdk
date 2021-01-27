package com.example.webrtcsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.rtcsdk.*

class MainActivity : AppCompatActivity() {

    private var helper: RTCSDKHelper = RTCSDKHelper();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        helper.initModule(this,"eb16476f-62f4-4e8a-8a80-9f7c693a80bd","96bdeb9f-1c1d-4e4d-aa96-08eb38f7f8bc","https://pa-digital-dev.ciadaconsulta.com.br/")
        setContentView(R.layout.activity_main)

        val btnJoinQueue = findViewById<Button>(R.id.btnJoinQueue)
        val btnJoinRoom = findViewById<Button>(R.id.btnJoinRoom)

        btnJoinQueue.setOnClickListener {
            val personRequest = TicketNumberRequest(TicketNumberPerson(
                "Fulano de Oliveira e Tal",
                "M",
                "1988-10-10",
                "renatonolo@hotmail.com",
                TicketNumberDocuments("059.592.380-12"),
                TicketNumberContact("993400828", "11", "55")
            ))

            helper.joinQueue(personRequest);
        }

        btnJoinRoom.setOnClickListener {
            helper.startCall("72bddb4987eda1b075598377af33fb62");
        }
    }
}
