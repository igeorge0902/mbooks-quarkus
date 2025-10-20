package com.jeet.booking;

import com.jeet.api.Purchase;
import com.jeet.service.BookingHandlerImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PurchaseDAO {

    @Inject
    private BookingHandlerImpl bookingHandler;

    public Purchase getBrainTreeCustomer(String uuid) throws InterruptedException {

        Purchase purchase = bookingHandler.getBrainTreeCustomerId(uuid);

        return purchase;
    }
}
