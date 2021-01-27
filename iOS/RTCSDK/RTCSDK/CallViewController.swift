//
//  CallView.swift
//  RTCSDK
//
//  Created by Ricardo Dorta on 13/12/20.
//

import UIKit
import WebRTC

public class CallViewController : UIViewController {
    
    var baseApiUri:String? = nil
    var roomId:String? = nil
    var socketId:String? = nil
    var peerId:String? = nil
    let computerId:UUID = UUID()
    @IBOutlet private weak var localVideoView: UIView?
    @IBOutlet private weak var remoteVideoView: UIView?
    
    var manager:CustomSocketManager? = nil
    var socket:CustomSocketClient? = nil
    var webRTCClient: WebRTCClient? = nil
    
    var mute: Bool = false
    var hideVideo: Bool = false
    
    @IBAction func videoTap(_ sender: Any) {
        self.hideVideo = !self.hideVideo
        if self.hideVideo {
            self.webRTCClient!.hideVideo()
        }
        else {
            self.webRTCClient!.showVideo()
        }
        
        self.webRTCClient?.hideVideo()
    }
    
    @IBAction func stopCallTap(_ sender: Any) {
        self.dismiss(animated: true)
    }
    
    @IBAction func micTap(_ sender: Any) {
        self.mute = !self.mute
        if self.mute {
            self.webRTCClient!.muteAudio()
        }
        else {
            self.webRTCClient!.unmuteAudio()
        }
    }
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        
        self.manager = CustomSocketManager(socketURL: URL(string: "https://meet-dev.ciadaconsulta.com.br")!, config: [.forceWebsockets(true), .log(true)])
        
        self.socket = self.manager!.socket(forNamespace: "/public") as? CustomSocketClient
        
        self.socket!.on(clientEvent: .connect) {data, ack in
            print("socket connected")
            if(self.socketId == nil)
            {
                self.socketId = self.socket!.sid
                self.socket!.emit("iceServers")
            } else {
                
                do {
                    let updateString = "{ \"oldSocketId\":\"\(self.socketId!)\", \"newSocketId\":\"\(self.socket!.sid!)\"   }"
                    let updateData = updateString.data(using: .utf8)!
                    if let jsonUpdate = try JSONSerialization.jsonObject(with: updateData, options : .allowFragments) as? Dictionary<String,Any>
                    {
                        self.socket!.emit("updateSocketId", jsonUpdate)
                    } else {
                        print("bad json")
                    }
                } catch let error as NSError {
                    print(error)
                }
                
                do {
                    let joinRoomString = "{\"roomId\":\"\(self.roomId!)\", \"computerId\":\(self.computerId.uuidString)}"
                    let joinRoomData = joinRoomString.data(using: .utf8)!
                    if let jsonJoinRoom = try JSONSerialization.jsonObject(with: joinRoomData, options : .allowFragments) as? Dictionary<String,Any>
                    {
                        self.socket!.emit("joinRoom", jsonJoinRoom)
                    } else {
                        print("bad json")
                    }
                } catch let error as NSError {
                    print(error)
                }
                
                self.socketId = self.socket!.sid
            }
        }
        
