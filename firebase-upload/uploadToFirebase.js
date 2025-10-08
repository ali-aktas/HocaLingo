/**
 * Firebase Upload Script
 * Uploads test_words.json to Firebase Storage and Firestore
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// === CONFIGURATION ===
const SERVICE_ACCOUNT_PATH = '../serviceAccountKey.json';
const TEST_WORDS_JSON_PATH = '../app/src/main/assets/test_words.json';
const STORAGE_BUCKET_PATH = 'word_packages/a1_en_tr_test_v1.json';
const FIRESTORE_COLLECTION = 'wordPackages';

// === COLORS FOR CONSOLE ===
const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m'
};

function log(message, color = colors.reset) {
  console.log(`${color}${message}${colors.reset}`);
}

// === INITIALIZE FIREBASE ===
async function initializeFirebase() {
  try {
    log('\n🔥 Firebase Admin SDK başlatılıyor...', colors.cyan);

    const serviceAccount = require(SERVICE_ACCOUNT_PATH);

    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      storageBucket: 'hocalingo.firebasestorage.app',
      databaseURL: `https://${serviceAccount.project_id}.firebaseio.com`
    });

    // Set database ID for non-default database
    const db = admin.firestore();
    db.settings({
      databaseId: 'hocalingodatabase'
    });

    log('✅ Firebase başarıyla başlatıldı!', colors.green);
    log('✅ Database: hocalingodatabase', colors.green);
    return true;
  } catch (error) {
    log(`❌ Firebase başlatma hatası: ${error.message}`, colors.red);
    return false;
  }
}

// === READ JSON FILE ===
function readJsonFile(filePath) {
  try {
    log(`\n📖 JSON dosyası okunuyor: ${filePath}`, colors.cyan);

    const absolutePath = path.resolve(__dirname, filePath);
    const fileContent = fs.readFileSync(absolutePath, 'utf8');
    const data = JSON.parse(fileContent);

    log(`✅ JSON başarıyla okundu: ${data.words.length} kelime`, colors.green);
    return data;
  } catch (error) {
    log(`❌ JSON okuma hatası: ${error.message}`, colors.red);
    return null;
  }
}

// === UPLOAD TO STORAGE ===
async function uploadToStorage(data) {
  try {
    log('\n☁️  Firebase Storage\'a yükleniyor...', colors.cyan);

    const bucket = admin.storage().bucket();
    const file = bucket.file(STORAGE_BUCKET_PATH);

    // JSON'u string'e çevir
    const jsonString = JSON.stringify(data, null, 2);

    // Upload
    await file.save(jsonString, {
      contentType: 'application/json',
      metadata: {
        cacheControl: 'public, max-age=3600'
      }
    });

    // Public URL al
    await file.makePublic();
    const publicUrl = `https://storage.googleapis.com/${bucket.name}/${STORAGE_BUCKET_PATH}`;

    log(`✅ Storage\'a yüklendi!`, colors.green);
    log(`   URL: ${publicUrl}`, colors.blue);

    return publicUrl;
  } catch (error) {
    log(`❌ Storage yükleme hatası: ${error.message}`, colors.red);
    return null;
  }
}

// === UPLOAD TO FIRESTORE ===
async function uploadToFirestore(packageInfo, storageUrl) {
  try {
    log('\n🗄️  Firestore\'a metadata yazılıyor...', colors.cyan);

    const db = admin.firestore();
    const docRef = db.collection(FIRESTORE_COLLECTION).doc(packageInfo.id);

    const firestoreData = {
      packageId: packageInfo.id,
      version: packageInfo.version,
      level: packageInfo.level,
      languagePair: packageInfo.language_pair,
      totalWords: packageInfo.total_words,
      description: packageInfo.description,
      attribution: packageInfo.attribution,
      storageUrl: storageUrl,
      requiresPremium: false,
      fileSize: JSON.stringify(packageInfo).length,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: packageInfo.updated_at
    };

    await docRef.set(firestoreData);

    log(`✅ Firestore\'a yazıldı!`, colors.green);
    log(`   Document ID: ${packageInfo.id}`, colors.blue);

    return true;
  } catch (error) {
    log(`❌ Firestore yazma hatası: ${error.message}`, colors.red);
    return false;
  }
}

// === DELETE PLACEHOLDER ===
async function deletePlaceholder() {
  try {
    log('\n🗑️  Placeholder document siliniyor...', colors.cyan);

    const db = admin.firestore();
    const placeholderRef = db.collection(FIRESTORE_COLLECTION).doc('_placeholder');

    const doc = await placeholderRef.get();
    if (doc.exists) {
      await placeholderRef.delete();
      log('✅ Placeholder silindi!', colors.green);
    } else {
      log('ℹ️  Placeholder bulunamadı (zaten silinmiş olabilir)', colors.yellow);
    }
  } catch (error) {
    log(`⚠️  Placeholder silme hatası (sorun değil): ${error.message}`, colors.yellow);
  }
}

// === MAIN FUNCTION ===
async function main() {
  log('\n' + '='.repeat(60), colors.cyan);
  log('🚀 HOCALINGO - FIREBASE UPLOAD SCRIPT', colors.cyan);
  log('='.repeat(60) + '\n', colors.cyan);

  // 1. Initialize Firebase
  const initialized = await initializeFirebase();
  if (!initialized) {
    log('\n❌ Script sonlandırıldı.', colors.red);
    process.exit(1);
  }

  // 2. Read JSON
  const data = readJsonFile(TEST_WORDS_JSON_PATH);
  if (!data) {
    log('\n❌ Script sonlandırıldı.', colors.red);
    process.exit(1);
  }

  // 3. Upload to Storage
  const storageUrl = await uploadToStorage(data);
  if (!storageUrl) {
    log('\n❌ Script sonlandırıldı.', colors.red);
    process.exit(1);
  }

  // 4. Upload to Firestore
  const firestoreSuccess = await uploadToFirestore(data.package_info, storageUrl);
  if (!firestoreSuccess) {
    log('\n❌ Script sonlandırıldı.', colors.red);
    process.exit(1);
  }

  // 5. Delete placeholder
  await deletePlaceholder();

  // Success!
  log('\n' + '='.repeat(60), colors.green);
  log('✅ TÜM İŞLEMLER BAŞARILI!', colors.green);
  log('='.repeat(60), colors.green);
  log('\n📊 ÖZET:', colors.cyan);
  log(`   📦 Paket ID: ${data.package_info.id}`, colors.blue);
  log(`   📝 Kelime Sayısı: ${data.words.length}`, colors.blue);
  log(`   🔗 Storage URL: ${storageUrl}`, colors.blue);
  log('\n✅ Firebase Console\'da kontrol edebilirsin!', colors.green);
  log('   - Storage: https://console.firebase.google.com/project/_/storage', colors.blue);
  log('   - Firestore: https://console.firebase.google.com/project/_/firestore', colors.blue);

  process.exit(0);
}

// === RUN ===
main().catch(error => {
  log(`\n💥 Beklenmeyen hata: ${error.message}`, colors.red);
  console.error(error);
  process.exit(1);
});