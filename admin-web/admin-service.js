/**
 * Firebase Auth & Named Cloud Firestore Data Layer for Property Broker Admin Portal
 * Uses Firebase JS SDK v11 modular CDN with explicit named database ID.
 */
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.1/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signInWithRedirect,
  getRedirectResult,
  signOut,
  onAuthStateChanged
} from "https://www.gstatic.com/firebasejs/11.6.1/firebase-auth.js";
import {
  getFirestore,
  collection,
  doc,
  getDoc,
  getDocs,
  getDocsFromServer,
  setDoc,
  updateDoc,
  deleteDoc,
  onSnapshot,
  query,
  orderBy,
  where,
  limit,
  Timestamp,
  writeBatch,
  serverTimestamp
} from "https://www.gstatic.com/firebasejs/11.6.1/firebase-firestore.js";

import {
  FIREBASE_CONFIG,
  FIRESTORE_DATABASE_ID,
  ADMIN_EMAIL,
  getPresetForCategory
} from "./firebase-config.js";

const app = initializeApp(FIREBASE_CONFIG);
export const auth = getAuth(app);
// CRITICAL: Connect to the Android app's named Firestore database, never (default)
export const db = getFirestore(app, FIRESTORE_DATABASE_ID);

const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({
  prompt: "select_account",
  login_hint: ADMIN_EMAIL
});

export function isAuthorizedAdmin(user) {
  return Boolean(
    user &&
    user.emailVerified &&
    user.email &&
    user.email.toLowerCase() === ADMIN_EMAIL.toLowerCase()
  );
}

export async function signInAdminWithGoogle() {
  try {
    return await signInWithPopup(auth, googleProvider);
  } catch (err) {
    if (err?.code === "auth/popup-blocked" || err?.code === "auth/cancelled-popup-request") {
      await signInWithRedirect(auth, googleProvider);
      return null;
    }
    throw err;
  }
}

export async function checkRedirectSignIn() {
  try {
    return await getRedirectResult(auth);
  } catch (err) {
    throw err;
  }
}

export function signOutAdmin() {
  return signOut(auth);
}

export function subscribeAuth(callback) {
  return onAuthStateChanged(auth, callback);
}

// ---------------- Live Collection Subscriptions ----------------

export function subscribeProperties(onData, onError) {
  const q = query(collection(db, "properties"), orderBy("updatedAt", "desc"));
  return onSnapshot(
    q,
    (snap) => {
      const items = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      onData(items);
    },
    (err) => {
      // Fallback if any legacy doc lacks updatedAt
      if (err?.code === "failed-precondition") {
        onSnapshot(collection(db, "properties"), (snap) => {
          onData(snap.docs.map((d) => ({ id: d.id, ...d.data() })));
        }, onError);
      } else {
        onError?.(err);
      }
    }
  );
}

export async function fetchPropertiesOnce() {
  const orderedQuery = query(collection(db, "properties"), orderBy("updatedAt", "desc"));
  const readSnap = async (targetQuery) => {
    try {
      return await getDocsFromServer(targetQuery);
    } catch (err) {
      if (err?.code === "unavailable") {
        return await getDocs(targetQuery);
      }
      throw err;
    }
  };

  try {
    const snap = await readSnap(orderedQuery);
    return snap.docs.map((d) => ({ id: d.id, ...d.data() }));
  } catch (err) {
    if (err?.code === "failed-precondition") {
      const snap = await readSnap(collection(db, "properties"));
      const items = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      items.sort((a, b) => (b.updatedAt?.seconds || 0) - (a.updatedAt?.seconds || 0));
      return items;
    }
    throw err;
  }
}

export function subscribeUsers(onData, onError) {
  return onSnapshot(
    collection(db, "users"),
    (snap) => {
      const items = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      items.sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
      onData(items);
    },
    onError
  );
}

export function subscribeChats(onData, onError) {
  return onSnapshot(
    collection(db, "chats"),
    (snap) => {
      const items = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      items.sort((a, b) => (b.updatedAt?.seconds || 0) - (a.updatedAt?.seconds || 0));
      onData(items);
    },
    onError
  );
}

export function subscribeChatMessages(chatId, onData, onError) {
  const q = query(collection(db, "chats", chatId, "messages"), orderBy("createdAt", "asc"));
  return onSnapshot(
    q,
    (snap) => onData(snap.docs.map((d) => ({ id: d.id, ...d.data() }))),
    onError
  );
}

export function subscribeAppSettings(onData, onError) {
  return onSnapshot(
    doc(db, "app_config", "settings"),
    (snap) => onData(snap.exists() ? snap.data() : null),
    onError
  );
}

export function subscribeAuditLogs(onData, onError) {
  const q = query(
    collection(db, "admin_audit_logs"),
    orderBy("createdAt", "desc"),
    limit(200)
  );
  return onSnapshot(
    q,
    (snap) => onData(snap.docs.map((d) => ({ id: d.id, ...d.data() }))),
    onError
  );
}

