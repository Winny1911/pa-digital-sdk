//
//  ViewController.swift
//  Sample
//
//  Created by Ricardo Dorta on 12/12/20.
//

import UIKit
import RTCSDK

class ViewController: UIViewController {
    
    private var helper:RTCSDKHelper? = nil;

    override func viewDidLoad() {
        super.viewDidLoad()
        
        helper = RTCSDKHelper()
        helper?.initModule(clientId:"eb16476f-62f4-4e8a-8a80-9f7c693a80bd", clientSecret: "96bdeb9f-1c1d-4e4d-aa96-08eb38f7f8bc", baseApiUri: "https://pa-digital-dev.ciadaconsulta.com.br/")
        
        // Do any additional setup after loading the view.
    }

    @IBAction func joinRoonTouchUpInside(_ sender: Any) {
        helper?.joinRoom(caller:self,roomId: "72bddb4987eda1b075598377af33fb62");
    }
    
    @IBAction func joinQueueTouchUpInside(_ sender: Any) {
        let person:PersonParameter = PersonParameter(name:"Fulano de Oliveira e Tal",
                                                     gender:"M",
                                                     dateOfBirth:"1988-10-10",
                                                     email:"renatonolo@hotmail.com",
                                                     cpf:"059.592.380-12",
                                                     phoneNumber:"993400828",
                                                     phoneArea: "11",
                                                     phoneCountry:"55")
        helper?.joinQueue(caller:self,person: person);
    }
}

