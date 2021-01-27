//
//  Structs.swift
//  RTCSDK
//
//  Created by Ricardo Dorta on 12/12/20.
//

import Foundation

struct TokenResponse: Codable {
    let token: String
}

struct TokenRequest: Codable {
    let clientId: String
    let secret: String
}

struct TicketNumberRequest: Codable {
    let person: TicketNumberPerson
}

struct TicketNumberPerson: Codable {
    let name:String
    let gender:String
    let dateOfBirth:String
    let email:String
    let documents:TicketNumberDocuments
    let contact: TicketNumberContact
}

struct TicketNumberDocuments: Codable {
    let cpf:String
}

struct TicketNumberContact: Codable {
    let phoneNumber:String
    let phoneArea:String
    let phoneCountry:String
}

struct TicketNumberResponse : Codable {
    let ticket:Int
    let ticketId:Int
    let url:String
}

public class PersonParameter {
    
    let name:String
    let gender:String
    let dateOfBirth:String
    let email:String
    let cpf:String
    let phoneNumber:String
    let phoneArea:String
    let phoneCountry:String
    
    public init (name:String, gender:String, dateOfBirth:String, email:String, cpf:String, phoneNumber:String, phoneArea:String, phoneCountry:String) {
        self.name = name
        self.gender = gender
        self.dateOfBirth = dateOfBirth
        self.email = email
        self.cpf = cpf
        self.phoneNumber = phoneNumber
        self.phoneArea = phoneArea
        self.phoneCountry = phoneCountry
    }
    
}


