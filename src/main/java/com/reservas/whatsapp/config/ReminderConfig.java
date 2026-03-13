package com.reservas.whatsapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuración de recordatorios.
 * Permite definir múltiples recordatorios con diferentes tiempos de anticipación.
 * 
 * Ejemplo de configuración en application.properties:
 * 
 * reminder.enabled=true
 * reminder.template-name=reserva_recordatorio
 * reminder.times[0].minutes-before=1440  # 24 horas antes
 * reminder.times[0].enabled=true
 * reminder.times[1].minutes-before=60    # 1 hora antes
 * reminder.times[1].enabled=true
 * reminder.times[2].minutes-before=15    # 15 minutos antes
 * reminder.times[2].enabled=false
 * 
 * # Configuración de parámetros por template
 * reminder.templates.recordatorio_reserva.params=customer_name,staff_name,date,time
 * reminder.templates.reserva_recordatorio.params=customer_name,date,time,staff_name,time_remaining
 */
@Configuration
@ConfigurationProperties(prefix = "reminder")
@Data
public class ReminderConfig {
    
    /**
     * Habilita o deshabilita el sistema de recordatorios
     */
    private boolean enabled = true;
    
    /**
     * Nombre del template de WhatsApp aprobado por Meta para recordatorios
     */
    private String templateName = "reserva_recordatorio";
    
    /**
     * Idioma del template (es, en, pt, etc.)
     */
    private String templateLanguage = "es";
    
    /**
     * Lista de tiempos de recordatorio configurables
     */
    private List<ReminderTime> times = new ArrayList<>();
    
    /**
     * Intervalo en minutos para revisar reservas pendientes de recordatorio
     */
    private int checkIntervalMinutes = 5;
    
    /**
     * Configuración de parámetros por template.
     * Mapea nombre_template -> configuración de parámetros
     */
    private Map<String, TemplateParams> templates = new HashMap<>();
    
    /**
     * Configuración individual de cada recordatorio
     */
    @Data
    public static class ReminderTime {
        /**
         * Minutos antes de la cita para enviar el recordatorio
         */
        private int minutesBefore;
        
        /**
         * Si este recordatorio está habilitado
         */
        private boolean enabled = true;
        
        /**
         * Nombre del template específico para este recordatorio (opcional)
         * Si no se especifica, usa el template por defecto
         */
        private String templateName;
        
        /**
         * Descripción amigable del recordatorio
         */
        private String description;
    }
    
    /**
     * Configuración de parámetros para un template específico.
     * Define qué campos se envían y en qué orden.
     * 
     * Campos disponibles:
     * - customer_name: Nombre del cliente
     * - staff_name: Nombre del profesional  
     * - date: Fecha formateada (ej: "viernes 20 de marzo")
     * - time: Hora (ej: "15:00")
     * - time_remaining: Tiempo restante (ej: "24 horas", "2 horas")
     */
    @Data
    public static class TemplateParams {
        /**
         * Lista ordenada de parámetros a enviar.
         * El orden corresponde a {{1}}, {{2}}, {{3}}, etc. del template.
         */
        private List<String> params = new ArrayList<>();
        
        /**
         * Idioma del template (ej: es, es_AR, es_UY, en, pt_BR).
         * Si no se especifica, usa el idioma por defecto de ReminderConfig.
         */
        private String language;
    }
    
    /**
     * Obtiene solo los recordatorios habilitados
     */
    public List<ReminderTime> getEnabledReminders() {
        return times.stream()
                .filter(ReminderTime::isEnabled)
                .toList();
    }
    
    /**
     * Obtiene el nombre del template para un recordatorio específico
     */
    public String getTemplateForReminder(ReminderTime reminder) {
        return reminder.getTemplateName() != null ? 
               reminder.getTemplateName() : 
               this.templateName;
    }
    
    /**
     * Obtiene el idioma para un template específico.
     * Busca en orden: configuración del template -> idioma global por defecto
     */
    public String getLanguageForTemplate(String templateName) {
        TemplateParams templateParams = templates.get(templateName);
        if (templateParams != null && templateParams.getLanguage() != null) {
            return templateParams.getLanguage();
        }
        return this.templateLanguage;
    }
}
