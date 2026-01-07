# ResQNav - Disaster Management System 🚨

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**ResQNav** is a comprehensive disaster management and emergency response Android application built with Kotlin. It provides real-time disaster alerts, AI-powered assistance, emergency shelter location, and offline map capabilities for disaster-affected areas.

---

## 🌟 Features

### 🗺️ Smart Mapping & Navigation
- **Real-time Maps**: Powered by OpenStreetMap (OSM) with offline tile support
- **Disaster Zone Visualization**: Color-coded overlays for different disaster types
- **Smart Route Planning**: AI-powered route optimization avoiding disaster zones
- **Emergency Shelter Locator**: Find nearby shelters with real-time availability

### 🚨 Disaster Alerts & Monitoring
- **Real-time Alerts**: Push notifications for disasters in your area
- **Multiple Disaster Types**: Earthquake, Flood, Cyclone, Fire, Tsunami, Volcano
- **Geofencing**: Automatic alerts when entering high-risk zones
- **Historical Data**: View past disaster events and patterns

### 🤖 AI-Powered Assistance
- **Emergency Chatbot**: 24/7 AI assistant powered by Groq API (LLama 3.1)
- **Damage Assessment**: ML-based image analysis for disaster damage estimation
- **Smart Routing**: AI suggests safest evacuation routes
- **Voice SOS**: Voice-activated emergency assistance

### 📴 Offline Capabilities
- **Offline Maps**: Download map tiles for offline use (up to 20GB storage)
- **Offline Shelters**: Cached emergency shelter information
- **Works Without Internet**: Critical features available offline

### 🏥 Emergency Services
- **Shelter Management**: Real-time capacity tracking and booking
- **Emergency Contacts**: Quick access to disaster helplines
- **Medical Assistance**: Locate nearby hospitals and clinics
- **Resource Tracking**: Monitor availability of food, water, medicine

---

## 📱 Screenshots

| Map View | Disaster Alerts | AI Chatbot | Shelters |
|----------|----------------|------------|----------|
| ![Map](screenshots/map.png) | ![Alerts](screenshots/alerts.png) | ![AI](screenshots/chatbot.png) | ![Shelters](screenshots/shelters.png) |

---

## 🛠️ Tech Stack

### Android
- **Language**: Kotlin 2.0
- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 15 (API 36)
- **Architecture**: MVVM with LiveData

### Libraries & APIs
- **Firebase**: Authentication, Firestore, Cloud Messaging, Analytics
- **OSM Droid**: OpenStreetMap for offline-capable maps
- **Groq API**: Free AI chatbot (LLama 3.1 8B Instant)
- **TensorFlow Lite**: On-device ML for damage assessment
- **Retrofit**: REST API client
- **Room**: Local database for offline data
- **Coroutines**: Asynchronous programming
- **Material Design 3**: Modern UI components

