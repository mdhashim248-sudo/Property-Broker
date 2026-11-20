const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_123";
const BOB_UID = "bob_buyer_456";
const CHARLIE_UID = "charlie_intruder_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated users cannot read or write properties, profiles, or chats", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("properties").where("status", "==", "ACTIVE").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("properties").doc("prop_1").set({
      ownerId: ALICE_UID,
      ownerName: "Alice",
      title: "Unauthorized Listing",
      description: "Test",
      price: 5000000,
      location: "Mumbai",
      listingType: "SALE",
      propertyCategory: "APARTMENT",
      bedrooms: 2,
      areaSqFt: 1100,
      photoKeys: ["preset:apartment"],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 180000,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile is owner-only and rejects spoofing or ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Sharma",
      email: "alice@example.com",
      role: "OWNER",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot read or overwrite Alice's profile PII
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: pastDate(1000),
    })
  );

  // Ghost field rejected (Shadow Update Test)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );

  // Valid partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "BOTH",
      updatedAt: pastDate(1000),
    })
  );
});

test("Property listing enforces min 1 photo, max 8 MB media limit, and PII contact vault isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  // Zero photos must fail (minimum 1 photo enforced)
  await assertFails(
    aliceDb.collection("properties").doc("prop_no_photo").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "Missing Photo Property",
      description: "No photos attached",
      price: 5500000,
      location: "Mumbai",
      listingType: "SALE",
      propertyCategory: "APARTMENT",
      bedrooms: 2,
      areaSqFt: 950,
      photoKeys: [],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 0,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Media size > 8 MB (8388608 bytes) must fail
  await assertFails(
    aliceDb.collection("properties").doc("prop_oversize").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "Oversized Media Property",
      description: "Exceeds 8 MB",
      price: 5500000,
      location: "Mumbai",
      listingType: "SALE",
      propertyCategory: "APARTMENT",
      bedrooms: 2,
      areaSqFt: 950,
      photoKeys: ["preset:apartment"],
      hasVideo: true,
      videoLabel: "content://video/huge.mp4",
      totalMediaBytes: 9000000,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid property listing with >= 1 photo and <= 8 MB totalMediaBytes succeeds
  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_101").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "2BHK Sea View Apartment",
      description: "Spacious modern flat near Worli Sea Face",
      price: 28500000,
      location: "Worli, Mumbai",
      listingType: "SALE",
      propertyCategory: "APARTMENT",
      bedrooms: 2,
      areaSqFt: 1280,
      photoKeys: ["preset:apartment"],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 180000,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Ghost field @DocumentId `id` rejected
  await assertFails(
    aliceDb.collection("properties").doc("prop_102").set({
      id: "prop_102",
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "Bad ID Field",
      description: "Desc",
      price: 1000000,
      location: "Pune",
      listingType: "SALE",
      propertyCategory: "STUDIO",
      bedrooms: 1,
      areaSqFt: 500,
      photoKeys: ["preset:studio"],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 180000,
      verifiedListing: false,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice attaches PII-isolated contact vault
  await assertSucceeds(
    aliceDb
      .collection("properties")
      .doc("prop_101")
      .collection("private")
      .doc("contact")
      .set({
        ownerId: ALICE_UID,
        contactPhone: "+919876543210",
        contactEmail: "alice@example.com",
        whatsappEnabled: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Filtered marketplace query succeeds for Bob; unfiltered list fails
  await assertSucceeds(bobDb.collection("properties").where("status", "==", "ACTIVE").get());
  await assertFails(bobDb.collection("properties").get());

  // Pre-2027 free window allows authenticated contact read; non-owner still cannot edit/delete contact
  await assertSucceeds(
    bobDb
      .collection("properties")
      .doc("prop_101")
      .collection("private")
      .doc("contact")
      .get()
  );
  await assertFails(
    bobDb
      .collection("properties")
      .doc("prop_101")
      .collection("private")
      .doc("contact")
      .update({
        contactPhone: "+910000000000",
        updatedAt: pastDate(1000),
      })
  );

  // Bob CANNOT modify Alice's property
  await assertFails(
    bobDb.collection("properties").doc("prop_101").update({
      price: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Bob unlocks contact for prop_101 via immutable receipt
  await assertSucceeds(
    bobDb
      .collection("users")
      .doc(BOB_UID)
      .collection("unlocks")
      .doc("prop_101")
      .set({
        userId: BOB_UID,
        propertyId: "prop_101",
        amountPaid: 1000,
        paymentMethod: "UPI_VERIFIED",
        referenceCode: "PB-UNLOCK-9081",
        unlockedAt: pastDate(1000),
      })
  );

  // Now Bob CAN read private contact for prop_101!
  await assertSucceeds(
    bobDb
      .collection("properties")
      .doc("prop_101")
      .collection("private")
      .doc("contact")
      .get()
  );

  // Tampered future timestamp rejected
  await assertFails(
    aliceDb.collection("properties").doc("prop_101").update({
      price: 29000000,
      updatedAt: new Date(Date.now() + 120000),
    })
  );
});

test("3-Day Premium Pass unlocks private contacts and blocks cross-user tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate();

  await aliceDb.collection("properties").doc("prop_201").set({
    ownerId: ALICE_UID,
    ownerName: "Alice Sharma",
    title: "Luxury Tropical Villa",
    description: "Private pool villa",
    price: 120000,
    location: "Assagao, Goa",
    listingType: "RENT",
    propertyCategory: "VILLA",
    bedrooms: 4,
    areaSqFt: 3400,
    photoKeys: ["preset:villa"],
    hasVideo: false,
    videoLabel: "",
    totalMediaBytes: 180000,
    verifiedListing: true,
    status: "ACTIVE",
    createdAt: ts,
    updatedAt: ts,
  });

  await aliceDb
    .collection("properties")
    .doc("prop_201")
    .collection("private")
    .doc("contact")
    .set({
      ownerId: ALICE_UID,
      contactPhone: "+919811122233",
      contactEmail: "villa@example.com",
      whatsappEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    });

  // Charlie cannot modify Alice's private contact vault
  await assertFails(
    charlieDb
      .collection("properties")
      .doc("prop_201")
      .collection("private")
      .doc("contact")
      .delete()
  );

  // Bob activates 3-Day Premium Pass
  await assertSucceeds(
    bobDb
      .collection("users")
      .doc(BOB_UID)
      .collection("private")
      .doc("membership")
      .set({
        userId: BOB_UID,
        planName: "3-Day Premium Pass",
        amountPaid: 999,
        premiumActive: true,
        referenceCode: "PB-PASS-3312",
        expiresAtMillis: Date.now() + 3 * 24 * 60 * 60 * 1000,
        activatedAt: ts,
        updatedAt: ts,
      })
  );

  // Bob can now read private contact via active premium pass
  await assertSucceeds(
    bobDb
      .collection("properties")
      .doc("prop_201")
      .collection("private")
      .doc("contact")
      .get()
  );
});

test("Chat threads and messages strictly enforce participant membership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate();

  await aliceDb.collection("properties").doc("prop_301").set({
    ownerId: ALICE_UID,
    ownerName: "Alice",
    title: "Cozy Bandra Studio",
    description: "Furnished studio",
    price: 45000,
    location: "Bandra West, Mumbai",
    listingType: "RENT",
    propertyCategory: "STUDIO",
    bedrooms: 1,
    areaSqFt: 550,
    photoKeys: ["preset:studio"],
    hasVideo: false,
    videoLabel: "",
    totalMediaBytes: 180000,
    verifiedListing: true,
    status: "ACTIVE",
    createdAt: ts,
    updatedAt: ts,
  });

  const chatId = `prop_301_${BOB_UID}`;
  const participants = [BOB_UID, ALICE_UID];

  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).set({
      propertyId: "prop_301",
      propertyTitle: "Cozy Bandra Studio",
      buyerId: BOB_UID,
      buyerName: "Bob",
      ownerId: ALICE_UID,
      ownerName: "Alice",
      participantUids: participants,
      lastMessage: "Hi, is this studio available from next month?",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      senderId: BOB_UID,
      senderName: "Bob",
      participantUids: participants,
      text: "Hi, is this studio available from next month?",
      createdAt: ts,
    })
  );

  // Alice (owner) can query her chats and read/send messages
  await assertSucceeds(
    aliceDb.collection("chats").where("participantUids", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .where("participantUids", "array-contains", ALICE_UID)
      .get()
  );
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_2").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "Alice",
      participantUids: participants,
      text: "Yes, available from 1st! Would you like a tour?",
      createdAt: pastDate(1000),
    })
  );

  // Charlie (non-participant) cannot read chat, read msg_1, list messages unfiltered, or inject messages
  await assertFails(charlieDb.collection("chats").doc(chatId).get());
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").get()
  );
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").get()
  );
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_3").set({
      chatId,
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      participantUids: participants,
      text: "Spoof attempt",
      createdAt: pastDate(500),
    })
  );
});

