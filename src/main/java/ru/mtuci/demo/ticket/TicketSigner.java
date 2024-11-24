package ru.mtuci.demo.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TicketSigner {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String serializeTicket(Ticket ticket) {
        try {
            return objectMapper.writeValueAsString(ticket);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сериализации объекта Ticket", e);
        }
    }
}
