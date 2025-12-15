# 🎚️ ControlVolumen

Aplicación Android que muestra un **control de volumen flotante** sobre cualquier app del sistema, permitiendo ajustar el volumen multimedia mediante una barra deslizante tipo overlay.

## 📱 Características

- Barra de volumen flotante (overlay)
- Control del volumen multimedia del sistema
- Deslizar para mostrar u ocultar el control
- Funciona sobre cualquier aplicación
- Compatible desde Android 5.0 (API 21)

## 🛠️ Tecnologías

- **Kotlin**
- **Android SDK**
- **Gradle (Kotlin DSL)**
- **Material Design**
- **Servicios Android (Service)**
- **SYSTEM_ALERT_WINDOW (overlay)**

## 📂 Estructura del proyecto

ControlVolumen/
├── app/
│ ├── src/main/java/com/example/controlvolumen/
│ │ ├── MainActivity.kt
│ │ ├── VolumeOverlayService.kt
│ ├── res/
│ │ ├── layout/
│ │ │ ├── activity_main.xml
│ │ │ └── floating_volume_control.xml
│ │ └── values/
│ │ └── strings.xml
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts

bash
Copiar código

## 🚀 Instalación y ejecución

1. Clona el repositorio:
   ```bash
   git clone https://github.com/tu-usuario/ControlVolumen.git
Abre el proyecto en Android Studio

Ejecuta la app en un dispositivo físico o emulador

🔐 Permisos requeridos
La aplicación solicita el permiso:

SYSTEM_ALERT_WINDOW
Necesario para mostrar la barra flotante sobre otras aplicaciones.

Al iniciar la app, se redirige automáticamente a la pantalla de permisos si no está concedido.

▶️ Uso
Abre la aplicación

Pulsa “Iniciar Barra de Volumen”

Concede el permiso de superposición

Desliza desde el borde para mostrar el control de volumen

Ajusta el volumen con la barra

📌 Notas
El servicio se ejecuta en segundo plano

El control puede ocultarse y mostrarse mediante gestos

Pensado como base para personalización o ampliación

📄 Licencia
Este proyecto está bajo la licencia MIT.
Puedes usarlo, modificarlo y distribuirlo libremente.

✍️ Desarrollado en Kotlin para Android