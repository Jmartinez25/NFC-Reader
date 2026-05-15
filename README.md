<h1 align="center">📱 Lector NFC — Tarjetas Bancarias</h1>

<p align="center">
  App Android que lee datos públicos de tarjetas bancarias contactless.<br/>
  Sin almacenamiento. Sin red. Sin terceros.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Material%203-757575?style=for-the-badge&logo=material-design&logoColor=white" />
  <img src="https://img.shields.io/badge/Lottie-00DDB3?style=for-the-badge&logo=airbnb&logoColor=white" />
  <img src="https://img.shields.io/badge/Android%20NFC-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
</p>

---

## 📸 Capturas de pantalla

### Inicio

<p align="center">
  <img src="https://github.com/user-attachments/assets/618115f1-4773-4022-b018-5ea08a87b170" width="22%" />
  &nbsp;&nbsp;
  <img src="https://github.com/user-attachments/assets/c21a0b0b-fbf6-4019-8796-e5a6cfbb80d4" width="22%" />
</p>

### Lectura

<p align="center">
  <img src="https://github.com/user-attachments/assets/602e85d7-ef7e-4465-98c5-e4b850fa4ea0" width="22%" />
  &nbsp;&nbsp;
  <img src="https://github.com/user-attachments/assets/923e5355-ce2f-4779-883a-c91d25558c2c" width="22%" />
  &nbsp;&nbsp;
  <img src="https://github.com/user-attachments/assets/ddcef106-47ee-4194-8df3-8b0d2d40b7e3" width="22%" />
</p>

### Resultado

<p align="center">
  <img src="https://github.com/user-attachments/assets/6163649f-29d6-438a-92b9-fd134f85fdab" width="22%" />
</p>

---

## ⚠️ Aviso importante — Lee esto antes de continuar

> **Este proyecto es exclusivamente educativo.** Fue desarrollado para aprender cómo funciona el protocolo EMV, la comunicación NFC entre dispositivos y tarjetas bancarias, y el desarrollo de apps Android con Jetpack Compose.

**Este proyecto NO:**
- Incentiva el robo, clonación o uso fraudulento de tarjetas
- Permite realizar pagos o transacciones
- Puede modificar ningún dato de la tarjeta
- Almacena, transmite ni comparte ningún dato leído

**Uso responsable:**
- Úsalo únicamente con tus **propias tarjetas**
- O con tarjetas de personas que te hayan dado su **autorización explícita**
- Nunca uses esta app sobre tarjetas de terceros sin su consentimiento
- El uso indebido de esta tecnología puede constituir un **delito en tu país**

> El autor no se hace responsable del mal uso que terceros puedan hacer de este código. Al clonar o usar este repositorio aceptas usarlo únicamente con fines educativos y de manera ética y legal.

---

## ✨ Qué hace

El lector NFC usa el chip NFC del teléfono para leer datos públicos de tarjetas bancarias contactless — los mismos datos que lee un datáfono antes de procesar un pago.

**Datos que puede leer:**
- PAN (número de tarjeta, enmascarado en pantalla)
- Fecha de vencimiento
- Nombre del titular (cuando está disponible)
- Franquicia (Visa, Mastercard, Amex, Discover, Maestro, Diners, UnionPay)

**Lo que NO puede leer:**
- CVV / CVC — nunca sale del chip
- PIN — nunca sale del chip
- Historial de transacciones — solo lo tiene el banco
- Datos de cuenta bancaria

---

## 🔒 Privacidad

| | |
|---|---|
| 🚫 Sin almacenamiento | Los datos nunca se guardan en el dispositivo |
| 🌐 Sin red | Nada sale del teléfono |
| 👁️ Solo lectura | La tarjeta no puede ser modificada ni clonada |
| 🔕 Sin terceros | Sin SDKs de analytics, sin publicidad |

---

## 📚 Qué aprendí construyendo esto

Este proyecto nació como ejercicio para entender:

- El protocolo **EMV** (Europay + Mastercard + Visa) y cómo funciona en tarjetas contactless
- Cómo se estructuran los comandos **APDU** (Application Protocol Data Unit)
- El formato **TLV** (Tag-Length-Value) usado para transmitir datos entre chip y terminal
- El directorio **PPSE** y los **AIDs** de cada franquicia
- Cómo Android expone el hardware NFC a través de `IsoDep`
- Desarrollo de interfaces con **Jetpack Compose** y sistema de diseño con temas dinámicos

