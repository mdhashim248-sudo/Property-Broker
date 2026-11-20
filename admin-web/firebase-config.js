/**
 * Property Broker — Admin Web Portal Configuration & Utilities
 * Connected directly to the live Android app's Firebase project & named Firestore database.
 */

export const FIREBASE_CONFIG = {
  apiKey: "AIzaSyCdZnQ_Xjtzq-AnRr9MVP4bByIqHc0n0W4",
  authDomain: "gen-lang-client-0124044841.firebaseapp.com",
  projectId: "gen-lang-client-0124044841",
  storageBucket: "gen-lang-client-0124044841.firebasestorage.app",
  messagingSenderId: "260049592708",
  // Web appId falls back cleanly when using Firebase Auth & Firestore REST/gRPC endpoints
  appId: "1:260049592708:web:propertybrokeradmin01"
};

// CRITICAL: Must always match the Android app's named Firestore database ID
export const FIRESTORE_DATABASE_ID =
  "ai-studio-android-property-927de2c4-bc76-4f59-b4aa-99e514cdbdd6";

// Only this verified Google account is authorized by firestore.rules isAdmin()
export const ADMIN_EMAIL = "md.hashim248@gmail.com";

// Scheduled Premium Activation: Free until 31 Dec 2026; activates 1 Jan 2027 00:00:00 IST
export const PREMIUM_ACTIVATION_ISO = "2027-01-01T00:00:00+05:30";
export const PREMIUM_PASS_DAYS_DEFAULT = 7;
export const PREMIUM_PASS_PRICE_DEFAULT = 999;

export const LISTING_TYPES = [
  { value: "SALE", label: "Buy / Sale", shortLabel: "Sale", color: "#1E3A8A" },
  { value: "RENT", label: "Rent", shortLabel: "Rent", color: "#059669" },
  { value: "HEAVY_DEPOSIT", label: "Heavy Deposit", shortLabel: "Heavy Deposit", color: "#D97706" }
];

export const PROPERTY_CATEGORIES = [
  { value: "APARTMENT", label: "Apartment / Flat", preset: "preset:apartment" },
  { value: "VILLA", label: "Villa / Bungalow", preset: "preset:villa" },
  { value: "HOUSE", label: "Independent House", preset: "preset:house" },
  { value: "SHOP", label: "Shop / Showroom", preset: "preset:shop" },
  { value: "GALA", label: "Industrial Gala", preset: "preset:gala" },
  { value: "OFFICE", label: "Office Space", preset: "preset:office" },
  { value: "STUDIO", label: "Studio", preset: "preset:studio" },
  { value: "COMMERCIAL", label: "Commercial", preset: "preset:commercial" },
  { value: "PLOT", label: "Plot / Land", preset: "preset:plot" }
];

export const USER_ROLES = [
  { value: "BUYER", label: "Buyer" },
  { value: "OWNER", label: "Owner / Broker" },
  { value: "RENTER", label: "Renter" },
  { value: "BOTH", label: "Buyer & Owner" }
];

// Mirrors Android LocationResolver.kt Indian cities & Mumbai/NCR/Metro localities
export const INDIAN_LOCALITY_COORDS = {
  "mumbai": [19.0760, 72.8777],
  "andheri": [19.1136, 72.8697],
  "bandra": [19.0596, 72.8295],
  "juhu": [19.1075, 72.8263],
  "powai": [19.1176, 72.9060],
  "borivali": [19.2307, 72.8567],
  "malad": [19.1874, 72.8484],
  "kandivali": [19.2047, 72.8520],
  "goregaon": [19.1663, 72.8526],
  "dadar": [19.0178, 72.8478],
  "worli": [19.0176, 72.8162],
  "lower parel": [18.9977, 72.8304],
  "colaba": [18.9067, 72.8147],
  "kurla": [19.0726, 72.8845],
  "ghatkopar": [19.0856, 72.9082],
  "mulund": [19.1726, 72.9425],
  "chembur": [19.0522, 72.9005],
  "mira road": [19.2813, 72.8785],
  "thane": [19.2183, 72.9781],
  "navi mumbai": [19.0330, 73.0297],
  "vashi": [19.0771, 72.9986],
  "kharghar": [19.0473, 73.0699],
  "panvel": [18.9894, 73.1175],
  "pune": [18.5204, 73.8567],
  "baner": [18.5590, 73.7868],
  "hinjewadi": [18.5913, 73.7389],
  "wakad": [18.5987, 73.7688],
  "koregaon park": [18.5362, 73.8940],
  "delhi": [28.6139, 77.2090],
  "new delhi": [28.6139, 77.2090],
  "gurgaon": [28.4595, 77.0266],
  "gurugram": [28.4595, 77.0266],
  "noida": [28.5355, 77.3910],
  "greater noida": [28.4744, 77.5040],
  "faridabad": [28.4089, 77.3178],
  "ghaziabad": [28.6692, 77.4538],
  "bengaluru": [12.9716, 77.5946],
  "bangalore": [12.9716, 77.5946],
  "whitefield": [12.9698, 77.7500],
  "indiranagar": [12.9784, 77.6408],
  "koramangala": [12.9352, 77.6245],
  "hsr layout": [12.9121, 77.6446],
  "hyderabad": [17.3850, 78.4867],
  "gachibowli": [17.4401, 78.3489],
  "banjara hills": [17.4156, 78.4347],
  "chennai": [13.0827, 80.2707],
  "kolkata": [22.5726, 88.3639],
  "ahmedabad": [23.0225, 72.5714],
  "surat": [21.1702, 72.8311],
  "jaipur": [26.9124, 75.7873],
  "lucknow": [26.8467, 80.9462],
  "indore": [22.7196, 75.8577],
  "nagpur": [21.1458, 79.0882],
  "patna": [25.5941, 85.1376],
  "chandigarh": [30.7333, 76.7794],
  "kochi": [9.9312, 76.2673],
  "goa": [15.2993, 74.1240],
  "bhopal": [23.2599, 77.4126],
  "nashik": [19.9975, 73.7898],
  "vadodara": [22.3072, 73.1812]
};

