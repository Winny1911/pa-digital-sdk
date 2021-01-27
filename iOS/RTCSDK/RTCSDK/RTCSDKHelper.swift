//
//  RTCSDKHelper.swift
//  RTCSDK
//
//  Created by Swell on 12/12/20.
//

import Foundation
import UIKit

public class RTCSDKHelper {
    
    private var clientId:String? = nil;
    private var clientSecret:String? = nil;
    private var baseApiUri:String? = nil;
    
    
    public init() {}

    public func joinRoom(caller:UIViewController,roomId:String) {
        let callViewController = CallViewController(nibName: "CallView", bundle: Bundle(for: type(of: self)))
        callViewController.baseApiUri = self.baseApiUri
        callViewController.roomId = roomId
        caller.present(callViewController, animated: true, completion: nil)
    }
    
    public func joinQueue(caller:UIViewController, person:PersonParameter) {
        let queueViewController = QueueViewController(nibName: "QueueView", bundle: Bundle(for: type(of: self)))
        queueViewController.baseApiUri = self.baseApiUri
        queueViewController.clientSecret = self.clientSecret
        queueViewController.clientId = self.clientId
        queueViewController.person = TicketNumberRequest(person: TicketNumberPerson(name: person.name, gender: person.gender, dateOfBirth: person.dateOfBirth, email: person.email, documents: TicketNumberDocuments(cpf: person.cpf), contact: TicketNumberContact(phoneNumber: person.phoneNumber, phoneArea: person.phoneArea, phoneCountry: person.phoneCountry)))
        caller.present(queueViewController, animated: true, completion: nil)
    }

    public func initModule(clientId:String , clientSecret:String, baseApiUri: String) {
        self.clientId = clientId;
        self.clientSecret = clientSecret;
        self.baseApiUri = baseApiUri;
    }
    
}