        self.socket!.on("iceServers") { data, ack in
            guard let jsonObject = data[0] as? Dictionary<String,AnyObject> else { return }
            
            let userName:String = jsonObject["username"] as! String
            let credential:String = jsonObject["credential"] as! String
            let urls:[String] = jsonObject["urls"] as! [String]
            var servers:[RTCIceServer] = []
            servers.append(RTCIceServer(urlStrings:urls, username: userName, credential: credential))
            
            self.webRTCClient = WebRTCClient(iceServers: servers)
            self.webRTCClient?.delegate = self
            
            
            #if arch(arm64)
                // Using metal (arm64 only)
                let localRenderer = RTCMTLVideoView(frame: self.localVideoView?.frame ?? CGRect.zero)
                let remoteRenderer = RTCMTLVideoView(frame: self.remoteVideoView?.frame ?? CGRect.zero)
                localRenderer.videoContentMode = .scaleAspectFill
                remoteRenderer.videoContentMode = .scaleAspectFill
            #else
                // Using OpenGLES for the rest
                let localRenderer = RTCEAGLVideoView(frame: self.localVideoView?.frame ?? CGRect.zero)
                let remoteRenderer = RTCEAGLVideoView(frame: self.remoteVideoView?.frame ?? CGRect.zero)
            #endif

            self.webRTCClient!.startCaptureLocalVideo(renderer: localRenderer)
            self.webRTCClient!.renderRemoteVideo(to: remoteRenderer)
            
            if let localVideoView = self.localVideoView {
                self.embedView(localRenderer, into: localVideoView)
            }
            
            if let remoteVideoView = self.remoteVideoView {
                self.embedView(remoteRenderer, into: remoteVideoView)
            }
            
            self.webRTCClient?.speakerOn()
            
            
            
            let string = "{\"roomId\":\"\(self.roomId!)\", \"computerId\":\"\(self.computerId.uuidString)\"}"
            let data = string.data(using: .utf8)!
            do {
                if let jsonArray = try JSONSerialization.jsonObject(with: data, options : .allowFragments) as? Dictionary<String,Any>
                {
                    self.socket!.emit("joinRoom", jsonArray)
                } else {
                    print("bad json")
                }
            } catch let error as NSError {
                print(error)
            }
            
            
            
        }
        
        self.socket!.on("signal") { data, ack in
            guard let jsonObject = data[0] as? Dictionary<String,AnyObject> else { return }
            self.peerId = jsonObject["fromComputerId"] as? String
            guard let signalObject = jsonObject["signal"] as? Dictionary<String,AnyObject> else { return }
            if(signalObject.keys.contains("candidate")) {
                guard let candidateObject = signalObject["candidate"] as? Dictionary<String,AnyObject> else { return }
                let sdpMid = candidateObject["sdpMid"]! as! String
                let sdpMLineIndex = candidateObject["sdpMid"]! as! String
                let sdp = candidateObject["candidate"]! as! String
                let candidate = RTCIceCandidate(sdp: sdp,sdpMLineIndex: Int32(sdpMLineIndex) ?? 0, sdpMid: sdpMid)
                self.webRTCClient!.set(remoteCandidate: candidate)
            } else {
                if(signalObject["type"]!.isEqual(to: "offer")) {
                    let sessionDescription = RTCSessionDescription(type: RTCSdpType.offer, sdp: signalObject["sdp"]! as! String)
                    self.webRTCClient!.set(remoteSdp: sessionDescription) { (error) in
                        
                    }
                    self.webRTCClient!.answer { (localSdp) in
                        
                        
                        let jsonString = "{\n" +
                            "  \"roomId\": \"\(self.roomId!)\",\n" +
                            "  \"fromComputerId\": \"\(self.computerId.uuidString)\",\n" +
                                            "  \"toComputerId\": \"\(self.peerId!)\",\n" +
                            "  \"signal\": { \"type\": \"answer\", \"sdp\": \"\(localSdp.description.replacingOccurrences(of: "RTCSessionDescription:\nanswer\n", with: "").replacingOccurrences(of: "\n", with: "\\n").replacingOccurrences(of: "\r", with: "\\r"))\"  }\n" +
                            "}";
                        
                        let data = jsonString.data(using: .utf8)!
                        do {
                            if let jsonArray = try JSONSerialization.jsonObject(with: data, options : .allowFragments) as? Dictionary<String,Any>
                            {
                                self.socket!.emit("sendSignal", jsonArray)
                            } else {
                                print("bad json")
                            }
                        } catch let error as NSError {
                            print(error)
                        }
                                            
                    }
                } else {
                    let sessionDescription = RTCSessionDescription(type: RTCSdpType.answer, sdp: signalObject["sdp"]! as! String)
                    self.webRTCClient!.set(remoteSdp: sessionDescription) { (error) in
                        
                    }
                }
            }
            
        }
        
