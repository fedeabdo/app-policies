import os.path
from datetime import datetime, timedelta
from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError

# Si modificas los scopes, elimina el archivo token.json
SCOPES = ['https://www.googleapis.com/auth/calendar']

class GoogleCalendarManager:
    def __init__(self, calendar_id='primary'):
        """
        Inicializa el gestor de Google Calendar
        
        Args:
            calendar_id: ID del calendario a usar (default: 'primary' para calendario principal)
        """
        self.calendar_id = calendar_id
        self.service = self._authenticate()
        
        # Configuración de horarios de trabajo
        self.hora_inicio = 9  # 9 AM
        self.hora_fin = 18    # 6 PM
        self.duracion_reserva = 60  # minutos por reserva
    
    def _authenticate(self):
        """Autenticación con Google Calendar API"""
        creds = None
        
        # El archivo token.json almacena los tokens de acceso y refresh del usuario
        if os.path.exists('token.json'):
            creds = Credentials.from_authorized_user_file('token.json', SCOPES)
        
        # Si no hay credenciales válidas, hacer login
        if not creds or not creds.valid:
            if creds and creds.expired and creds.refresh_token:
                creds.refresh(Request())
            else:
                flow = InstalledAppFlow.from_client_secrets_file(
                    'credentials.json', SCOPES)
                creds = flow.run_local_server(port=0)
            
            # Guardar credenciales para la próxima vez
            with open('token.json', 'w') as token:
                token.write(creds.to_json())
        
        return build('calendar', 'v3', credentials=creds)
    
    def obtener_eventos_dia(self, fecha):
        """
        Obtiene todos los eventos de un día específico
        
        Args:
            fecha: datetime object del día
        
        Returns:
            Lista de eventos
        """
        try:
            # Inicio y fin del día
            time_min = fecha.replace(hour=0, minute=0, second=0).isoformat() + 'Z'
            time_max = fecha.replace(hour=23, minute=59, second=59).isoformat() + 'Z'
            
            events_result = self.service.events().list(
                calendarId=self.calendar_id,
                timeMin=time_min,
                timeMax=time_max,
                singleEvents=True,
                orderBy='startTime'
            ).execute()
            
            return events_result.get('items', [])
        
        except HttpError as error:
            print(f'Error al obtener eventos: {error}')
            return []
    
    def obtener_horarios_dia(self, fecha):
        """
        Obtiene los horarios disponibles para un día específico
        
        Args:
            fecha: datetime object del día
        
        Returns:
            Lista de strings con horarios disponibles (ej: ["09:00", "10:00", ...])
        """
        # Obtener eventos existentes
        eventos = self.obtener_eventos_dia(fecha)
        
        # Crear lista de todos los horarios posibles
        horarios_posibles = []
        hora_actual = self.hora_inicio
        
        while hora_actual < self.hora_fin:
            horarios_posibles.append(f"{hora_actual:02d}:00")
            hora_actual += 1
        
        # Filtrar horarios ocupados
        horarios_disponibles = []
        
        for horario in horarios_posibles:
            hora, minuto = map(int, horario.split(':'))
            inicio_slot = fecha.replace(hour=hora, minute=minuto, second=0)
            fin_slot = inicio_slot + timedelta(minutes=self.duracion_reserva)
            
            # Verificar si hay conflicto con algún evento
            conflicto = False
            for evento in eventos:
                evento_inicio = datetime.fromisoformat(evento['start'].get('dateTime', evento['start'].get('date')))
                evento_fin = datetime.fromisoformat(evento['end'].get('dateTime', evento['end'].get('date')))
                
                # Remover timezone info para comparación
                if evento_inicio.tzinfo:
                    evento_inicio = evento_inicio.replace(tzinfo=None)
                if evento_fin.tzinfo:
                    evento_fin = evento_fin.replace(tzinfo=None)
                
                # Verificar solapamiento
                if not (fin_slot <= evento_inicio or inicio_slot >= evento_fin):
                    conflicto = True
                    break
            
            if not conflicto:
                horarios_disponibles.append(horario)
        
        return horarios_disponibles
    
    def obtener_horarios_disponibles(self, dias=7):
        """
        Obtiene horarios disponibles para los próximos N días
        
        Args:
            dias: Número de días a consultar
        
        Returns:
            Dict con fecha como key y lista de horarios disponibles como value
        """
        resultado = {}
        hoy = datetime.now()
        
        for i in range(dias):
            fecha = hoy + timedelta(days=i)
            dia_texto = fecha.strftime('%A %d/%m')
            horarios = self.obtener_horarios_dia(fecha)
            resultado[dia_texto] = horarios
        
        return resultado
    
    def crear_reserva(self, fecha, horario, nombre, telefono=""):
        """
        Crea una reserva en Google Calendar
        
        Args:
            fecha: datetime object
            horario: string (ej: "09:00")
            nombre: nombre del cliente
            telefono: teléfono del cliente (opcional)
        
        Returns:
            Dict con 'success' (bool) y 'event_id' o 'error'
        """
        try:
            # Parsear horario
            hora, minuto = map(int, horario.split(':'))
            inicio = fecha.replace(hour=hora, minute=minuto, second=0)
            fin = inicio + timedelta(minutes=self.duracion_reserva)
            
            # Crear evento
            evento = {
                'summary': f'Reserva - {nombre}',
                'description': f'Cliente: {nombre}\nTeléfono: {telefono}',
                'start': {
                    'dateTime': inicio.isoformat(),
                    'timeZone': 'America/Montevideo',  # Ajusta según tu zona horaria
                },
                'end': {
                    'dateTime': fin.isoformat(),
                    'timeZone': 'America/Montevideo',
                },
                'reminders': {
                    'useDefault': False,
                    'overrides': [
                        {'method': 'popup', 'minutes': 60},
                        {'method': 'popup', 'minutes': 24 * 60},
                    ],
                },
            }
            
            evento_creado = self.service.events().insert(
                calendarId=self.calendar_id,
                body=evento
            ).execute()
            
            return {
                'success': True,
                'event_id': evento_creado['id'],
                'link': evento_creado.get('htmlLink')
            }
        
        except HttpError as error:
            return {
                'success': False,
                'error': str(error)
            }
    
    def cancelar_reserva(self, event_id):
        """
        Cancela una reserva existente
        
        Args:
            event_id: ID del evento a cancelar
        
        Returns:
            Dict con 'success' (bool)
        """
        try:
            self.service.events().delete(
                calendarId=self.calendar_id,
                eventId=event_id
            ).execute()
            
            return {'success': True}
        
        except HttpError as error:
            return {
                'success': False,
                'error': str(error)
            }

# Ejemplo de uso
if __name__ == '__main__':
    manager = GoogleCalendarManager()
    
    # Obtener horarios disponibles
    print("Horarios disponibles:")
    horarios = manager.obtener_horarios_disponibles(dias=3)
    for dia, slots in horarios.items():
        print(f"\n{dia}:")
        for slot in slots:
            print(f"  - {slot}")