// ---------------- Properties CRUD ----------------

export async function getPropertyContact(propertyId) {
  const snap = await getDoc(doc(db, "properties", propertyId, "private", "contact"));
  return snap.exists() ? snap.data() : null;
}

export async function savePropertyWithContact({ existingId, existingOwnerId, adminUid, form }) {
  const isEdit = Boolean(existingId);
  const propRef = isEdit
    ? doc(db, "properties", existingId)
    : doc(collection(db, "properties"));
  const ownerId = (existingOwnerId || adminUid || "admin").trim();

  const photoInput = (form.photo || "").trim();
  const photoKeys = [photoInput || getPresetForCategory(form.propertyCategory)];

  const basePayload = {
    ownerName: form.ownerName.trim().slice(0, 80),
    title: form.title.trim().slice(0, 100),
    description: (form.description || "").trim().slice(0, 1200),
    price: Math.max(1, Math.min(10000000000, Math.round(Number(form.price) || 1))),
    location: form.location.trim().slice(0, 120),
    listingType: form.listingType,
    propertyCategory: form.propertyCategory,
    bedrooms: Math.max(0, Math.min(20, Math.round(Number(form.bedrooms) || 0))),
    areaSqFt: Math.max(0, Math.min(1000000, Math.round(Number(form.areaSqFt) || 0))),
    photoKeys,
    verifiedListing: Boolean(form.verifiedListing),
    status: form.status === "CLOSED" ? "CLOSED" : "ACTIVE",
    updatedAt: serverTimestamp()
  };

  if (form.latitude !== "" && form.latitude != null && !Number.isNaN(Number(form.latitude))) {
    basePayload.latitude = Number(form.latitude);
  }
  if (form.longitude !== "" && form.longitude != null && !Number.isNaN(Number(form.longitude))) {
    basePayload.longitude = Number(form.longitude);
  }

  const cleanFloor = (form.floor || "").trim().slice(0, 40);
  if (cleanFloor || isEdit) {
    basePayload.floor = (form.listingType === "RENT" || form.listingType === "HEAVY_DEPOSIT") ? cleanFloor : "";
  }
  if (form.listingType === "RENT" && form.depositAmount !== "" && form.depositAmount != null && !Number.isNaN(Number(form.depositAmount))) {
    basePayload.depositAmount = Math.max(0, Math.min(10000000000, Math.round(Number(form.depositAmount))));
  } else if (isEdit) {
    basePayload.depositAmount = 0;
  }
  if (form.listingType === "HEAVY_DEPOSIT" && form.monthlyRent !== "" && form.monthlyRent != null && !Number.isNaN(Number(form.monthlyRent))) {
    basePayload.monthlyRent = Math.max(0, Math.min(10000000000, Math.round(Number(form.monthlyRent))));
  } else if (isEdit) {
    basePayload.monthlyRent = 0;
  }
  const cleanBuildingAge = (form.buildingAge || "").trim().slice(0, 40);
  if (cleanBuildingAge || isEdit) {
    basePayload.buildingAge = cleanBuildingAge;
  }
  basePayload.hasLift = Boolean(form.hasLift);

  if (isEdit) {
    await updateDoc(propRef, basePayload);
  } else {
    await setDoc(propRef, {
      ...basePayload,
      ownerId,
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 0,
      createdAt: serverTimestamp()
    });
  }

  const contactRef = doc(db, "properties", propRef.id, "private", "contact");
  const contactSnap = await getDoc(contactRef);
  const contactUpdate = {
    contactPhone: form.contactPhone.trim().slice(0, 20),
    contactEmail: (form.contactEmail || "").trim().slice(0, 160),
    whatsappEnabled: Boolean(form.whatsappEnabled),
    updatedAt: serverTimestamp()
  };

  if (contactSnap.exists()) {
    await updateDoc(contactRef, contactUpdate);
  } else {
    await setDoc(contactRef, {
      ...contactUpdate,
      ownerId,
      createdAt: serverTimestamp()
    });
  }

  return propRef.id;
}

export async function togglePropertyStatus(propertyId, currentStatus) {
  const nextStatus = currentStatus === "ACTIVE" ? "CLOSED" : "ACTIVE";
  await updateDoc(doc(db, "properties", propertyId), {
    status: nextStatus,
    updatedAt: serverTimestamp()
  });
  return nextStatus;
}

export async function togglePropertyVerified(propertyId, currentVerified) {
  const nextVerified = !currentVerified;
  await updateDoc(doc(db, "properties", propertyId), {
    verifiedListing: nextVerified,
    updatedAt: serverTimestamp()
  });
  return nextVerified;
}

export async function deletePropertyCompletely(propertyId) {
  try {
    await deleteDoc(doc(db, "properties", propertyId, "private", "contact"));
  } catch {
    // Contact vault may not exist on older listings
  }
  await deleteDoc(doc(db, "properties", propertyId));
}

