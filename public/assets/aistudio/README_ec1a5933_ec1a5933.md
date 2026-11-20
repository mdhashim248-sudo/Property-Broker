# Property Broker - Full Featured Android App

## ✅ Features Included

### Core
- **Sale / Buy / Rent** flow
- **Property Owner** & **Renter** separate paths
- Add Property with **Title, Price, Location, Description, Contact**
- **Up to 10 Photos** + **1 Video** upload
- Room Database (offline)

### Search & Filter
- Live search by title / location / description
- Price range filter (Min - Max)

### Payment (₹1000 Contact Fee)
- Contact number locked by default
- **"Pay ₹1000 to Unlock Contact"** button
- Razorpay integration code ready (just add your Key ID)
- Demo/Simulate payment also available

### Chat
- Simple Chat screen with Owner
- Message bubbles (left/right)
- Demo auto-reply
- Ready for Firebase Firestore (comments in code)

### Firebase Ready
- Structure prepared for:
  - Firebase Storage (photos & video)
  - Firestore (properties + chat messages)
  - Firebase Auth (future login)

---

## How to Setup in Android Studio

### 1. Create Project
- New → Empty Views Activity
- Name: `PropertyBroker`
- Package: `com.propertybroker.app`
- Language: **Kotlin**
- Min SDK: 24+

### 2. Add Dependencies (`app/build.gradle.kts`)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
    // For Firebase later:
    // id("com.google.gms.google-services")
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // RecyclerView + CardView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // ========== Razorpay ==========
    implementation("com.razorpay:checkout:1.6.33")

    // ========== Firebase (optional - enable later) ==========
    // implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    // implementation("com.google.firebase:firebase-firestore-ktx")
    // implementation("com.google.firebase:firebase-storage-ktx")
    // implementation("com.google.firebase:firebase-auth-ktx")
}
```

### 3. Copy all source files from this project into your Android Studio project.

### 4. Razorpay Setup
1. Go to https://dashboard.razorpay.com → Settings → API Keys
2. Copy **Key ID** (test or live)
3. Open `PropertyDetailActivity.kt`
4. Uncomment the Razorpay code block
5. Replace `"rzp_test_YOUR_KEY_HERE"` with your real Key ID

### 5. Firebase Setup (when you want cloud)
1. Create project at https://console.firebase.google.com
2. Add Android app with package `com.propertybroker.app`
3. Download `google-services.json` → put in `app/` folder
4. Uncomment Firebase dependencies
5. Enable **Firestore** + **Storage** in Firebase Console
6. Update `AddPropertyActivity` and `ChatActivity` to upload to Firebase (comments already present)

---

## App Flow

```
MainActivity
├── Sale     → List + Search/Filter + Add Property
├── Buy      → List + Search/Filter (view only)
└── Rent
    ├── Property Owner → Add Property (photos + video)
    └── Renter         → List + Search/Filter

Property Detail
├── Photos (horizontal) + Video
├── Pay ₹1000 → Unlock Contact
└── Chat with Owner
```

---

## Important Notes

- **Payment** is currently simulated + Razorpay code ready
- **Chat** is local demo + Firestore ready
- **Photos/Video** currently local URIs. For production upload to Firebase Storage
- For real users you should also add **Login / Signup** (Firebase Auth recommended)

Agar koi error aaye ya koi specific part change karna ho to batao.
