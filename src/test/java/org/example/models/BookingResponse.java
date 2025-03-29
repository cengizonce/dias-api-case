package org.example.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BookingResponse {
    @JsonProperty("bookingid")
    private final int bookingid;

    @JsonProperty("booking")
    private final BookingRequest booking;

    @JsonCreator
    public BookingResponse(
            @JsonProperty("bookingid") int bookingid,
            @JsonProperty("booking") BookingRequest booking) {
        this.bookingid = bookingid;
        this.booking = booking;
    }

    public int getBookingid() {
        return bookingid;
    }

    public BookingRequest getBooking() {
        return booking;
    }
}