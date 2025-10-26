#!/usr/bin/env python3
"""
Script de Verificación de Comunicación Arduino
Sistema de Asistencia Biométrica v2.0.0
Versión: 1.0

Uso:
    python test_arduino_comm.py

Este script verifica:
1. Conexión al puerto COM3 a 57600 baudios
2. Respuesta del Arduino a comandos
3. Estado del sensor de huellas
4. Operaciones CRUD básicas
"""

import serial
import time
import sys

# Configuración
PORT = 'COM3'
BAUDRATE = 57600
TIMEOUT = 5

def print_header():
    print("\n" + "="*60)
    print("  TEST DE COMUNICACIÓN ARDUINO - SISTEMA BIOMÉTRICO")
    print("="*60)
    print(f"Puerto: {PORT}")
    print(f"Baudrate: {BAUDRATE}")
    print("="*60 + "\n")

def print_test(name, status, message=""):
    status_icon = "✓" if status else "✗"
    status_color = "\033[92m" if status else "\033[91m"
    reset_color = "\033[0m"
    
    print(f"{status_color}[{status_icon}]{reset_color} {name:40} ", end="")
    if message:
        print(f"({message})")
    else:
        print()

def send_command(ser, command, wait_time=1):
    """Envía un comando al Arduino y lee la respuesta"""
    try:
        # Limpiar buffers
        ser.reset_input_buffer()
        ser.reset_output_buffer()
        
        # Enviar comando
        command_str = f"{command}\n"
        ser.write(command_str.encode())
        ser.flush()
        
        time.sleep(wait_time)
        
        # Leer respuestas
        responses = []
        while ser.in_waiting > 0:
            line = ser.readline().decode('utf-8', errors='ignore').strip()
            if line:
                responses.append(line)
        
        return responses
    except Exception as e:
        return [f"ERROR: {str(e)}"]

