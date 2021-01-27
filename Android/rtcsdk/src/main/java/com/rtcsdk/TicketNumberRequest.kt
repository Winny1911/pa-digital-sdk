package com.rtcsdk

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

// result generated from /json

@Parcelize
data class TicketNumberRequest(val person: TicketNumberPerson?) : Parcelable

@Parcelize
data class TicketNumberContact(val phoneNumber: String?, val phoneArea: String?, val phoneCountry: String?): Parcelable

@Parcelize
data class TicketNumberDocuments(val cpf: String?): Parcelable

@Parcelize
data class TicketNumberPerson(val name: String?, val gender: String?, val dateOfBirth: String?, val email: String?, val documents: TicketNumberDocuments, val contact: TicketNumberContact): Parcelable
