package com.hocalingo.app.core.common

/**
 * Firebase configuration constants
 *
 * WEB CLIENT ID'yi almak için:
 * 1. Firebase Console'a git
 * 2. Authentication > Sign-in method > Google
 * 3. "Web SDK configuration" bölümünü genişlet
 * 4. "Web client ID" değerini kopyala
 *
 * VEYA
 *
 * 1. google-services.json dosyasını aç
 * 2. "oauth_client" bölümünde "client_type": 3 olan kısmı bul
 * 3. "client_id" değerini kopyala
 */
object FirebaseConfig {
    // Bu değeri Firebase Console'dan veya google-services.json'dan al
    const val WEB_CLIENT_ID = "YOUR_ACTUAL_WEB_CLIENT_ID_HERE"

    // Örnek format:
    // const val WEB_CLIENT_ID = "123456789-abcdefghijk.apps.googleusercontent.com"
}