test("Heavy Deposit listings with Shop/Gala categories and sq.ft area are validated strictly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate();

  // Valid HEAVY_DEPOSIT GALA listing succeeds
  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_hd_gala").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "Industrial Gala on Heavy Deposit",
      description: "Ground floor gala with shutter and loft",
      price: 1500000,
      location: "Bhiwandi, Thane",
      listingType: "HEAVY_DEPOSIT",
      propertyCategory: "GALA",
      bedrooms: 0,
      areaSqFt: 1400,
      photoKeys: ["preset:apartment"],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 220000,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid SHOP listing succeeds
  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_shop_1").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "Main Road Corner Shop",
      description: "High footfall retail shop",
      price: 6500000,
      location: "Andheri West, Mumbai",
      listingType: "SALE",
      propertyCategory: "SHOP",
      bedrooms: 0,
      areaSqFt: 450,
      photoKeys: ["preset:studio"],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 180000,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid listingType or negative areaSqFt must fail
  await assertFails(
    aliceDb.collection("properties").doc("prop_bad_type").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "Invalid Listing",
      description: "Bad type",
      price: 500000,
      location: "Mumbai",
      listingType: "INVALID_TYPE",
      propertyCategory: "SHOP",
      bedrooms: 0,
      areaSqFt: 400,
      photoKeys: ["preset:studio"],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 180000,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid geo-pinned listing with latitude & longitude succeeds
  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_geo_valid").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "Bandra West Sea View Flat",
      description: "Pinned on map",
      price: 25000000,
      location: "Bandra West, Mumbai",
      listingType: "SALE",
      propertyCategory: "APARTMENT",
      bedrooms: 3,
      areaSqFt: 1450,
      latitude: 19.0596,
      longitude: 72.8295,
      photoKeys: ["preset:apartment"],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 180000,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Out-of-bounds latitude (> 90) must fail
  await assertFails(
    aliceDb.collection("properties").doc("prop_geo_invalid").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "Bad Coordinates",
      description: "Invalid lat",
      price: 1000000,
      location: "Mumbai",
      listingType: "SALE",
      propertyCategory: "APARTMENT",
      bedrooms: 2,
      areaSqFt: 900,
      latitude: 120.5,
      longitude: 72.8,
      photoKeys: ["preset:apartment"],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 180000,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Verified Admin can manage all users, properties, passes, chats, and app_config while non-admins cannot", async () => {
  const adminDb = testEnv
    .authenticatedContext("admin_uid_001", {
      email: "md.hashim248@gmail.com",
      email_verified: true,
    })
    .firestore();
  const fakeAdminDb = testEnv
    .authenticatedContext("spoof_uid_002", {
      email: "md.hashim248@gmail.com",
      email_verified: false,
    })
    .firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  // Regular user creates profile & listing
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Buyer",
      email: "bob@example.com",
      role: "BUYER",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Regular user & unverified email CANNOT list users or write app_config
  await assertFails(bobDb.collection("users").get());
  await assertFails(fakeAdminDb.collection("users").get());
  await assertFails(
    bobDb.collection("app_config").doc("settings").set({
      latestVersion: "2.0",
      announcementBanner: "Hacked",
    })
  );

  // Verified Admin CAN list users, grant 7-Day Premium Pass, and update app_config
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb
      .collection("users")
      .doc(BOB_UID)
      .collection("private")
      .doc("membership")
      .set({
        userId: BOB_UID,
        planName: "7-Day Premium Pass (Admin Grant)",
        amountPaid: 999,
        premiumActive: true,
        referenceCode: "ADMIN-PASS-7D",
        expiresAtMillis: Date.now() + 7 * 24 * 60 * 60 * 1000,
        activatedAt: ts,
        updatedAt: ts,
      })
  );
  await assertSucceeds(
    adminDb.collection("app_config").doc("settings").set({
      latestVersion: "2.0",
      premiumPassPriceInr: 999,
      premiumDurationDays: 7,
      announcementBanner: "Welcome to Property Broker v2.0",
      maintenanceMode: false,
      updatedAt: ts,
    })
  );
  await assertSucceeds(bobDb.collection("app_config").doc("settings").get());

  // Audit Log: Verified Admin can create & list, cannot mutate (append-only), non-admins denied
  await assertSucceeds(
    adminDb.collection("admin_audit_logs").doc("log_001").set({
      action: "PROPERTY_STATUS",
      entityType: "PROPERTY",
      entityId: "prop_101",
      summary: "Changed status to CLOSED",
      details: "2 BHK Bandra West",
      adminEmail: "md.hashim248@gmail.com",
      createdAt: ts,
    })
  );
  await assertSucceeds(adminDb.collection("admin_audit_logs").get());
  await assertFails(
    adminDb.collection("admin_audit_logs").doc("log_001").update({
      summary: "Tampered summary",
    })
  );
  await assertFails(bobDb.collection("admin_audit_logs").get());
  await assertFails(
    bobDb.collection("admin_audit_logs").doc("log_spoof").set({
      action: "PROPERTY_EDIT",
      entityType: "PROPERTY",
      entityId: "prop_101",
      summary: "Spoof",
      adminEmail: "bob@example.com",
      createdAt: ts,
    })
  );
  await assertFails(fakeAdminDb.collection("admin_audit_logs").get());
});

