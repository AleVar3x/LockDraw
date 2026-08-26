# LockDraw 🎨❤️
### Disegna in tempo reale sul Lockscreen e sullo schermo con il tuo partner

**LockDraw** è un'applicazione Android moderna sviluppata in **Kotlin** e **Jetpack Compose** che permette alle coppie e agli amici di disegnare insieme in tempo reale direttamente sopra il lockscreen e le app, condividendo messaggi, note d'amore, doodle e sticker.

---

## ✨ Funzionalità Principali

### 💑 Sincronizzazione in Tempo Reale
- **Connessione di Coppia:** Associa facilmente i dispositivi tramite codice univoco o scansione QR.
- **Disegno Live:** Guarda i tratti del tuo partner apparire istantaneamente sul tuo schermo mentre disegna.
- **Presenza Live & Reazioni:** Visualizza quando il partner è online e invia cuori e reazioni fluttuanti in tempo reale.

### 🖌️ Strumenti di Disegno & Pennelli Avanzati
- **Pennelli Multipli:**
  - ✏️ **Penna:** Tratto fluido e preciso.
  - 📝 **Matita:** Tratto textured sfumato.
  - 🖍️ **Evidenziatore:** Trasparenza perfetta per evidenziare.
  - ✨ **Neon Glow:** Tratto luminoso e pulsante con bagliore neon.
  - 🌈 **Arcobaleno:** Sfumatura dinamica multicolore.
  - 🟣 **Puntini:** Linea tratteggiata stilizzata.
  - 🧼 **Gomma:** Strumento di cancellazione evidenziato in verde con raggio regolabile.
- **Stili di Tratto Dinamici:**
  - 🌊 **Onda (Wave):** Animazione sinusoidale sul tratto.
  - 💓 **Pulsante (Pulsing):** Effetto respiro e pulsazione del neon.
  - 🌀 **Flusso di Punti (Dot Flow):** Punti in movimento continuo lungo il percorso.
- **Regolazione Tratto & Opacità:** Pannello dedicato per calibrare spessore (pixel) e opacità in tempo reale con anteprima immediata.

### 👁️ Modalità Trasparenza ("Ghost Mode" 20%)
- **Pulsante Occhio Fluttuante:** Switch on/off veloce per ridurre al **20%** l'opacità dei tuoi disegni mentre usi lo smartphone.
- **Protezione Tratti Partner:** I disegni e le risposte del tuo partner rimangono sempre visibili al **100%**, evitando di cancellare il tuo disegno prima che il partner l'abbia visto.

### 📱 Overlay Fluttuante & Lockscreen
- **Servizio Overlay Fluttuante:** Disegna ovunque con una bolla fluttuante sempre a portata di mano.
- **Barra Strumenti Intelligente:** Pulsante *"Abbassa"* fissato a sinistra per minimizzare istantaneamente la barra degli strumenti con un tocco.
- **Integrazione Sfondo Lockscreen:** Possibilità di impostare automaticamente il disegno condiviso come sfondo della schermata di blocco.
- **Simulatore Lockscreen Integrato:** Anteprima realistica di come appare il disegno sulla schermata di blocco.

### 🌟 Sticker & Personalizzazioni
- Galleria di sticker romantici, kawaii e divertenti.
- Ridimensionamento, rotazione e posizionamento drag-and-drop su tela.

---

## 🛠️ Stack Tecnologico

- **Linguaggio:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) con Material 3 (M3)
- **Architettura:** MVVM (Model-View-ViewModel) + Clean Architecture
- **Concorrenza & Reattività:** Coroutine Kotlin & StateFlow / SharedFlow
- **Backend / Realtime Sync:** Firebase Firestore & Cloud Database
- **Servizi di Sistema:** Android Foreground Service per Overlay Canvas Fluttuante (`SYSTEM_ALERT_WINDOW`)
- **Wallpaper API:** Android `WallpaperManager` per aggiornamento dinamico dello sfondo lockscreen

---

## 🚀 Come Compilare ed Eseguire il Progetto

### Prerequisiti
- **Android Studio Ladybug (o più recente)**
- **JDK 17+**
- **Android SDK API 34+**

### Installazione
1. Clona il repository:
   ```bash
   git clone https://github.com/tuo-username/lockdraw-android.git
   cd lockdraw-android
   ```
2. Apri il progetto in **Android Studio**.
3. Assicurati che il file `google-services.json` sia configurato correttamente se utilizzi il tuo backend Firebase.
4. Compila ed esegui sull'emulatore o su un dispositivo fisico:
   ```bash
   ./gradlew installDebug
   ```

---

## 🔒 Permessi Richiesti

- `SYSTEM_ALERT_WINDOW`: Necessario per consentire all'overlay di disegno fluttuante di funzionare sopra le app e sul lockscreen.
- `FOREGROUND_SERVICE`: Mantiene attivo il servizio di sincronizzazione e il canvas fluttuante in background.
- `SET_WALLPAPER`: Utilizzato per applicare i disegni come sfondo della schermata di blocco.
- `INTERNET`: Per la sincronizzazione bidirezionale in tempo reale tra i due dispositivi.

---

## 📄 Licenza

Distribuito sotto licenza **MIT**. Consulta il file `LICENSE` per ulteriori informazioni.