---

## ⚙️ Cómo funciona

La app se comunica con el chip de la tarjeta usando el protocolo EMV sobre ISO 14443-4 (NFC-A / NFC-B):

```
1. SELECT PPSE        →  busca las apps de pago disponibles en la tarjeta
2. SELECT AID         →  selecciona la app (Visa, Mastercard, etc.)
3. GET PROC. OPTIONS  →  abre la transacción enviando los datos del PDOL
4. PARSEAR AFL        →  mapea dónde están los datos dentro de la tarjeta
5. READ RECORD        →  lee PAN, fecha y nombre desde los registros TLV
```

Toda la comunicación usa comandos APDU directamente a través de la API `IsoDep` de Android — **sin librerías NFC de terceros**.

---

## 🛠️ Tecnologías

| | Tecnología | Uso |
|---|-----------|-----|
| ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white) | Kotlin | Lenguaje principal |
| ![Compose](https://img.shields.io/badge/Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white) | Jetpack Compose | UI declarativa |
| ![Material](https://img.shields.io/badge/Material%203-757575?style=flat&logo=material-design&logoColor=white) | Material 3 | Sistema de diseño + temas claro/oscuro |
| ![Lottie](https://img.shields.io/badge/Lottie-00DDB3?style=flat&logo=airbnb&logoColor=white) | Lottie | Animaciones |
| ![NFC](https://img.shields.io/badge/NFC%20API-3DDC84?style=flat&logo=android&logoColor=white) | Android NFC API | Comunicación con la tarjeta vía `IsoDep` |
| ![Android](https://img.shields.io/badge/Min%20SDK%2023-3DDC84?style=flat&logo=android&logoColor=white) | Android 6.0+ | API 23 mínimo |

---

## 📁 Estructura del proyecto

```
app/src/main/java/com/martzlabs/myapplication/
├── MainActivity.kt          # Ciclo de vida NFC + estado de navegación
├── NfcCardReader.kt         # Lógica EMV + parser TLV + modelo CardData
└── ui/
    ├── WelcomeScreen.kt     # Pantalla de inicio con verificación de NFC
    ├── ScanScreen.kt        # Pantalla de lectura con 4 estados
    └── theme/
        ├── Theme.kt         # Esquemas de color claro / oscuro
        └── Color.kt         # Paleta de colores
```

---

## 🔄 Estados de la pantalla de lectura

```
Esperando  →  NFC activo, esperando que acerques una tarjeta
Inactivo   →  NFC desactivado, redirige a ajustes del sistema
Exitoso    →  Tarjeta leída, muestra PAN, fecha y franquicia
Error      →  Lectura fallida, opción de reintentar
```

---

## 💳 Franquicias soportadas

| Franquicia | Prefijo BIN |
|-----------|------------|
| Visa | 4 |
| Mastercard | 51–55, 2221–2720 |
| American Express | 34, 37 |
| Discover | 6011, 622126–622925, 644–649, 65 |
| Maestro | 6304, 6759, 6761–6763 |
| Diners Club | 300–305, 36, 38 |
| UnionPay | 62 |

---

## 🚀 Configuración

**Requisitos:**
- Android Studio Hedgehog o superior
- Dispositivo Android físico con NFC — el emulador no soporta NFC
- Android 6.0+ (API 23)

```bash
git clone https://github.com/Jmartinez25/nfc-card-reader.git
cd nfc-card-reader
```

Abre en Android Studio, espera que Gradle sincronice y ejecuta en un dispositivo físico con NFC activado.

---

## 🔑 Permisos

```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="true" />
```

No se requieren permisos adicionales.

---

## 📖 Referencias técnicas

- [Especificaciones EMV](https://www.emvco.com/emv-technologies/contact/)
- [ISO/IEC 7816-4](https://www.iso.org/standard/77180.html) — estructura de comandos APDU
- [ISO/IEC 14443](https://www.iso.org/standard/73599.html) — comunicación NFC con tarjetas

---

## 📄 Licencia

```
MIT License — Solo para uso educativo

Copyright (c) 2025

Se concede permiso para usar, copiar, modificar y distribuir este software
únicamente con fines educativos, de investigación o aprendizaje personal.
Queda expresamente prohibido su uso para actividades ilegales, fraudulentas
o que violen la privacidad de terceros.
```

---

<p align="center">
  <i>La curiosidad técnica y el aprendizaje son valiosos. Úsalos de forma ética y responsable.</i>
</p>
