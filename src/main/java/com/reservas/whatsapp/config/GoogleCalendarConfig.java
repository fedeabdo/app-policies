package com.reservas.whatsapp.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Configuration
@Slf4j
public class GoogleCalendarConfig {
    
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    
    @Value("${google.calendar.credentials-file}")
    private String credentialsFile;
    
    @Value("${google.calendar.tokens-directory}")
    private String tokensDirectory;
    
    @Value("${google.calendar.application-name}")
    private String applicationName;
    
    @Bean
    public Calendar googleCalendar() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new Calendar.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
                .setApplicationName(applicationName)
                .build();
    }
    
    private Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        // Cargar credenciales del cliente
        InputStream in = GoogleCalendarConfig.class.getResourceAsStream("/" + credentialsFile);
        if (in == null) {
            throw new FileNotFoundException("Archivo de credenciales no encontrado: " + credentialsFile);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        
        // Construir flujo de autorización
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectory)))
                .setAccessType("offline")
                .build();
        
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        
        log.info("Credenciales de Google Calendar cargadas exitosamente");
        return credential;
    }
}
