//
//  QueueViewController.swift
//  RTCSDK
//
//  Created by Ricardo Dorta on 12/12/20.
//

import UIKit
import RestEssentials
import SocketIO

class QueueViewController : UIViewController {
    
    var clientId:String? = nil
    var clientSecret:String? = nil
    var baseApiUri:String? = nil
    var person:TicketNumberRequest? = nil

    var manager:CustomSocketManager? = nil
    
    @IBOutlet weak var waitingLabel: UILabel!
    
    
    
   
    override func viewDidLoad() {
        super.viewDidLoad()
        
       
        
        let urlString = "\(self.baseApiUri!)api/"
        guard let rest = RestController.make(urlString: urlString) else {
            print("Bad URL")
            return
        }
        
        
        
        let tokenRequest = TokenRequest(clientId: self.clientId!, secret: self.clientSecret!)
        rest.post(tokenRequest, at:"auth/login", responseType:TokenResponse.self) { result, httpResponse in
            do {
                let response = try result.value() // response is of type HttpBinResponse
                let token = response.token;
                var options = RestOptions()
                options.httpHeaders = ["Authorization":"Bearer \(token)"]
                rest.post(self.person, at:"client/pa/ticket/generate", responseType:TicketNumberResponse.self, options: options) { result, httpResponse in
                    do {
                        let ticket = try result.value() // response is of type HttpBinResponse
                        print(ticket.ticketId)
                        
                        
                        self.manager = CustomSocketManager(socketURL: URL(string: self.baseApiUri!)!, config: [.forceWebsockets(true),.log(true), .connectParams(["ticketId": "\(ticket.ticketId)"])])
                        
                        let socket = self.manager!.socket(forNamespace: "/client")
                        
                        socket.on("client.connected") {data, ack in
                            print("connected")
                        }
                        
                        socket.on("client.queueInfo") {data, ack in
                            guard let jsonObject = data[0] as? Dictionary<String,AnyObject> else { return }
                            let msg:String = "Aguardando \(jsonObject["ticketsBefore"]!) pessoas na sua frente"
                            self.waitingLabel.text = msg;
                            print("queueInfo")
                        }
                        
                        socket.on("client.callTicket") {data, ack in
                            print("callTicket")
                            guard let jsonObject = data[0] as? Dictionary<String,AnyObject> else { return }
                            
                            let callViewController = CallViewController(nibName: "CallView", bundle: Bundle(for: type(of: self)))
                            callViewController.baseApiUri = self.baseApiUri
                            callViewController.roomId = jsonObject["roomId"] as! String?
                            self.present(callViewController, animated: true, completion: nil)
                            socket.disconnect()
                        }
                        
                        socket.connect()
                        
                        
                        
                    } catch {
                        print("Error performing GET: \(error)")
                    }
                }
                
                
                
            } catch {
                print("Error performing GET: \(error)")
            }
        }
        
        
        
    }
    
    
}