test("Properties accept optional floor, depositAmount, monthlyRent, buildingAge, and hasLift with strict validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate();

  // Valid RENT listing with floor, depositAmount, buildingAge, and hasLift
  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_rent_details").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "2BHK Rental with Lift",
      description: "Well-ventilated flat",
      price: 38000,
      location: "Andheri West, Mumbai",
      listingType: "RENT",
      propertyCategory: "APARTMENT",
      bedrooms: 2,
      areaSqFt: 920,
      floor: "4th Floor",
      depositAmount: 150000,
      buildingAge: "3 Years",
      hasLift: true,
      photoKeys: ["preset:apartment"],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 180000,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid HEAVY_DEPOSIT listing with floor, monthlyRent, buildingAge, and hasLift = false
  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_heavy_details").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "1BHK Heavy Deposit Flat",
      description: "Low monthly maintenance rent",
      price: 1200000,
      location: "Mira Road, Thane",
      listingType: "HEAVY_DEPOSIT",
      propertyCategory: "APARTMENT",
      bedrooms: 1,
      areaSqFt: 620,
      floor: "2nd Floor",
      monthlyRent: 2500,
      buildingAge: "5-10 Years",
      hasLift: false,
      photoKeys: ["preset:apartment"],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 180000,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner can update buildingAge and hasLift on existing listing
  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_heavy_details").update({
      buildingAge: "6 Years",
      hasLift: true,
      monthlyRent: 0,
      updatedAt: ts,
    })
  );

  // Negative depositAmount must fail
  await assertFails(
    aliceDb.collection("properties").doc("prop_bad_deposit").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "Invalid Deposit Listing",
      description: "Negative deposit",
      price: 25000,
      location: "Pune",
      listingType: "RENT",
      propertyCategory: "APARTMENT",
      bedrooms: 1,
      areaSqFt: 550,
      depositAmount: -5000,
      photoKeys: ["preset:apartment"],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 180000,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-boolean hasLift must fail
  await assertFails(
    aliceDb.collection("properties").doc("prop_bad_lift").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Sharma",
      title: "Invalid Lift Listing",
      description: "String lift value",
      price: 5000000,
      location: "Mumbai",
      listingType: "SALE",
      propertyCategory: "APARTMENT",
      bedrooms: 2,
      areaSqFt: 850,
      hasLift: "Yes",
      photoKeys: ["preset:apartment"],
      hasVideo: false,
      videoLabel: "",
      totalMediaBytes: 180000,
      verifiedListing: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
