"""
Script de prueba para verificar la integración con Google Calendar
"""

from google_calendar_manager import GoogleCalendarManager
from datetime import datetime, timedelta

def main():
    print("=" * 50)
    print("Test de Google Calendar Manager")
    print("=" * 50)
    print()
    
    # Inicializar el manager
    print("Inicializando conexión con Google Calendar...")
    try:
        manager = GoogleCalendarManager()
        print("✅ Conexión exitosa!\n")
    except Exception as e:
        print(f"❌ Error al conectar: {e}")
        return
    
    # Obtener horarios disponibles para hoy
    print("-" * 50)
    print("1. Horarios disponibles HOY:")
    print("-" * 50)
    hoy = datetime.now()
    horarios_hoy = manager.obtener_horarios_dia(hoy)
    
    if horarios_hoy:
        for horario in horarios_hoy:
            print(f"  ⏰ {horario}")
    else:
        print("  ❌ No hay horarios disponibles")
    print()
    
    # Obtener horarios para los próximos 3 días
    print("-" * 50)
    print("2. Disponibilidad próximos 3 días:")
    print("-" * 50)
    horarios = manager.obtener_horarios_disponibles(dias=3)
    
    for dia, slots in horarios.items():
        print(f"\n📅 {dia}:")
        if slots:
            for slot in slots[:5]:  # Mostrar solo los primeros 5
                print(f"  ⏰ {slot}")
            if len(slots) > 5:
                print(f"  ... y {len(slots) - 5} más")
        else:
            print("  ❌ Sin horarios disponibles")
    print()
    
    # Obtener eventos del día
    print("-" * 50)
    print("3. Eventos existentes HOY:")
    print("-" * 50)
    eventos = manager.obtener_eventos_dia(hoy)
    
    if eventos:
        for evento in eventos:
            inicio = evento['start'].get('dateTime', evento['start'].get('date'))
            titulo = evento.get('summary', 'Sin título')
            print(f"  📌 {inicio}: {titulo}")
    else:
        print("  ℹ️  No hay eventos programados")
    print()
    
    # Prueba de creación de reserva (comentada por defecto)
    print("-" * 50)
    print("4. Test de creación de reserva (DESHABILITADO)")
    print("-" * 50)
    print("Para probar la creación de reservas, descomenta el código en test_calendar.py")
    print()
    
    """
    # Descomentar para probar creación de reserva
    print("Creando reserva de prueba...")
    resultado = manager.crear_reserva(
        fecha=datetime.now() + timedelta(days=1),
        horario="10:00",
        nombre="Test Usuario",
        telefono="+598123456789"
    )
    
    if resultado['success']:
        print(f"✅ Reserva creada exitosamente!")
        print(f"   ID del evento: {resultado['event_id']}")
        print(f"   Link: {resultado['link']}")
    else:
        print(f"❌ Error: {resultado['error']}")
    """
    
    print("=" * 50)
    print("Test completado")
    print("=" * 50)

if __name__ == '__main__':
    main()
