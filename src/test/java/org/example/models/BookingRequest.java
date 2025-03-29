package org.example.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Objects;

public class BookingRequest {
    @JsonProperty("firstname")
    private final String firstname;

    @JsonProperty("lastname")
    private final String lastname;

    @JsonProperty("totalprice")
    private final int totalprice;

    @JsonProperty("depositpaid")
    private final boolean depositpaid;

    @JsonProperty("bookingdates")
    private final BookingDates bookingdates;

    @JsonProperty("additionalneeds")
    private final String additionalneeds;

    @JsonCreator
    public BookingRequest(
            @JsonProperty("firstname") String firstname,
            @JsonProperty("lastname") String lastname,
            @JsonProperty("totalprice") int totalprice,
            @JsonProperty("depositpaid") boolean depositpaid,
            @JsonProperty("bookingdates") BookingDates bookingdates,
            @JsonProperty("additionalneeds") String additionalneeds) {

        this.firstname = Objects.requireNonNull(firstname, "firstname cannot be null");
        this.lastname = Objects.requireNonNull(lastname, "lastname cannot be null");
        this.totalprice = totalprice;
        this.depositpaid = depositpaid;
        this.bookingdates = Objects.requireNonNull(bookingdates, "bookingdates cannot be null");
        this.additionalneeds = additionalneeds != null ? additionalneeds : "Breakfast";

        if (bookingdates.getCheckin().after(bookingdates.getCheckout())) {
            throw new IllegalArgumentException("Checkin date cannot be after checkout date");
        }
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public int getTotalprice() {
        return totalprice;
    }

    public boolean isDepositpaid() {
        return depositpaid;
    }

    public BookingDates getBookingdates() {
        return bookingdates;
    }

    public String getAdditionalneeds() {
        return additionalneeds;
    }
}