### Backend & APIs
- **Firebase Firestore**: Real-time disaster data
- **Overpass API**: Emergency facility locations from OSM
- **Groq Cloud**: Free AI inference (100% free API!)

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Hedgehog or later (2023.1.1+)
- **JDK**: Version 11 or higher
- **Android SDK**: API 36 (Android 15)
- **Firebase Account**: For backend services
- **Groq API Key**: Free from [console.groq.com](https://console.groq.com/keys)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/NikhilNaik23/ResQNav.git
cd ResQNav
```

2. **Set up Firebase**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or use existing one
   - Add an Android app with package name: `com.resqnav.app`
   - Download `google-services.json`
   - Place it in `app/` directory

3. **Configure Keystore** (for release builds)
   - Copy `keystore.properties.example` to `keystore.properties`
   - Fill in your keystore details:
   ```properties
   storeFile=/path/to/your/keystore.jks
   storePassword=YOUR_PASSWORD
   keyAlias=YOUR_ALIAS
   keyPassword=YOUR_PASSWORD
   ```

4. **Set up Groq API** (for AI Chatbot)
   - Get free API key from [console.groq.com](https://console.groq.com/keys)
   - Open `app/src/main/java/com/resqnav/app/utils/GroqConfig.kt`
   - Replace `GROQ_API_KEY` with your key (or add to local.properties)

5. **Build and Run**
   ```bash
   # Open in Android Studio and click Run
   # OR use command line:
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

### 🔐 Security Note
**NEVER commit these files to Git:**
- `google-services.json` (Firebase keys)
- `keystore.properties` (passwords)
- `*.jks` / `*.pem` (signing keys)

These files are already in `.gitignore`. See [SECURITY.md](SECURITY.md) for details.

---

## 📖 Usage

### For End Users
1. **Sign Up/Login**: Create account or use Google Sign-In
2. **Grant Permissions**: Location, notifications, storage (for offline maps)
3. **Enable Alerts**: Get notified about disasters in your area
4. **Explore Map**: View disaster zones, shelters, and safe routes
5. **Chat with AI**: Get emergency guidance 24/7
6. **Find Shelters**: Locate and navigate to nearest emergency shelter

### For Developers
- See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines
- Read [SECURITY.md](SECURITY.md) for security best practices
- Check [PRE_COMMIT_CHECKLIST.md](PRE_COMMIT_CHECKLIST.md) before pushing

---

## 🗂️ Project Structure

```
ResQNav/
├── app/
│   ├── src/main/
│   │   ├── java/com/resqnav/app/
│   │   │   ├── MainActivity.kt           # Main entry point
│   │   │   ├── MapFragment.kt            # OSM map interface
│   │   │   ├── SheltersFragment.kt       # Emergency shelters
│   │   │   ├── AlertsFragment.kt         # Disaster alerts
│   │   │   ├── ai/                       # AI features
│   │   │   │   ├── AIChatbotFragment.kt  # Groq-powered chatbot
│   │   │   │   ├── SmartRouteFragment.kt # AI route planning
│   │   │   │   └── DamageAssessmentFragment.kt
│   │   │   ├── api/                      # API clients
│   │   │   ├── model/                    # Data models
│   │   │   ├── viewmodel/                # MVVM ViewModels
│   │   │   ├── notifications/            # Push notifications
│   │   │   ├── offline/                  # Offline features
│   │   │   └── utils/                    # Utilities
│   │   ├── res/                          # Resources (layouts, drawables)
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts                  # App build config
│   ├── proguard-rules.pro                # R8/ProGuard rules
│   └── google-services.json.example      # Firebase template
├── gradle/                               # Gradle wrapper
├── .gitignore                            # Git ignore patterns
├── SECURITY.md                           # Security guidelines
├── PRE_COMMIT_CHECKLIST.md              # Pre-commit checks
├── check-security.ps1                    # Security check script
└── README.md                             # This file
```

---

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
```bash
./gradlew jacocoTestReport
```

---

## 📦 Building Release APK/AAB

### Build Release AAB (for Play Store)
```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

### Build Release APK
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Generate Debug Symbols (for Play Console)
Debug symbols are automatically generated in:
`app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip`

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

**Before submitting PR:**
- Run `./check-security.ps1` to verify no secrets exposed
- Test on multiple Android versions
- Follow Kotlin coding conventions
- Update documentation if needed

---

## 🐛 Known Issues & Limitations

- **Offline Maps**: Requires significant storage (up to 20GB for large areas)
- **Groq API**: Free tier has rate limits (30 requests/minute)
- **OSM Data**: Emergency facilities may be incomplete in some regions
- **Geofencing**: Battery intensive when continuously monitoring location

---

## 📝 Roadmap

### Version 1.3 (Q1 2026)
- [ ] Multi-language support (Hindi, Punjabi, Tamil, etc.)
- [ ] Peer-to-peer mesh networking for disaster zones
- [ ] Blockchain-based resource tracking
- [ ] AR navigation for indoor shelters

### Version 1.4 (Q2 2026)
- [ ] Drone integration for aerial surveillance
- [ ] Predictive disaster modeling with ML
- [ ] Integration with government NDMA systems
- [ ] Community reporting features

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors & Maintainers

- **Nikhil Naik** - Initial work - [@nikhilnaik23](https://github.com/nikhilnaik23)

---

## 🙏 Acknowledgments

- **OpenStreetMap**: Community-driven map data
- **Groq**: Free AI inference API
- **Firebase**: Backend infrastructure
- **TensorFlow**: ML models
- **Material Design**: UI/UX guidelines
- **OSMDroid**: Android map library
- **NDMA India**: Disaster management guidelines

---

## 📞 Support & Contact

- **Issues**: [GitHub Issues](https://github.com/NikhilNaik23/ResQNav/issues)
- **Documentation**: [Wiki](https://github.com/NikhilNaik23/ResQNav/wiki)
- **Discord**: [Join our community](https://discord.gg/resqnav)
- **Documentation**: [Wiki](https://github.com/YOUR_USERNAME/ResQNav/wiki)

---

---

<div align="center">
  <strong>Built with ❤️ for saving lives during disasters</strong>
  <br>
  <sub>© 2026 ResQNav. All rights reserved.</sub>
</div>
