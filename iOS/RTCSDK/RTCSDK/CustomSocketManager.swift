//
//  CustomSocketManager.swift
//  RTCSDK
//
//  Created by Ricardo Dorta on 13/12/20.
//

import Foundation
import SocketIO

class CustomSocketManager : SocketManager {
    override func socket(forNamespace nsp: String) -> SocketIOClient {
        assert(nsp.hasPrefix("/"), "forNamespace must have a leading /")

        if let socket = nsps[nsp] {
            return socket
        }

        let client = CustomSocketClient(manager: self, nsp: nsp)

        nsps[nsp] = client

        return client
    }
}

class CustomSocketClient : SocketIOClient {
    
    override func handleEvent(_ event: String, data: [Any], isInternalMessage: Bool, withAck ack: Int = -1) {
        if(event != "client.connected" && event != "client.queueInfo") {
            return super.handleEvent(event, data: data, isInternalMessage: isInternalMessage, withAck: ack)
        }

        for handler in handlers where handler.event == event {
            handler.executeCallback(with: data, withAck: ack, withSocket: self)
        }
    }
    
}
