package com.reservas.whatsapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservas.whatsapp.service.ConversationService;
import com.reservas.whatsapp.service.WhatsAppService;


@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookControllerTest {

    @Mock
    private ConversationService conversationService;

    @Mock
    private WhatsAppService whatsAppService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WhatsAppWebhookController controller;

    private String verifyToken = "test-token-123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "verifyToken", verifyToken);
    }

    @Test
    void testVerifyWebhookSuccess() {
        ResponseEntity<?> response = controller.verifyWebhook("subscribe", "challenge123", verifyToken);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("challenge123", response.getBody());
    }

    @Test
    void testVerifyWebhookInvalidToken() {
        ResponseEntity<?> response = controller.verifyWebhook("subscribe", "challenge123", "invalid-token");
        
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Forbidden", response.getBody());
    }

    @Test
    void testVerifyWebhookInvalidMode() {
        ResponseEntity<?> response = controller.verifyWebhook("invalid", "challenge123", verifyToken);
        
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
    //ToDo: Arreglar tests

    // @Test
    // void testReceiveMessageWithTextMessage() throws Exception {
    //     String payload = "{\"entry\":[{\"changes\":[{\"value\":{\"messages\":[{\"id\":\"msg123\",\"from\":\"1234567890\",\"type\":\"text\",\"text\":{\"body\":\"Hello\"}}]}}]}]}";
        
    //     ResponseEntity<?> response = controller.receiveMessage(payload);
        
    //     assertEquals(HttpStatus.OK, response.getStatusCode());
    //     verify(whatsAppService, timeout(2000)).markAsRead("msg123");
    // }

    // @Test
    // void testReceiveMessageWithInteractiveButton() throws Exception {
    //     String payload = "{\"entry\":[{\"changes\":[{\"value\":{\"messages\":[{\"id\":\"msg456\",\"from\":\"1234567890\",\"type\":\"interactive\",\"interactive\":{\"button_reply\":{\"id\":\"btn_1\",\"title\":\"Yes\"}}}]}}]}]}";
        
    //     ResponseEntity<?> response = controller.receiveMessage(payload);
        
    //     assertEquals(HttpStatus.OK, response.getStatusCode());
    //     verify(whatsAppService, timeout(2000)).markAsRead("msg456");
    // }

    // @Test
    // void testReceiveMessageWithoutMessages() throws Exception {
    //     String payload = "{\"entry\":[{\"changes\":[{\"value\":{\"messages\":[]}}]}]}";
        
    //     ResponseEntity<?> response = controller.receiveMessage(payload);
        
    //     assertEquals(HttpStatus.OK, response.getStatusCode());
    //     verify(whatsAppService, never()).markAsRead(anyString());
    // }

    // @Test
    // void testReceiveMessageWithoutChanges() throws Exception {
    //     String payload = "{\"entry\":[{\"changes\":[]}]}";
        
    //     ResponseEntity<?> response = controller.receiveMessage(payload);
        
    //     assertEquals(HttpStatus.OK, response.getStatusCode());
    // }

    @Test
    void testReceiveMessageWithInvalidJson() {
        String invalidPayload = "{invalid json}";
        
        ResponseEntity<?> response = controller.receiveMessage(invalidPayload);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testHealthEndpoint() {
        ResponseEntity<?> response = controller.health();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\":\"ok\"}", response.getBody());
    }

    @Test
    void testGetLastResponse() {
        String phoneNumber = "1234567890";
        String expectedResponse = "Bot response";
        
        when(conversationService.getLastResponse(phoneNumber)).thenReturn(expectedResponse);
        
        ResponseEntity<?> response = controller.getLastResponse(phoneNumber);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(conversationService).getLastResponse(phoneNumber);
    }

    // @Test
    // void testSyncCalendarReservations() {
    //     ConversationService.SyncResult syncResult = new ConversationService.SyncResult();
    //     syncResult.added = 5;
    //     syncResult.skipped = 3;
    //     syncResult.errors = 0;
    //     syncResult.message = "✅ Sincronización completada:\n- 5 nuevas reservas importadas\n- 3 reservas ya existentes\n- 0 errores";
        
    //     when(conversationService.syncReservationsFromCalendar()).thenReturn(syncResult);
        
    //     ResponseEntity<?> response = controller.syncCalendarReservations();
        
    //     assertEquals(HttpStatus.OK, response.getStatusCode());
    //     verify(conversationService).syncReservationsFromCalendar();
    // }
}