package com.jeet.booking;

import com.jeet.api.Seats;
import com.jeet.api.Ticket;
import com.jeet.service.BookingHandlerImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class TicketService {
    @Inject
    private BookingHandlerImpl ticketDAO;

    public List<Ticket> reserveTickets(int screeningDateId, String[] seats, String uuid, String orderId) throws InterruptedException {
        return ticketDAO.returnTickets(screeningDateId, Arrays.asList(seats), uuid, orderId);
    }

    public List<Seats> getUpdatedSeats(int screeningDateId) throws InterruptedException {
        return ticketDAO.returnUpdatedseats(screeningDateId);
    }

    public void rollbackTickets(List<Integer> tickets, int purchaseId) throws InterruptedException {
        if (!tickets.isEmpty()) {
            ticketDAO.deleteTicket(tickets, purchaseId);
        }
    }
}