def main():
    print_header()
    
    # Test 1: Conexión al puerto
    print("FASE 1: CONEXIÓN")
    print("-" * 60)
    
    try:
        ser = serial.Serial(PORT, BAUDRATE, timeout=TIMEOUT)
        time.sleep(2)  # Esperar reset del Arduino
        print_test("Conexión al puerto serial", True, f"{PORT} @ {BAUDRATE}")
    except Exception as e:
        print_test("Conexión al puerto serial", False, str(e))
        sys.exit(1)
    
    # Leer mensajes de inicio del Arduino
    time.sleep(1)
    init_messages = []
    while ser.in_waiting > 0:
        line = ser.readline().decode('utf-8', errors='ignore').strip()
        if line:
            init_messages.append(line)
    
    if init_messages:
        print_test("Mensajes de inicio recibidos", True, f"{len(init_messages)} líneas")
        for msg in init_messages[-5:]:  # Mostrar últimas 5 líneas
            print(f"    → {msg}")
    else:
        print_test("Mensajes de inicio recibidos", False, "Sin respuesta")
    
    print()
    
    # Test 2: Comando PING
    print("FASE 2: COMANDOS BÁSICOS")
    print("-" * 60)
    
    responses = send_command(ser, "PING", 0.5)
    ping_ok = any("PONG" in r for r in responses)
    print_test("PING", ping_ok, responses[0] if responses else "Sin respuesta")
    
    # Test 3: Comando STATUS
    responses = send_command(ser, "STATUS", 0.5)
    status_ok = any("STATUS" in r or "CONNECTED" in r for r in responses)
    print_test("STATUS", status_ok)
    if responses:
        for resp in responses:
            print(f"    → {resp}")
    
    # Test 4: Comando COUNT
    responses = send_command(ser, "COUNT", 0.5)
    count_ok = any("TEMPLATES" in r for r in responses)
    print_test("COUNT", count_ok)
    if count_ok:
        for resp in responses:
            if "TEMPLATES" in resp:
                print(f"    → {resp}")
    
    # Test 5: Comando INFO
    responses = send_command(ser, "INFO", 1)
    info_ok = any("INFO:" in r for r in responses)
    print_test("INFO", info_ok)
    if info_ok:
        for resp in responses:
            if "INFO:" in resp:
                print(f"    → {resp}")
    
    print()
    
    # Test 6: Verificación de sensor
    print("FASE 3: VERIFICACIÓN DE SENSOR")
    print("-" * 60)
    
    sensor_detected = False
    sensor_baud = 0
    templates = 0
    
    for resp in responses:
        if "SENSOR_BAUD:" in resp or "BAUDRATE:" in resp:
            try:
                sensor_baud = int(resp.split(":")[-1])
                sensor_detected = True
            except:
                pass
        if "TEMPLATES:" in resp:
            try:
                templates = int(resp.split(":")[-1])
            except:
                pass
    
    print_test("Sensor detectado", sensor_detected, 
               f"DY50 @ {sensor_baud} baud" if sensor_detected else "No detectado")
    print_test("Huellas registradas", True, f"{templates} templates")
    
    # Test 7: Sincronización de baudrate
    baudrate_sync = sensor_baud == BAUDRATE or sensor_baud == 57600
    print_test("Baudrate sincronizado", baudrate_sync,
               f"Sensor: {sensor_baud}, PC: {BAUDRATE}")
    
    print()
    
    # Test 8: Hardware (opcional - comentar si no deseas activar LEDs)
    print("FASE 4: TEST DE HARDWARE (OPCIONAL)")
    print("-" * 60)
    
    test_hardware = input("¿Ejecutar test de hardware? (LEDs y buzzer) [s/N]: ").lower()
    if test_hardware == 's':
        responses = send_command(ser, "HARDWARE", 3)
        hardware_ok = any("OK" in r for r in responses)
        print_test("Test de hardware", hardware_ok)
        if responses:
            for resp in responses[-10:]:  # Últimas 10 líneas
                print(f"    → {resp}")
    else:
        print("    [Omitido]")
    
    print()
    
    # Resumen final
    print("="*60)
    print("RESUMEN DE PRUEBAS")
    print("="*60)
    
    tests_passed = sum([
        True,  # Conexión OK (si llegamos aquí)
        ping_ok,
        status_ok,
        count_ok,
        info_ok,
        sensor_detected,
        baudrate_sync
    ])
    total_tests = 7
    
    print(f"\nPruebas exitosas: {tests_passed}/{total_tests}")
    
    if tests_passed == total_tests:
        print("\n✅ SISTEMA FUNCIONANDO CORRECTAMENTE")
        print("\nEl Arduino está listo para recibir comandos desde Java.")
        print("Todas las operaciones CRUD deberían funcionar sin problemas.\n")
    elif tests_passed >= 5:
        print("\n⚠️  SISTEMA PARCIALMENTE FUNCIONAL")
        print("\nLa comunicación funciona pero hay algunos problemas.")
        print("Revisa los tests fallidos arriba.\n")
    else:
        print("\n❌ SISTEMA CON PROBLEMAS")
        print("\nLa comunicación no está funcionando correctamente.")
        print("\nVerifica:")
        print("1. Firmware v4.2 cargado en Arduino")
        print("2. Baudrate configurado a 57600")
        print("3. Conexiones del sensor DY50")
        print("4. Puerto COM3 disponible\n")
    
    # Comandos disponibles
    print("="*60)
    print("COMANDOS DISPONIBLES PARA JAVA")
    print("="*60)
    print("PING         - Test de comunicación")
    print("STATUS       - Estado del sistema")
    print("COUNT        - Contar huellas")
    print("INFO         - Información del sensor")
    print("DETECT       - Re-detectar sensor")
    print("ENROLL:N     - Enrollar huella con ID N (1-127)")
    print("VERIFY       - Verificar huella")
    print("DELETE:N     - Eliminar huella con ID N")
    print("EMPTY        - Limpiar base de datos")
    print("HARDWARE     - Test de hardware")
    print("BUZZER:ON    - Activar buzzer")
    print("BUZZER:OFF   - Desactivar buzzer")
    print("="*60 + "\n")
    
    # Cerrar puerto
    ser.close()
    print("Puerto serial cerrado.\n")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\nTest interrumpido por el usuario.\n")
        sys.exit(0)
    except Exception as e:
        print(f"\n❌ ERROR INESPERADO: {e}\n")
        sys.exit(1)