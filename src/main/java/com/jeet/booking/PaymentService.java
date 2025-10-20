package com.jeet.booking;

import com.braintreegateway.*;
import com.jeet.api.Purchase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PaymentService {
    private BraintreeGateway gateway;

    public PaymentService() {
        gateway = new BraintreeGateway(
                Environment.SANDBOX,
                "j3ndqpzrhy4gp2p7",
                "rzmyrsbswb3hwsmk",
                "37113dbf6dc015806f510e7e630755fb"
        );
    }

    public String getOrCreateCustomerId(String uuid, Purchase purchase) {

        if (purchase != null && purchase.getBrainTreeId() != null) {
            Customer customer = gateway.customer().find(purchase.getBrainTreeId());
            if (customer != null) {
                return customer.getId();
            }
        }

        CustomerRequest request = new CustomerRequest()
                .firstName("Mark")
                .lastName("Jones")
                .email("mark.jones@example.com")
                .phone("614-555-1234");

        Result<Customer> result = gateway.customer().create(request);
        return result.isSuccess() ? result.getTarget().getId() : null;
    }

    public Result<Transaction> processTransaction(TransactionRequest request) {
        return gateway.transaction().sale(request);
    }
}