        self.socket!.on("joinRoom") { data, ack in
            
            guard let jsonObject = data[0] as? Dictionary<String,AnyObject> else { return }
            
            let peers:[Dictionary<String,AnyObject>] = jsonObject["peers"] as! [Dictionary<String,AnyObject>]
            if(!peers.isEmpty)
            {
                self.peerId = peers[0]["computerId"] as? String
                
                
                self.webRTCClient!.offer { (localSdp) in
                    let jsonString = "{" +
                        "  \"roomId\": \"\(self.roomId!)\"," +
                        "  \"fromComputerId\": \"\(self.computerId.uuidString)\"," +
                                        "  \"toComputerId\": \"\(self.peerId!)\"," +
                        "  \"signal\": { \"type\": \"offer\", \"sdp\": \"\(localSdp.description.replacingOccurrences(of: "RTCSessionDescription:\noffer\n", with: "").replacingOccurrences(of: "\n", with: "\\n").replacingOccurrences(of: "\r", with: "\\r"))\"  }" +
                        "}";
                    
                    let data = jsonString.data(using: .utf8)!
                    do {
                        if let jsonArray = try JSONSerialization.jsonObject(with: data, options : .allowFragments) as? Dictionary<String,Any>
                        {
                            self.socket!.emit("sendSignal", jsonArray)
                        } else {
                            print("bad json")
                        }
                    } catch let error as NSError {
                        print(error)
                    }
                }
            }
            
        }
        
        self.socket!.connect()
        
    }
    
    private func embedView(_ view: UIView, into containerView: UIView) {
        containerView.insertSubview(view, at: 0)
        view.translatesAutoresizingMaskIntoConstraints = false
        containerView.addConstraints(NSLayoutConstraint.constraints(withVisualFormat: "H:|[view]|",
                                                                    options: [],
                                                                    metrics: nil,
                                                                    views: ["view":view]))
        
        containerView.addConstraints(NSLayoutConstraint.constraints(withVisualFormat: "V:|[view]|",
                                                                    options: [],
                                                                    metrics: nil,
                                                                    views: ["view":view]))
        containerView.layoutIfNeeded()
    }
    
    @IBAction private func backDidTap(_ sender: Any) {
        self.dismiss(animated: true)
    }
    
}

extension CallViewController: WebRTCClientDelegate {
    
    func webRTCClient(_ client: WebRTCClient, didDiscoverLocalCandidate candidate: RTCIceCandidate) {
        print("discovered local candidate")
        
        let jsonString = "{" +
            "  \"roomId\": \"\(self.roomId!)\"," +
            "  \"fromComputerId\": \"\(self.computerId.uuidString)\"," +
                            "  \"toComputerId\": \"\(self.peerId!)\"," +
            "  \"signal\": {  \"type\": \"candidate\", \"candidate\": { \"sdpMid\": \"\(candidate.sdpMid!)\", \"sdpMLineIndex\": \"\(candidate.sdpMLineIndex)\", \"candidate\": \"\(candidate.sdp.replacingOccurrences(of: "\n", with: "\\n").replacingOccurrences(of: "\r", with: "\\r"))\"  } }" +
            "}";
        
        let data = jsonString.data(using: .utf8)!
        do {
            if let jsonArray = try JSONSerialization.jsonObject(with: data, options : .allowFragments) as? Dictionary<String,Any>
            {
                self.socket!.emit("sendSignal", jsonArray)
            } else {
                print("bad json")
            }
        } catch let error as NSError {
            print(error)
        }
        
    }
    
    func webRTCClient(_ client: WebRTCClient, didChangeConnectionState state: RTCIceConnectionState) {
   
    }
    
    func webRTCClient(_ client: WebRTCClient, didReceiveData data: Data) {
        
    }
}
