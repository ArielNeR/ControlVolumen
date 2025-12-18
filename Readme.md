# 🎚️ ControlVolumen

Aplicación Android que superpone un **control deslizante de volumen multimedia** sobre cualquier pantalla, permitiendo ajustar el volumen sin salir de la app que estés usando.

---

## 📱 Características

- Barra flotante (overlay) que aparece sobre cualquier aplicación  
- Control exclusivo del volumen de multimedia del sistema  
- Se muestra / oculta con un gesto de deslizamiento  
- Compatible desde Android 5.0 (API 21)

---

## 🛠️ Stack técnico

| Tecnología | Uso |
|------------|-----|
| Kotlin | Lenguaje principal |
| Android SDK | Framework nativo |
| Gradle Kotlin DSL | Automatización de builds |
| Material Design | UI / UX |
| Service + `SYSTEM_ALERT_WINDOW` | Overlay flotante |

---

## 📂 Estructura del proyecto
ControlVolumen/
├── app/src/main/java/com/example/controlvolumen/
│   ├── MainActivity.kt
│   └── VolumeOverlayService.kt
├── app/src/main/res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   └── floating_volume_control.xml
│   └── values/strings.xml
├── build.gradle.kts
└── settings.gradle.kts
Copy

---

## 📸 Capturas de pantalla

<p align="center">
  <img src="captures/pantallaprincipal.jpeg" width="220" alt="Pantalla principal"/>
  <img src="captures/modocantador.jpeg" width="220" alt="Modo cantador"/>
  <img src="captures/modojugador.jpeg" width="220" alt="Modo jugador"/>
</p>

---

## ⬇️ APK de prueba

1. Descarga el APK:  
   👉 [app-debug.apk](https://github.com/ArielNeR/BingoRoyale/raw/master/apk/app-debug.apk)
2. Activa “Orígenes desconocidos” en Ajustes → Seguridad.
3. Abre el APK y confía la instalación.

> ⚠️ Android advertirá sobre instalaciones externas a Play Protect; es normal en APKs de desarrollo.

---

## 🚀 Compilar desde código

```bash
# Clonar
git clone https://github.com/ArielNeR/ControlVolumen.git
cd ControlVolumen

# Importar en Android Studio (Chipmunk o superior)
# Build → Run en dispositivo/emulador
🔐 Permisos
Table
Copy
Permiso	¿Por qué?
SYSTEM_ALERT_WINDOW	Dibujar la barra flotante sobre otras apps
La app redirige automáticamente a la pantalla de permisos si no está concedido.
▶️ Uso rápido
Abre la app y pulsa “Iniciar Barra de Volumen”.
Concede el permiso de superposición.
Desliza desde el borde para mostrar/ocultar el control.
¡Listo! Ajusta el volumen sin cerrar tu juego o reproductor.
📌 Notas de desarrollo
El servicio VolumeOverlayService vive en 1º plano para evitar que el sistema lo mate.
El control se oculta 3 s después de soltar la barra (configurable).
Pensado como plantilla: puedes agregar temas, más flujos de audio o widgets extra.
📄 Licencia
MIT © 2024 – puedes usar, modificar y distribuir el proyecto libremente.