export function resolveIndianCoordinates(locationText) {
  const normalized = (locationText || "").toLowerCase().trim();
  if (!normalized) return { lat: 19.0760, lng: 72.8777 };

  for (const [key, coords] of Object.entries(INDIAN_LOCALITY_COORDS)) {
    if (normalized.includes(key)) {
      // Deterministic tiny jitter so pins in the same locality don't stack on 1 pixel
      let hash = 0;
      for (let i = 0; i < normalized.length; i++) {
        hash = ((hash << 5) - hash) + normalized.charCodeAt(i);
        hash |= 0;
      }
      const jitterLat = ((hash % 100) / 100) * 0.006;
      const jitterLng = (((hash >> 4) % 100) / 100) * 0.006;
      return {
        lat: Number((coords[0] + jitterLat).toFixed(5)),
        lng: Number((coords[1] + jitterLng).toFixed(5))
      };
    }
  }
  return { lat: 19.0760, lng: 72.8777 };
}

export function formatINR(amount, listingType = "SALE") {
  const num = Number(amount) || 0;
  let formatted;
  if (num >= 10000000) {
    formatted = `₹${(num / 10000000).toFixed(2).replace(/\.00$/, "")} Cr`;
  } else if (num >= 100000) {
    formatted = `₹${(num / 100000).toFixed(2).replace(/\.00$/, "")} L`;
  } else if (num >= 1000) {
    formatted = `₹${num.toLocaleString("en-IN")}`;
  } else {
    formatted = `₹${num}`;
  }
  if (listingType === "RENT") return `${formatted}/mo`;
  if (listingType === "HEAVY_DEPOSIT") return `${formatted} Deposit`;
  return formatted;
}

export function formatTimestamp(ts) {
  if (!ts) return "—";
  let date;
  if (typeof ts.toDate === "function") {
    date = ts.toDate();
  } else if (typeof ts === "number") {
    date = new Date(ts);
  } else if (ts.seconds) {
    date = new Date(ts.seconds * 1000);
  } else {
    date = new Date(ts);
  }
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

export function formatShortDate(millis) {
  if (!millis || millis <= 0) return "—";
  const d = new Date(millis);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  });
}

export function escapeHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

export function downloadCsv(filename, headers, rows) {
  const escapeCell = (val) => {
    const s = String(val ?? "").replace(/"/g, '""');
    return /[",\n]/.test(s) ? `"${s}"` : s;
  };
  const lines = [
    headers.map(escapeCell).join(","),
    ...rows.map((r) => r.map(escapeCell).join(","))
  ];
  const blob = new Blob(["\uFEFF" + lines.join("\n")], {
    type: "text/csv;charset=utf-8;"
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export function getListingTypeLabel(type) {
  return LISTING_TYPES.find((t) => t.value === type)?.shortLabel || type || "Sale";
}

export function getCategoryLabel(cat) {
  return PROPERTY_CATEGORIES.find((c) => c.value === cat)?.label || cat || "Apartment";
}

export function getPresetForCategory(cat) {
  return PROPERTY_CATEGORIES.find((c) => c.value === cat)?.preset || "preset:apartment";
}
