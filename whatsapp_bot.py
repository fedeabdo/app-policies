from flask import Flask, request
from twilio.twiml.messaging_response import MessagingResponse
from twilio.rest import Client
import os
from datetime import datetime, timedelta
from google_calendar_manager import GoogleCalendarManager

app = Flask(__name__)

# Configuración de Twilio (obtener de https://console.twilio.com)
TWILIO_ACCOUNT_SID = os.environ.get('TWILIO_ACCOUNT_SID', 'tu_account_sid')
TWILIO_AUTH_TOKEN = os.environ.get('TWILIO_AUTH_TOKEN', 'tu_auth_token')
TWILIO_WHATSAPP_NUMBER = os.environ.get('TWILIO_WHATSAPP_NUMBER', 'whatsapp:+14155238886')

# Inicializar cliente de Twilio
client = Client(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)

# Inicializar gestor de calendario
calendar_manager = GoogleCalendarManager()

# Almacenamiento temporal de conversaciones (en producción usa Redis o base de datos)
user_sessions = {}

@app.route('/webhook', methods=['POST'])
def webhook():
    """Endpoint que recibe los mensajes de WhatsApp"""
    incoming_msg = request.values.get('Body', '').strip().lower()
    from_number = request.values.get('From', '')
    
    # Crear respuesta
    resp = MessagingResponse()
    msg = resp.message()
    
    # Obtener o crear sesión del usuario
    if from_number not in user_sessions:
        user_sessions[from_number] = {'state': 'inicio'}
    
    session = user_sessions[from_number]
    
    # Máquina de estados para el flujo de conversación
    if session['state'] == 'inicio':
        response_text = (
            "¡Hola! 👋 Bienvenido al sistema de reservas.\n\n"
            "Para hacer una reserva, por favor indícame:\n"
            "📅 ¿Qué día deseas reservar? (ejemplo: 15/02/2026 o mañana)\n\n"
            "También puedes escribir 'horarios' para ver disponibilidad."
        )
        msg.body(response_text)
        session['state'] = 'esperando_fecha'
    
    elif session['state'] == 'esperando_fecha':
        if 'horarios' in incoming_msg or 'disponibilidad' in incoming_msg:
            # Mostrar horarios disponibles para hoy y mañana
            horarios = calendar_manager.obtener_horarios_disponibles(dias=2)
            response_text = "📅 Horarios disponibles:\n\n"
            for dia, slots in horarios.items():
                response_text += f"*{dia}*\n"
                if slots:
                    for slot in slots:
                        response_text += f"  ⏰ {slot}\n"
                else:
                    response_text += "  ❌ No hay horarios disponibles\n"
                response_text += "\n"
            response_text += "Por favor, indícame la fecha que prefieres."
            msg.body(response_text)
        else:
            # Procesar la fecha ingresada
            fecha = parse_fecha(incoming_msg)
            if fecha:
                session['fecha'] = fecha
                # Obtener horarios disponibles para esa fecha
                horarios = calendar_manager.obtener_horarios_dia(fecha)
                
                if horarios:
                    response_text = f"Para el día {fecha.strftime('%d/%m/%Y')}:\n\n"
                    response_text += "⏰ Horarios disponibles:\n"
                    for i, horario in enumerate(horarios, 1):
                        response_text += f"{i}. {horario}\n"
                    response_text += "\n¿Qué horario prefieres? (escribe el número)"
                    session['horarios_disponibles'] = horarios
                    session['state'] = 'esperando_horario'
                else:
                    response_text = f"😕 Lo siento, no hay horarios disponibles para el {fecha.strftime('%d/%m/%Y')}.\n\n"
                    response_text += "Por favor, elige otra fecha."
                
                msg.body(response_text)
            else:
                msg.body("No pude entender la fecha. Por favor usa el formato DD/MM/YYYY o escribe 'hoy' o 'mañana'.")
    
    elif session['state'] == 'esperando_horario':
        try:
            seleccion = int(incoming_msg) - 1
            horarios = session.get('horarios_disponibles', [])
            
            if 0 <= seleccion < len(horarios):
                session['horario'] = horarios[seleccion]
                response_text = (
                    f"✅ Perfecto!\n\n"
                    f"📅 Fecha: {session['fecha'].strftime('%d/%m/%Y')}\n"
                    f"⏰ Horario: {session['horario']}\n\n"
                    f"Por favor, indícame tu nombre para confirmar la reserva."
                )
                session['state'] = 'esperando_nombre'
                msg.body(response_text)
            else:
                msg.body("Por favor, selecciona un número válido de la lista.")
        except ValueError:
            msg.body("Por favor, escribe el número del horario que prefieres.")
    
    elif session['state'] == 'esperando_nombre':
        session['nombre'] = request.values.get('Body', '').strip()
        
        # Confirmar antes de crear la reserva
        response_text = (
            f"📋 *Resumen de tu reserva:*\n\n"
            f"👤 Nombre: {session['nombre']}\n"
            f"📅 Fecha: {session['fecha'].strftime('%d/%m/%Y')}\n"
            f"⏰ Horario: {session['horario']}\n\n"
            f"¿Confirmas la reserva? (escribe 'sí' o 'no')"
        )
        session['state'] = 'esperando_confirmacion'
        msg.body(response_text)
    
    elif session['state'] == 'esperando_confirmacion':
        if 'si' in incoming_msg or 'sí' in incoming_msg or 'confirmar' in incoming_msg:
            # Crear la reserva en Google Calendar
            resultado = calendar_manager.crear_reserva(
                fecha=session['fecha'],
                horario=session['horario'],
                nombre=session['nombre'],
                telefono=from_number
            )
            
            if resultado['success']:
                response_text = (
                    f"🎉 ¡Reserva confirmada!\n\n"
                    f"👤 {session['nombre']}\n"
                    f"📅 {session['fecha'].strftime('%d/%m/%Y')}\n"
                    f"⏰ {session['horario']}\n\n"
                    f"Te esperamos! 😊\n\n"
                    f"Recibirás un recordatorio antes de tu cita."
                )
            else:
                response_text = f"❌ Hubo un error al crear la reserva: {resultado['error']}\n\nPor favor, intenta nuevamente."
            
            msg.body(response_text)
            # Reiniciar sesión
            user_sessions[from_number] = {'state': 'inicio'}
        else:
            response_text = "❌ Reserva cancelada.\n\nEscribe cualquier mensaje para hacer una nueva reserva."
            msg.body(response_text)
            user_sessions[from_number] = {'state': 'inicio'}
    
    return str(resp)

def parse_fecha(texto):
    """Convertir texto a fecha"""
    hoy = datetime.now().date()
    
    if 'hoy' in texto:
        return datetime.combine(hoy, datetime.min.time())
    elif 'mañana' in texto or 'manana' in texto:
        return datetime.combine(hoy + timedelta(days=1), datetime.min.time())
    else:
        # Intentar parsear formato DD/MM/YYYY
        for formato in ['%d/%m/%Y', '%d-%m-%Y', '%d/%m/%y']:
            try:
                return datetime.strptime(texto, formato)
            except ValueError:
                continue
    
    return None

@app.route('/health', methods=['GET'])
def health():
    """Endpoint de salud"""
    return {'status': 'ok'}

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port, debug=True)