// ---------------- Users & 7-Day Premium Passes ----------------

export async function updateUserRole(userId, newRole) {
  await updateDoc(doc(db, "users", userId), {
    role: newRole,
    updatedAt: serverTimestamp()
  });
}

export async function deleteUserProfile(userId) {
  try {
    await deleteDoc(doc(db, "users", userId, "private", "membership"));
  } catch {
    // Ignore if no membership exists
  }
  await deleteDoc(doc(db, "users", userId));
}

export async function fetchUserMembership(userId) {
  try {
    const snap = await getDoc(doc(db, "users", userId, "private", "membership"));
    return snap.exists() ? snap.data() : null;
  } catch {
    return null;
  }
}

export async function fetchAllMemberships(users) {
  const map = new Map();
  await Promise.all(
    users.map(async (u) => {
      const m = await fetchUserMembership(u.id);
      if (m) map.set(u.id, m);
    })
  );
  return map;
}

export async function saveUserPremiumPass({ userId, planName, days, amountPaid, referenceCode, premiumActive }) {
  const memRef = doc(db, "users", userId, "private", "membership");
  const existing = await getDoc(memRef);
  const expiresAtMillis = premiumActive
    ? Date.now() + Math.max(1, Math.round(Number(days) || 7)) * 24 * 60 * 60 * 1000
    : 0;

  const common = {
    planName: planName.trim().slice(0, 80),
    amountPaid: Math.max(0, Math.min(10000, Math.round(Number(amountPaid) || 0))),
    premiumActive: Boolean(premiumActive),
    referenceCode: referenceCode.trim().slice(0, 64),
    expiresAtMillis,
    updatedAt: serverTimestamp()
  };

  if (existing.exists()) {
    await updateDoc(memRef, common);
  } else {
    await setDoc(memRef, {
      ...common,
      userId,
      activatedAt: serverTimestamp()
    });
  }
  return { userId, ...common };
}

export async function revokeUserPremiumPass(userId) {
  const memRef = doc(db, "users", userId, "private", "membership");
  const existing = await getDoc(memRef);
  if (!existing.exists()) return;
  await updateDoc(memRef, {
    premiumActive: false,
    expiresAtMillis: 0,
    updatedAt: serverTimestamp()
  });
}

// ---------------- Chats & App Config ----------------

export async function deleteChatThread(chatId) {
  const msgsSnap = await getDocs(collection(db, "chats", chatId, "messages"));
  await Promise.all(
    msgsSnap.docs.map((m) => deleteDoc(doc(db, "chats", chatId, "messages", m.id)))
  );
  await deleteDoc(doc(db, "chats", chatId));
}

export async function saveAppSettings(payload, adminEmail) {
  await setDoc(
    doc(db, "app_config", "settings"),
    {
      ...payload,
      updatedBy: adminEmail || ADMIN_EMAIL,
      updatedAt: serverTimestamp()
    },
    { merge: true }
  );
}

// ---------------- Admin Audit Trail ----------------

export async function recordAdminAuditLog({
  action,
  entityType,
  entityId,
  summary,
  details = "",
  adminEmail
}) {
  try {
    const logRef = doc(collection(db, "admin_audit_logs"));
    await setDoc(logRef, {
      action,
      entityType,
      entityId: String(entityId || "settings").slice(0, 128),
      summary: String(summary || action).trim().slice(0, 240),
      details: String(details || "").trim().slice(0, 500),
      adminEmail: (adminEmail || ADMIN_EMAIL).trim().slice(0, 160),
      createdAt: serverTimestamp()
    });
  } catch (err) {
    console.warn("Audit log write skipped:", err?.message || err);
  }
}

export async function clearAuditLogs(logIds) {
  await Promise.all(
    (logIds || []).map((id) => deleteDoc(doc(db, "admin_audit_logs", id)))
  );
}

export async function pruneExpiredAuditLogs(retentionDays = 30) {
  const safeDays = Math.max(1, Math.round(Number(retentionDays) || 30));
  const cutoffDate = new Date(Date.now() - safeDays * 24 * 60 * 60 * 1000);
  const cutoffTimestamp = Timestamp.fromDate(cutoffDate);
  const batchLimit = 450;
  let deletedCount = 0;

  while (true) {
    const expiredQuery = query(
      collection(db, "admin_audit_logs"),
      where("createdAt", "<", cutoffTimestamp),
      limit(batchLimit)
    );
    const snap = await getDocs(expiredQuery);
    if (snap.empty) break;

    const batch = writeBatch(db);
    snap.docs.forEach((docSnap) => {
      batch.delete(docSnap.ref);
    });
    await batch.commit();
    deletedCount += snap.size;

    if (snap.size < batchLimit) break;
  }

  return { deletedCount, cutoffDate, retentionDays: safeDays };
}
