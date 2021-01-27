package com.rtcsdk

// result generated from /json

data class AccessToken(val id: Number?, val accessToken: String?, val ticketId: Number?, val apiUrl: String?)

data class TicketResponse(val socketConnected: Boolean?, val id: Number?, val username: String?, val updatedAt: String?, val createdAt: String?, val ticketCode: Number?, val queueTicketSequence: Number?, val previousQueueTicket: Number?, val vinculatedAt: Any?, val resend: Any?, val firstCallAt: Any?, val calledAt: Any?, val newCallAt: Any?, val unableAt: Any?, val unableReason: Any?, val establishmentId: Number?, val localTicketSequence: Any?, val countCalls: Any?, val patientName: String?, val usedAt: Any?, val startAttendanceAt: Any?, val finishAttendanceAt: Any?, val callUser: Any?, val priority: Any?, val ticketReturnedAt: Any?, val lastConnectedAt: Any?, val person: Person?, val doctorName: Any?, val medicalInsurance: MedicalInsurance?, val accessToken: AccessToken?)

data class Contact(val phoneNumber: String?, val phoneArea: String?, val phoneCountry: String?)

data class Documents(val cpf: String?, val passport: Any?, val rne: Any?)

data class MedicalInsurance(val uuid: String?, val id: Number?, val description: String?, val situation: String?, val insuranceCategoryId: String?, val categoryName: String?, val insurancePlanId: String?, val planName: String?, val initialQueueId: Any?, val initialQueue: Any?, val smsHeader: Any?, val smsBody: Any?)

data class Person(val personType: Number?, val updateAt: String?, val username: String?, val id: String?, val name: String?, val gender: String?, val dateOfBirth: String?, val documents: Documents?, val contact: Contact?)
