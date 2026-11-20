/**
 * Property Broker — Admin Web Portal UI Controller
 */
import {
  ADMIN_EMAIL,
  PREMIUM_ACTIVATION_ISO,
  PREMIUM_PASS_DAYS_DEFAULT,
  PREMIUM_PASS_PRICE_DEFAULT,
  PROPERTY_CATEGORIES,
  USER_ROLES,
  downloadCsv,
  escapeHtml,
  formatINR,
  formatShortDate,
  formatTimestamp,
  getCategoryLabel,
  getListingTypeLabel,
  resolveIndianCoordinates
} from "./firebase-config.js";
import {
  checkRedirectSignIn,
  clearAuditLogs,
  deleteChatThread,
  deletePropertyCompletely,
  deleteUserProfile,
  fetchAllMemberships,
  fetchPropertiesOnce,
  getPropertyContact,
  isAuthorizedAdmin,
  pruneExpiredAuditLogs,
  recordAdminAuditLog,
  revokeUserPremiumPass,
  saveAppSettings,
  savePropertyWithContact,
  saveUserPremiumPass,
  signInAdminWithGoogle,
  signOutAdmin,
  subscribeAppSettings,
  subscribeAuditLogs,
  subscribeAuth,
  subscribeChatMessages,
  subscribeChats,
  subscribeProperties,
  subscribeUsers,
  togglePropertyStatus,
  togglePropertyVerified,
  updateUserRole
} from "./admin-service.js";
import { PropertyMapManager } from "./map-manager.js";
import { AnalyticsManager } from "./analytics-manager.js";

const VIEW_META = {
  overview: ["Overview Dashboard", "Real-time sync with your Property Broker Android app"],
  analytics: ["User Growth & DAU Analytics", "Interactive D3.js trends powered by live Firestore activity"],
  properties: ["Properties Management", "Add, edit, verify, reveal owner contacts, or close listings"],
  map: ["Interactive Property Map", "Free OpenStreetMap view of all active & closed listings"],
  users: ["Users & Roles", "Manage registered buyers, owners, renters & accounts"],
  premium: ["7-Day Premium Passes", "Grant, extend, or revoke ₹999 7-Day Passes live"],
  chats: ["Chats & Inquiries Monitor", "Inspect buyer-owner negotiations & remove spam"],
  settings: ["Settings, Hosting & Audit Log", "Manage /app_config/settings, free hosting & admin audit trail"]
};

const AUDIT_ACTION_META = {
  PROPERTY_CREATE: { label: "Property Created", badge: "badge-active" },
  PROPERTY_EDIT: { label: "Property Edited", badge: "badge-sale" },
  PROPERTY_STATUS: { label: "Status Changed", badge: "badge-heavy_deposit" },
  PROPERTY_VERIFY: { label: "Verification Updated", badge: "badge-verified" },
  PROPERTY_DELETE: { label: "Property Deleted", badge: "badge-closed" },
  USER_ROLE: { label: "Role Changed", badge: "badge-sale" },
  USER_DELETE: { label: "User Removed", badge: "badge-closed" },
  PASS_GRANT: { label: "7-Day Pass Saved", badge: "badge-active" },
  PASS_REVOKE: { label: "Pass Revoked", badge: "badge-closed" },
  CHAT_DELETE: { label: "Chat Deleted", badge: "badge-closed" },
  SETTINGS_UPDATE: { label: "Settings Updated", badge: "badge-verified" }
};

const LIVE_SYNC_INTERVAL_SEC = 30;
const LIVE_SYNC_STORAGE_KEY = "pb_admin_prop_live_sync";
const AUDIT_RETENTION_DAYS = 30;
const AUDIT_AUTOCLEAN_STORAGE_KEY = "pb_admin_audit_autoclean_30d";
const AUDIT_AUTOCLEAN_INTERVAL_MS = 6 * 60 * 60 * 1000;

const state = {
  adminUser: null,
  activeView: "overview",
  settingsTab: "config",
  properties: [],
  users: [],
  chats: [],
  auditLogs: [],
  memberships: new Map(),
  priceBand: "ALL",
  unsubscribers: [],
  chatMsgUnsub: null,
  activeChatId: null,
  liveSyncEnabled: false,
  liveSyncPollTimer: null,
  liveSyncTickTimer: null,
  liveSyncSecondsLeft: LIVE_SYNC_INTERVAL_SEC,
  liveSyncInFlight: false,
  liveSyncLastSyncedAt: null,
  auditAutoCleanEnabled: false,
  auditAutoCleanInFlight: false,
  auditAutoCleanTimer: null,
  auditLastCleanedAt: null,
  auditLastAutoRunMs: 0,
  previewPropertyId: null,
  previewImageIndex: 0,
  previewContactReqId: 0,
  previewContactData: undefined
};

const $ = (id) => document.getElementById(id);

const mapManager = new PropertyMapManager("property-map", {
  onEditProperty: (prop) => openPropertyModal(prop),
  onViewContact: (prop) => openContactVault(prop)
});
const analyticsManager = new AnalyticsManager();

function renderAnalyticsIfVisible() {
  if (state.activeView === "analytics") {
    analyticsManager.render({
      users: state.users,
      properties: state.properties,
      chats: state.chats
    });
  }
}

// ---------------- Toast & Theme ----------------

function showToast(message, type = "info") {
  const stack = $("toast-stack");
  const el = document.createElement("div");
  el.className = `toast ${type}`;
  el.textContent = message;
  stack.appendChild(el);
  setTimeout(() => el.remove(), 3800);
}

function initTableHeaderTooltips() {
  let tip = document.getElementById("ui-header-tooltip");
  if (!tip) {
    tip = document.createElement("div");
    tip.id = "ui-header-tooltip";
    tip.className = "ui-header-tooltip hidden";
    tip.setAttribute("role", "tooltip");
    document.body.appendChild(tip);
  }

  const positionTooltip = (anchorRect) => {
    const pad = 12;
    const tipWidth = tip.offsetWidth || 260;
    const tipHeight = tip.offsetHeight || 68;
    const left = Math.max(
      pad,
      Math.min(window.innerWidth - tipWidth - pad, anchorRect.left + anchorRect.width / 2 - tipWidth / 2)
    );
    const fitsAbove = anchorRect.top - tipHeight - 10 >= pad;
    const top = fitsAbove ? anchorRect.top - tipHeight - 10 : anchorRect.bottom + 10;
    tip.style.left = `${left}px`;
    tip.style.top = `${top}px`;
  };

  const show = (trigger) => {
    const title = trigger.dataset.tooltipTitle || "";
    const text = trigger.dataset.tooltip || "";
    if (!text) return;
    tip.innerHTML = `${title ? `<strong>${escapeHtml(title)}</strong>` : ""}<span>${escapeHtml(text)}</span>`;
    tip.classList.remove("hidden");
    positionTooltip(trigger.getBoundingClientRect());
  };

  const hide = () => tip.classList.add("hidden");

  document.querySelectorAll(".th-tooltip-trigger").forEach((trigger) => {
    trigger.addEventListener("mouseenter", () => show(trigger));
    trigger.addEventListener("focus", () => show(trigger));
    trigger.addEventListener("mouseleave", hide);
    trigger.addEventListener("blur", hide);
  });
  window.addEventListener("scroll", hide, true);
}

function initTheme() {
  const saved = localStorage.getItem("pb_admin_theme") || "light";
  document.documentElement.setAttribute("data-theme", saved);
  $("btn-theme-toggle").textContent = saved === "dark" ? "☀️" : "🌙";
}

function toggleTheme() {
  const cur = document.documentElement.getAttribute("data-theme") || "light";
  const next = cur === "dark" ? "light" : "dark";
  document.documentElement.setAttribute("data-theme", next);
  localStorage.setItem("pb_admin_theme", next);
  $("btn-theme-toggle").textContent = next === "dark" ? "☀️" : "🌙";
  renderAnalyticsIfVisible();
}

// ---------------- 1 Jan 2027 Countdown ----------------

function updateCountdown() {
  const target = new Date(PREMIUM_ACTIVATION_ISO).getTime();
  const diff = target - Date.now();
  if (diff <= 0) {
    $("cd-days").textContent = "0";
    $("cd-hours").textContent = "0";
    $("cd-mins").textContent = "0";
    $("countdown-desc").innerHTML =
      "<strong>Premium Paywall is LIVE!</strong> Add Property &amp; Contact Vault require an active 7-Day Premium Pass (₹999).";
    return;
  }
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff / (1000 * 60 * 60)) % 24);
  const mins = Math.floor((diff / (1000 * 60)) % 60);
  $("cd-days").textContent = String(days);
  $("cd-hours").textContent = String(hours).padStart(2, "0");
  $("cd-mins").textContent = String(mins).padStart(2, "0");
}

// ---------------- Navigation & Modals ----------------

function switchView(viewName) {
  state.activeView = viewName;
  document.querySelectorAll(".view-section").forEach((s) => s.classList.add("hidden"));
  $(`view-${viewName}`)?.classList.remove("hidden");

  document.querySelectorAll(".nav-item").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.view === viewName);
  });

  const [title, sub] = VIEW_META[viewName] || ["Admin", ""];
  $("view-title").textContent = title;
  $("view-subtitle").textContent = sub;

  closeMobileSidebar();

  if (viewName === "map") {
    mapManager.render(state.properties);
  }
  if (viewName === "analytics") {
    analyticsManager.render({
      users: state.users,
      properties: state.properties,
      chats: state.chats
    });
  }
  if (viewName !== "properties" && state.previewPropertyId) {
    closePropertyPreview();
  }
  if (viewName === "premium") {
    refreshMemberships();
  }
}

function closeMobileSidebar() {
  $("sidebar").classList.remove("open");
  $("sidebar-backdrop").classList.remove("open");
}

function openModal(id) {
  $(id)?.classList.remove("hidden");
}

function closeModal(id) {
  $(id)?.classList.add("hidden");
  if (id === "modal-chat" && state.chatMsgUnsub) {
    state.chatMsgUnsub();
    state.chatMsgUnsub = null;
    state.activeChatId = null;
  }
}

// ---------------- Live Data Subscriptions ----------------

function clearSubscriptions() {
  stopPropertyLiveSync();
  stopAuditAutoCleanScheduler();
  closePropertyPreview();
  state.unsubscribers.forEach((u) => { try { u(); } catch {} });
  state.unsubscribers = [];
  if (state.chatMsgUnsub) {
    state.chatMsgUnsub();
    state.chatMsgUnsub = null;
  }
}

function applyPropertiesUpdate(items) {
  state.properties = items;
  $("nav-count-properties").textContent = String(items.length);
  renderOverview();
  renderPropertiesTable();
  renderUsersTable();
  if (state.activeView === "map") mapManager.render(items);
  if (state.previewPropertyId) {
    const stillExists = items.some((p) => p.id === state.previewPropertyId);
    if (stillExists) {
      renderPropertyPreview({ reloadContact: false });
    } else {
      closePropertyPreview();
    }
  }
  renderAnalyticsIfVisible();
}

function updateLiveSyncUi() {
  const input = $("prop-live-sync");
  const wrap = $("prop-live-sync-wrap");
  const statusEl = $("prop-live-sync-status");
  if (!input || !wrap || !statusEl) return;

  input.checked = state.liveSyncEnabled;
  input.setAttribute("aria-checked", String(state.liveSyncEnabled));
  wrap.classList.toggle("is-active", state.liveSyncEnabled);
  wrap.classList.toggle("is-syncing", state.liveSyncInFlight);

  if (!state.liveSyncEnabled) {
    statusEl.textContent = "Off";
    wrap.title = "Enable to automatically poll Firestore every 30 seconds for updated property listings";
    return;
  }

  if (state.liveSyncInFlight) {
    statusEl.textContent = "Syncing…";
  } else {
    statusEl.textContent = `${state.liveSyncSecondsLeft}s`;
  }

  const lastLabel = state.liveSyncLastSyncedAt
    ? ` • Last synced ${state.liveSyncLastSyncedAt.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", second: "2-digit" })}`
    : "";
  wrap.title = `Live Sync active — polling Firestore every ${LIVE_SYNC_INTERVAL_SEC}s${lastLabel}`;
}

async function pollPropertiesNow({ notifyOnChange = false } = {}) {
  if (!state.adminUser || state.liveSyncInFlight) return;
  state.liveSyncInFlight = true;
  updateLiveSyncUi();

  const prevSignature = state.properties
    .map((p) => `${p.id}:${p.updatedAt?.seconds || 0}:${p.status}:${p.verifiedListing}:${p.price}`)
    .join("|");

  try {
    const freshItems = await fetchPropertiesOnce();
    const nextSignature = freshItems
      .map((p) => `${p.id}:${p.updatedAt?.seconds || 0}:${p.status}:${p.verifiedListing}:${p.price}`)
      .join("|");

    applyPropertiesUpdate(freshItems);
    state.liveSyncLastSyncedAt = new Date();
    state.liveSyncSecondsLeft = LIVE_SYNC_INTERVAL_SEC;

    if (notifyOnChange && prevSignature && prevSignature !== nextSignature) {
      showToast(`Live Sync updated ${freshItems.length} property listings.`, "success");
    }
  } catch (err) {
    showToast(`Live Sync poll failed: ${err?.message || "Network error"}`, "error");
  } finally {
    state.liveSyncInFlight = false;
    updateLiveSyncUi();
  }
}

function stopPropertyLiveSync() {
  if (state.liveSyncPollTimer) {
    clearInterval(state.liveSyncPollTimer);
    state.liveSyncPollTimer = null;
  }
  if (state.liveSyncTickTimer) {
    clearInterval(state.liveSyncTickTimer);
    state.liveSyncTickTimer = null;
  }
  state.liveSyncInFlight = false;
  state.liveSyncSecondsLeft = LIVE_SYNC_INTERVAL_SEC;
  updateLiveSyncUi();
}

function startPropertyLiveSync({ immediatePoll = true } = {}) {
  stopPropertyLiveSync();
  if (!state.liveSyncEnabled || !state.adminUser) return;

  state.liveSyncSecondsLeft = LIVE_SYNC_INTERVAL_SEC;
  updateLiveSyncUi();

  if (immediatePoll) {
    pollPropertiesNow({ notifyOnChange: false });
  }

  state.liveSyncPollTimer = setInterval(() => {
    if (state.liveSyncEnabled && state.adminUser) {
      pollPropertiesNow({ notifyOnChange: true });
    }
  }, LIVE_SYNC_INTERVAL_SEC * 1000);

  state.liveSyncTickTimer = setInterval(() => {
    if (!state.liveSyncEnabled || state.liveSyncInFlight) return;
    state.liveSyncSecondsLeft = state.liveSyncSecondsLeft > 1
      ? state.liveSyncSecondsLeft - 1
      : LIVE_SYNC_INTERVAL_SEC;
    updateLiveSyncUi();
  }, 1000);
}

function initPropertyLiveSync() {
  state.liveSyncEnabled = localStorage.getItem(LIVE_SYNC_STORAGE_KEY) === "1";
  updateLiveSyncUi();
}

function startLiveSubscriptions() {
  clearSubscriptions();

  state.unsubscribers.push(
    subscribeProperties(
      (items) => {
        applyPropertiesUpdate(items);
      },
      (err) => showToast(`Properties sync error: ${err.message}`, "error")
    )
  );

  if (state.liveSyncEnabled) {
    startPropertyLiveSync({ immediatePoll: false });
  }

  state.unsubscribers.push(
    subscribeUsers(
      (items) => {
        state.users = items;
        $("nav-count-users").textContent = String(items.length);
        renderOverview();
        renderUsersTable();
        refreshMemberships();
        renderAnalyticsIfVisible();
      },
      (err) => showToast(`Users sync error: ${err.message}`, "error")
    )
  );

  state.unsubscribers.push(
    subscribeChats(
      (items) => {
        state.chats = items;
        $("nav-count-chats").textContent = String(items.length);
        renderOverview();
        renderChatsTable();
        renderAnalyticsIfVisible();
      },
      (err) => showToast(`Chats sync error: ${err.message}`, "error")
    )
  );

  state.unsubscribers.push(
    subscribeAppSettings((cfg) => {
      if (!cfg) return;
      if (cfg.latestVersion) $("cfg-latest-version").value = cfg.latestVersion;
      if (cfg.minSupportedVersion) $("cfg-min-version").value = cfg.minSupportedVersion;
      if (cfg.premiumPriceInr != null) $("cfg-premium-price").value = cfg.premiumPriceInr;
      if (cfg.premiumDurationDays != null) $("cfg-premium-days").value = cfg.premiumDurationDays;
      if (cfg.premiumActivationDate) $("cfg-launch-date").value = cfg.premiumActivationDate;
      if (cfg.supportEmail) $("cfg-support-email").value = cfg.supportEmail;
      if (cfg.supportWhatsapp != null) $("cfg-support-whatsapp").value = cfg.supportWhatsapp;
      if (cfg.playStoreUrl != null) $("cfg-play-url").value = cfg.playStoreUrl;
      if (cfg.announcementBanner != null) $("cfg-announcement").value = cfg.announcementBanner;
      $("cfg-maintenance").checked = Boolean(cfg.maintenanceMode);

      if (typeof cfg.auditAutoClean30Days === "boolean") {
        const changed = state.auditAutoCleanEnabled !== cfg.auditAutoClean30Days;
        state.auditAutoCleanEnabled = cfg.auditAutoClean30Days;
        localStorage.setItem(AUDIT_AUTOCLEAN_STORAGE_KEY, state.auditAutoCleanEnabled ? "1" : "0");
        if (state.auditAutoCleanEnabled) {
          startAuditAutoCleanScheduler({ runNow: changed || state.auditLastAutoRunMs === 0 });
        } else {
          stopAuditAutoCleanScheduler();
        }
      }
    })
  );

  state.unsubscribers.push(
    subscribeAuditLogs(
      (items) => {
        state.auditLogs = items;
        const badge = $("audit-tab-count");
        if (badge) badge.textContent = String(items.length);
        renderAuditLogTable();
        updateAuditAutoCleanUi();

        if (
          state.auditAutoCleanEnabled &&
          !state.auditAutoCleanInFlight &&
          Date.now() - state.auditLastAutoRunMs > 60_000 &&
          items.some(isAuditEntryExpired)
        ) {
          runAuditAutoClean({ manual: false });
        }
      },
      (err) => showToast(`Audit log sync error: ${err.message}`, "error")
    )
  );

  if (state.auditAutoCleanEnabled) {
    startAuditAutoCleanScheduler({ runNow: true });
  }
}

function getAuditEntryMillis(entry) {
  const ts = entry?.createdAt;
  if (!ts) return Date.now();
  if (typeof ts.toMillis === "function") return ts.toMillis();
  if (typeof ts.seconds === "number") return ts.seconds * 1000;
  if (typeof ts === "number") return ts;
  const parsed = Date.parse(ts);
  return Number.isNaN(parsed) ? Date.now() : parsed;
}

function isAuditEntryExpired(entry) {
  const cutoffMs = Date.now() - AUDIT_RETENTION_DAYS * 24 * 60 * 60 * 1000;
  return getAuditEntryMillis(entry) < cutoffMs;
}

function updateAuditAutoCleanUi() {
  const input = $("audit-autoclean-toggle");
  const wrap = $("audit-autoclean-wrap");
  const statusEl = $("audit-autoclean-status");
  const hintEl = $("audit-autoclean-hint");
  const pruneBtn = $("btn-prune-audit-now");
  if (!input || !wrap || !statusEl) return;

  const expiredCount = state.auditLogs.filter(isAuditEntryExpired).length;

  input.checked = state.auditAutoCleanEnabled;
  input.setAttribute("aria-checked", String(state.auditAutoCleanEnabled));
  wrap.classList.toggle("is-active", state.auditAutoCleanEnabled);
  wrap.classList.toggle("is-syncing", state.auditAutoCleanInFlight);
  if (pruneBtn) pruneBtn.disabled = state.auditAutoCleanInFlight;

  if (state.auditAutoCleanInFlight) {
    statusEl.textContent = "Cleaning…";
  } else if (state.auditAutoCleanEnabled) {
    statusEl.textContent = `${AUDIT_RETENTION_DAYS}d Active`;
  } else {
    statusEl.textContent = "Off";
  }

  if (hintEl) {
    if (state.auditAutoCleanInFlight) {
      hintEl.textContent = `Scanning and deleting audit entries older than ${AUDIT_RETENTION_DAYS} days…`;
    } else if (state.auditAutoCleanEnabled) {
      const lastText = state.auditLastCleanedAt
        ? ` Last checked ${state.auditLastCleanedAt.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}.`
        : "";
      hintEl.textContent = `Active: entries older than ${AUDIT_RETENTION_DAYS} days are automatically deleted to keep Firestore & UI fast.${lastText}`;
    } else if (expiredCount > 0) {
      hintEl.textContent = `${expiredCount} loaded ${expiredCount === 1 ? "entry is" : "entries are"} older than ${AUDIT_RETENTION_DAYS} days. Enable Auto-Clean or click Clean >30d Now.`;
    } else {
      hintEl.textContent = `Optional: enable to automatically delete Firestore audit entries older than ${AUDIT_RETENTION_DAYS} days.`;
    }
  }
}

async function runAuditAutoClean({ manual = false } = {}) {
  if (!state.adminUser || state.auditAutoCleanInFlight) return;
  state.auditAutoCleanInFlight = true;
  state.auditLastAutoRunMs = Date.now();
  updateAuditAutoCleanUi();

  try {
    const { deletedCount } = await pruneExpiredAuditLogs(AUDIT_RETENTION_DAYS);
    state.auditLastCleanedAt = new Date();
    if (deletedCount > 0) {
      showToast(
        `Auto-Clean removed ${deletedCount} audit ${deletedCount === 1 ? "entry" : "entries"} older than ${AUDIT_RETENTION_DAYS} days.`,
        "success"
      );
    } else if (manual) {
      showToast(`Audit log is clean — no entries older than ${AUDIT_RETENTION_DAYS} days found.`);
    }
  } catch (err) {
    showToast(`Audit cleanup failed: ${err?.message || "Unknown error"}`, "error");
  } finally {
    state.auditAutoCleanInFlight = false;
    updateAuditAutoCleanUi();
  }
}

function stopAuditAutoCleanScheduler() {
  if (state.auditAutoCleanTimer) {
    clearInterval(state.auditAutoCleanTimer);
    state.auditAutoCleanTimer = null;
  }
  updateAuditAutoCleanUi();
}

function startAuditAutoCleanScheduler({ runNow = false } = {}) {
  stopAuditAutoCleanScheduler();
  if (!state.auditAutoCleanEnabled || !state.adminUser) return;

  if (runNow) {
    runAuditAutoClean({ manual: false });
  }

  state.auditAutoCleanTimer = setInterval(() => {
    if (state.auditAutoCleanEnabled && state.adminUser) {
      runAuditAutoClean({ manual: false });
    }
  }, AUDIT_AUTOCLEAN_INTERVAL_MS);
  updateAuditAutoCleanUi();
}

function initAuditAutoClean() {
  state.auditAutoCleanEnabled = localStorage.getItem(AUDIT_AUTOCLEAN_STORAGE_KEY) === "1";
  updateAuditAutoCleanUi();
}

function logAudit(action, entityType, entityId, summary, details = "") {
  recordAdminAuditLog({
    action,
    entityType,
    entityId,
    summary,
    details,
    adminEmail: state.adminUser?.email || ADMIN_EMAIL
  });
}

function switchSettingsTab(tabName) {
  state.settingsTab = tabName;
  $("settings-panel-config")?.classList.toggle("hidden", tabName !== "config");
  $("settings-panel-audit")?.classList.toggle("hidden", tabName !== "audit");
  document.querySelectorAll("#settings-subtabs .subtab-btn").forEach((btn) => {
    const isActive = btn.dataset.settingsTab === tabName;
    btn.classList.toggle("active", isActive);
    btn.setAttribute("aria-selected", String(isActive));
  });
  if (tabName === "audit") {
    renderAuditLogTable();
    updateAuditAutoCleanUi();
  }
}

function getFilteredAuditLogs() {
  const q = ($("audit-search")?.value || "").toLowerCase().trim();
  const entityFilter = $("audit-filter-entity")?.value || "ALL";
  return state.auditLogs.filter((entry) => {
    if (entityFilter !== "ALL" && entry.entityType !== entityFilter) return false;
    if (q) {
      const hay = `${entry.action} ${entry.entityType} ${entry.entityId} ${entry.summary} ${entry.details || ""} ${entry.adminEmail}`.toLowerCase();
      if (!hay.includes(q)) return false;
    }
    return true;
  });
}

function renderAuditLogTable() {
  const tbody = $("audit-tbody");
  if (!tbody) return;
  const list = getFilteredAuditLogs();
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="5"><div class="empty-state">No audit actions recorded yet. Property edits, status toggles, role updates, pass grants, and settings changes will appear here automatically.</div></td></tr>`;
    return;
  }

  tbody.innerHTML = list
    .map((entry) => {
      const meta = AUDIT_ACTION_META[entry.action] || { label: entry.action, badge: "badge-neutral" };
      const agedTag = isAuditEntryExpired(entry)
        ? `<span class="badge-aged" title="Older than ${AUDIT_RETENTION_DAYS} days">&gt;${AUDIT_RETENTION_DAYS}d</span>`
        : "";
      return `
      <tr>
        <td class="cell-sub" style="white-space:nowrap;">${escapeHtml(formatTimestamp(entry.createdAt))}${agedTag}</td>
        <td><span class="badge ${meta.badge}">${escapeHtml(meta.label)}</span></td>
        <td>
          <div class="cell-title">${escapeHtml(entry.summary)}</div>
          ${entry.details ? `<div class="cell-sub">${escapeHtml(entry.details)}</div>` : ""}
        </td>
        <td>
          <span class="badge badge-neutral">${escapeHtml(entry.entityType)}</span>
          <div class="cell-sub" style="margin-top:3px;"><code>${escapeHtml(String(entry.entityId || "").slice(0, 18))}</code></div>
        </td>
        <td class="cell-sub">${escapeHtml(entry.adminEmail || ADMIN_EMAIL)}</td>
      </tr>`;
    })
    .join("");
}

async function refreshMemberships() {
  if (!state.users.length) {
    state.memberships = new Map();
    renderPremiumTable();
    renderOverview();
    return;
  }
  state.memberships = await fetchAllMemberships(state.users);
  const activeCount = [...state.memberships.values()].filter(
    (m) => m.premiumActive && (!m.expiresAtMillis || m.expiresAtMillis > Date.now())
  ).length;
  $("nav-count-premium").textContent = String(activeCount);
  renderOverview();
  renderPremiumTable();
}

// ---------------- Rendering: Overview ----------------

function renderOverview() {
  const props = state.properties;
  const activeProps = props.filter((p) => p.status !== "CLOSED").length;
  const closedProps = props.length - activeProps;
  const saleCount = props.filter((p) => p.listingType === "SALE").length;
  const rentCount = props.filter((p) => p.listingType === "RENT").length;
  const heavyCount = props.filter((p) => p.listingType === "HEAVY_DEPOSIT").length;
  const verifiedCount = props.filter((p) => p.verifiedListing).length;

  const activePasses = [...state.memberships.values()].filter(
    (m) => m.premiumActive && (!m.expiresAtMillis || m.expiresAtMillis > Date.now())
  ).length;

  $("kpi-total-props").textContent = String(props.length);
  $("kpi-active-props").textContent = `${activeProps} Active • ${closedProps} Closed`;
  $("kpi-type-split").textContent = `${saleCount} / ${rentCount} / ${heavyCount}`;
  $("kpi-total-users").textContent = String(state.users.length);
  $("kpi-active-passes").textContent = String(activePasses);
  $("kpi-total-chats").textContent = String(state.chats.length);
  $("overview-verified-count").textContent = `${verifiedCount} Verified`;

  const owners = state.users.filter((u) => u.role === "OWNER" || u.role === "BOTH").length;
  const buyers = state.users.length - owners;
  $("kpi-role-split").textContent = `${owners} Owners/Brokers • ${buyers} Buyers/Renters`;

  // Recent properties list
  const recentContainer = $("overview-recent-props");
  if (!props.length) {
    recentContainer.innerHTML = `<div class="empty-state">No properties yet. Click <strong>+ Add Property</strong> to publish your first listing.</div>`;
  } else {
    recentContainer.innerHTML = `<div class="table-wrap"><table><tbody>${props
      .slice(0, 6)
      .map(
        (p) => `
      <tr>
        <td>
          <div class="cell-title">${escapeHtml(p.title)}</div>
          <div class="cell-sub">${escapeHtml(p.location)} • ${escapeHtml(p.ownerName || "Owner")}</div>
        </td>
        <td><span class="badge badge-${(p.listingType || "sale").toLowerCase()}">${escapeHtml(getListingTypeLabel(p.listingType))}</span></td>
        <td><strong>${escapeHtml(formatINR(p.price, p.listingType))}</strong></td>
      </tr>`
      )
      .join("")}</tbody></table></div>`;
  }

  // Category distribution bars
  const barsContainer = $("overview-category-bars");
  const total = Math.max(1, props.length);
  const counts = PROPERTY_CATEGORIES.map((c) => ({
    label: c.label,
    count: props.filter((p) => p.propertyCategory === c.value).length
  })).filter((c) => c.count > 0);

  if (!counts.length) {
    barsContainer.innerHTML = `<div class="empty-state">Category analytics will appear once listings exist.</div>`;
  } else {
    counts.sort((a, b) => b.count - a.count);
    barsContainer.innerHTML = counts
      .map((c) => {
        const pct = Math.round((c.count / total) * 100);
        return `
          <div class="bar-row">
            <div class="bar-meta"><span>${escapeHtml(c.label)}</span><span>${c.count} (${pct}%)</span></div>
            <div class="bar-track"><div class="bar-fill" style="width:${pct}%"></div></div>
          </div>`;
      })
      .join("");
  }
}

// ---------------- Rendering: Properties ----------------

function getFilteredProperties() {
  const q = ($("prop-search").value || "").toLowerCase().trim();
  const type = $("prop-filter-type").value;
  const cat = $("prop-filter-category").value;
  const status = $("prop-filter-status").value;

  return state.properties.filter((p) => {
    if (type !== "ALL" && p.listingType !== type) return false;
    if (cat !== "ALL" && p.propertyCategory !== cat) return false;
    if (status !== "ALL" && p.status !== status) return false;

    const price = Number(p.price) || 0;
    if (state.priceBand === "UNDER_50L" && price >= 5000000) return false;
    if (state.priceBand === "50L_1CR" && (price < 5000000 || price > 10000000)) return false;
    if (state.priceBand === "ABOVE_1CR" && price <= 10000000) return false;

    if (q) {
      const hay = `${p.title} ${p.location} ${p.ownerName} ${p.price} ${formatINR(p.price, p.listingType)}`.toLowerCase();
      if (!hay.includes(q)) return false;
    }
    return true;
  });
}

function renderPropertiesTable() {
  const list = getFilteredProperties();
  const tbody = $("properties-tbody");
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="6"><div class="empty-state">No matching properties found.</div></td></tr>`;
    return;
  }

  tbody.innerHTML = list
    .map(
      (p) => `
    <tr data-id="${escapeHtml(p.id)}" class="${state.previewPropertyId === p.id ? "is-selected" : ""}">
      <td>
        <div class="cell-title">${escapeHtml(p.title)}</div>
        <div class="cell-sub">📍 ${escapeHtml(p.location)} • ${p.bedrooms || 0} BHK • ${p.areaSqFt || 0} sq.ft</div>
      </td>
      <td>
        <span class="badge badge-${(p.listingType || "sale").toLowerCase()}">${escapeHtml(getListingTypeLabel(p.listingType))}</span>
        <div class="cell-sub" style="margin-top:4px;">${escapeHtml(getCategoryLabel(p.propertyCategory))}</div>
      </td>
      <td><strong>${escapeHtml(formatINR(p.price, p.listingType))}</strong></td>
      <td>
        <div class="cell-title">${escapeHtml(p.ownerName || "Unknown")}</div>
        <div class="cell-sub">${escapeHtml(formatTimestamp(p.updatedAt))}</div>
      </td>
      <td>
        <div style="display:flex;flex-direction:column;gap:4px;align-items:flex-start;">
          <span class="badge ${p.status === "CLOSED" ? "badge-closed" : "badge-active"}">${escapeHtml(p.status || "ACTIVE")}</span>
          ${p.verifiedListing ? `<span class="badge badge-verified">✓ Verified</span>` : ""}
        </div>
      </td>
      <td>
        <div class="action-row">
          <button class="btn btn-secondary btn-sm js-prop-contact" data-id="${escapeHtml(p.id)}" type="button" title="Reveal Contact Vault">📞 Contact</button>
          <button class="btn btn-secondary btn-sm js-prop-edit" data-id="${escapeHtml(p.id)}" type="button">✏️ Edit</button>
          <button class="btn btn-secondary btn-sm js-prop-verify" data-id="${escapeHtml(p.id)}" type="button">${p.verifiedListing ? "Unverify" : "✓ Verify"}</button>
          <button class="btn btn-secondary btn-sm js-prop-status" data-id="${escapeHtml(p.id)}" type="button">${p.status === "ACTIVE" ? "Close" : "Reopen"}</button>
          <button class="btn btn-danger btn-sm js-prop-delete" data-id="${escapeHtml(p.id)}" type="button">🗑</button>
        </div>
      </td>
    </tr>`
    )
    .join("");
}

// ---------------- Side-Panel Property Preview ----------------

const PRESET_VISUAL_META = {
  apartment: { icon: "🏢", label: "Apartment / Flat", tone: "apartment" },
  villa: { icon: "🏡", label: "Villa / Bungalow", tone: "villa" },
  house: { icon: "🏠", label: "Independent House", tone: "house" },
  shop: { icon: "🏪", label: "Shop / Showroom", tone: "shop" },
  gala: { icon: "🏭", label: "Industrial Gala", tone: "gala" },
  office: { icon: "💼", label: "Office Space", tone: "office" },
  studio: { icon: "🛋️", label: "Studio", tone: "studio" },
  commercial: { icon: "🏬", label: "Commercial", tone: "commercial" },
  plot: { icon: "🗺️", label: "Plot / Land", tone: "plot" }
};

function isRenderableImageUrl(rawKey) {
  const s = String(rawKey || "").trim();
  return /^https?:\/\//i.test(s) || /^data:image\/(png|jpeg|jpg|webp|gif);base64,/i.test(s);
}

function getPresetVisual(rawKey, category) {
  const normalized = String(rawKey || "").toLowerCase().replace(/^preset:/, "").trim();
  const catKey = String(category || "apartment").toLowerCase().trim();
  return PRESET_VISUAL_META[normalized] || PRESET_VISUAL_META[catKey] || PRESET_VISUAL_META.apartment;
}

function getPropertyMediaKeys(prop) {
  const keys = Array.isArray(prop?.photoKeys)
    ? prop.photoKeys.map((k) => String(k || "").trim()).filter(Boolean)
    : [];
  if (keys.length) return keys;
  const fallbackPreset = `preset:${String(prop?.propertyCategory || "apartment").toLowerCase()}`;
  return [fallbackPreset];
}

function renderPreviewGalleryHtml(prop) {
  const mediaKeys = getPropertyMediaKeys(prop);
  const safeIndex = Math.max(0, Math.min(state.previewImageIndex, mediaKeys.length - 1));
  state.previewImageIndex = safeIndex;
  const currentKey = mediaKeys[safeIndex];
  const preset = getPresetVisual(currentKey, prop.propertyCategory);

  const stageContent = isRenderableImageUrl(currentKey)
    ? `<img class="preview-gallery-img js-preview-img" src="${escapeHtml(currentKey)}" alt="${escapeHtml(prop.title || "Property image")}" loading="lazy" />`
    : `<div class="preview-preset-art" data-tone="${escapeHtml(preset.tone)}">
         <div class="preview-preset-icon">${preset.icon}</div>
         <div class="preview-preset-title">${escapeHtml(preset.label)}</div>
         <span class="preview-preset-key">${escapeHtml(currentKey)}</span>
       </div>`;

  const navControls = mediaKeys.length > 1
    ? `<button class="preview-gallery-nav preview-gallery-prev js-preview-prev" type="button" aria-label="Previous image">‹</button>
       <button class="preview-gallery-nav preview-gallery-next js-preview-next" type="button" aria-label="Next image">›</button>
       <span class="preview-gallery-count">${safeIndex + 1} / ${mediaKeys.length}</span>`
    : "";

  const thumbs = mediaKeys.length > 1
    ? `<div class="preview-thumbs">
        ${mediaKeys
          .map((key, idx) => {
            const pMeta = getPresetVisual(key, prop.propertyCategory);
            const inner = isRenderableImageUrl(key)
              ? `<img src="${escapeHtml(key)}" alt="Photo ${idx + 1}" loading="lazy" />`
              : `<span>${pMeta.icon}</span>`;
            return `<button class="preview-thumb-btn js-preview-thumb ${idx === safeIndex ? "active" : ""}" data-index="${idx}" type="button" aria-label="View image ${idx + 1}">${inner}</button>`;
          })
          .join("")}
      </div>`
    : "";

  return `
    <div>
      <div class="preview-gallery-stage">
        ${stageContent}
        ${navControls}
      </div>
      ${thumbs}
      ${prop.hasVideo ? `<div class="cell-sub" style="margin-top:8px;">🎬 Video attached${prop.videoLabel ? `: ${escapeHtml(prop.videoLabel)}` : ""}</div>` : ""}
    </div>`;
}

function openPropertyPreview(propertyId) {
  if (!propertyId) return;
  if (state.previewPropertyId === propertyId) {
    closePropertyPreview();
    return;
  }
  state.previewPropertyId = propertyId;
  state.previewImageIndex = 0;
  state.previewContactData = undefined;
  $("property-preview-backdrop")?.classList.remove("hidden");
  $("property-preview-drawer")?.classList.add("open");
  $("property-preview-drawer")?.setAttribute("aria-hidden", "false");
  renderPropertiesTable();
  renderPropertyPreview({ reloadContact: true });
}

function closePropertyPreview() {
  if (!state.previewPropertyId) return;
  state.previewPropertyId = null;
  state.previewImageIndex = 0;
  state.previewContactData = undefined;
  state.previewContactReqId += 1;
  $("property-preview-drawer")?.classList.remove("open");
  $("property-preview-drawer")?.setAttribute("aria-hidden", "true");
  $("property-preview-backdrop")?.classList.add("hidden");
  renderPropertiesTable();
}

function renderPreviewContactBox(contact) {
  const box = $("preview-contact-card");
  if (!box) return;
  if (!contact) {
    box.innerHTML = `<div class="cell-sub">No private contact vault found. Click <strong>Edit</strong> to add owner phone or email.</div>`;
    return;
  }
  const cleanPhone = (contact.contactPhone || "").replace(/[^0-9+]/g, "");
  const waDigits = cleanPhone.replace(/^\+/, "");
  box.innerHTML = `
    <div>
      <div class="cell-title" style="font-size:15px;">${escapeHtml(contact.contactPhone || "Not provided")}</div>
      <div class="cell-sub">${escapeHtml(contact.contactEmail || "No email provided")}</div>
    </div>
    <div class="action-row">
      ${cleanPhone ? `<a class="btn btn-primary btn-sm" href="tel:${escapeHtml(cleanPhone)}">📞 Call</a>` : ""}
      ${contact.whatsappEnabled && waDigits ? `<a class="btn btn-emerald btn-sm" target="_blank" rel="noopener" href="https://wa.me/${escapeHtml(waDigits)}">💬 WhatsApp</a>` : ""}
      ${contact.contactPhone ? `<button class="btn btn-secondary btn-sm js-preview-copy-phone" data-phone="${escapeHtml(contact.contactPhone)}" type="button">📋 Copy</button>` : ""}
    </div>
  `;
}

function renderPropertyPreview({ reloadContact = true } = {}) {
  const prop = state.properties.find((p) => p.id === state.previewPropertyId);
  if (!prop) {
    closePropertyPreview();
    return;
  }

  $("preview-prop-title").textContent = prop.title || "Untitled Listing";

  const hasCoords =
    prop.latitude != null && prop.longitude != null &&
    !Number.isNaN(Number(prop.latitude)) && !Number.isNaN(Number(prop.longitude));
  const coordsDisplay = hasCoords
    ? `${Number(prop.latitude).toFixed(4)}, ${Number(prop.longitude).toFixed(4)}`
    : "Auto-resolved from locality";
  const mapsUrl = hasCoords
    ? `https://www.openstreetmap.org/?mlat=${encodeURIComponent(prop.latitude)}&mlon=${encodeURIComponent(prop.longitude)}#map=16/${encodeURIComponent(prop.latitude)}/${encodeURIComponent(prop.longitude)}`
    : "";

  const descriptionText = (prop.description || "").trim();

  const body = $("property-preview-body");
  body.innerHTML = `
    ${renderPreviewGalleryHtml(prop)}

    <div>
      <div class="preview-price-row">
        <div class="preview-price">${escapeHtml(formatINR(prop.price, prop.listingType))}</div>
        <div class="preview-badge-row">
          <span class="badge badge-${(prop.listingType || "sale").toLowerCase()}">${escapeHtml(getListingTypeLabel(prop.listingType))}</span>
          <span class="badge ${prop.status === "CLOSED" ? "badge-closed" : "badge-active"}">${escapeHtml(prop.status || "ACTIVE")}</span>
          ${prop.verifiedListing ? `<span class="badge badge-verified">✓ Verified</span>` : ""}
        </div>
      </div>
      ${prop.listingType === "RENT" && Number(prop.depositAmount) > 0 ? `<div class="cell-sub" style="margin-top:4px;font-weight:700;color:var(--primary);">Deposit: ₹${Number(prop.depositAmount).toLocaleString("en-IN")}</div>` : ""}
      ${prop.listingType === "HEAVY_DEPOSIT" && Number(prop.monthlyRent) > 0 ? `<div class="cell-sub" style="margin-top:4px;font-weight:700;color:var(--primary);">Monthly Rent: ₹${Number(prop.monthlyRent).toLocaleString("en-IN")}/mo</div>` : ""}
      <div class="preview-location" style="margin-top:8px;">
        <span>📍 ${escapeHtml(prop.location || "Location not set")}</span>
        ${mapsUrl ? `<a href="${mapsUrl}" target="_blank" rel="noopener" style="font-size:12px;font-weight:700;">Open Map ↗</a>` : ""}
      </div>
    </div>

    <div class="preview-specs-grid">
      <div class="preview-spec-card">
        <div class="preview-spec-label">Category</div>
        <div class="preview-spec-value">${escapeHtml(getCategoryLabel(prop.propertyCategory))}</div>
      </div>
      <div class="preview-spec-card">
        <div class="preview-spec-label">Configuration</div>
        <div class="preview-spec-value">${prop.bedrooms || 0} BHK • ${Number(prop.areaSqFt || 0).toLocaleString("en-IN")} sq.ft</div>
      </div>
      ${(prop.floor || "").trim() ? `
      <div class="preview-spec-card">
        <div class="preview-spec-label">Floor</div>
        <div class="preview-spec-value">${escapeHtml(prop.floor)}</div>
      </div>` : ""}
      ${prop.listingType === "RENT" && Number(prop.depositAmount) > 0 ? `
      <div class="preview-spec-card">
        <div class="preview-spec-label">Deposit</div>
        <div class="preview-spec-value">₹${Number(prop.depositAmount).toLocaleString("en-IN")}</div>
      </div>` : ""}
      ${prop.listingType === "HEAVY_DEPOSIT" && Number(prop.monthlyRent) > 0 ? `
      <div class="preview-spec-card">
        <div class="preview-spec-label">Monthly Rent</div>
        <div class="preview-spec-value">₹${Number(prop.monthlyRent).toLocaleString("en-IN")}/mo</div>
      </div>` : ""}
      <div class="preview-spec-card">
        <div class="preview-spec-label">Building Age</div>
        <div class="preview-spec-value">${escapeHtml((prop.buildingAge || "").trim() || "Not specified")}</div>
      </div>
      <div class="preview-spec-card">
        <div class="preview-spec-label">Lift Available</div>
        <div class="preview-spec-value">${prop.hasLift === true ? "Yes" : prop.hasLift === false ? "No" : "Not specified"}</div>
      </div>
      <div class="preview-spec-card">
        <div class="preview-spec-label">Owner / Broker</div>
        <div class="preview-spec-value">${escapeHtml(prop.ownerName || "Unknown")}</div>
      </div>
      <div class="preview-spec-card">
        <div class="preview-spec-label">Last Updated</div>
        <div class="preview-spec-value">${escapeHtml(formatTimestamp(prop.updatedAt))}</div>
      </div>
      <div class="preview-spec-card">
        <div class="preview-spec-label">Coordinates</div>
        <div class="preview-spec-value">${escapeHtml(coordsDisplay)}</div>
      </div>
      <div class="preview-spec-card">
        <div class="preview-spec-label">Listing ID</div>
        <div class="preview-spec-value"><code>${escapeHtml(prop.id)}</code></div>
      </div>
    </div>

    <div>
      <h4 class="preview-section-title">Full Description</h4>
      <div class="preview-desc-box">${descriptionText ? escapeHtml(descriptionText) : `<span class="cell-sub">No description provided for this listing.</span>`}</div>
    </div>

    <div>
      <h4 class="preview-section-title">Private Owner Contact Vault</h4>
      <div id="preview-contact-card" class="preview-contact-card">
        <div class="cell-sub">Loading private contact details…</div>
      </div>
    </div>
  `;

  // Graceful fallback if an external image fails to load
  const mainImg = body.querySelector(".js-preview-img");
  if (mainImg) {
    mainImg.addEventListener("error", () => {
      const preset = getPresetVisual("", prop.propertyCategory);
      const stage = mainImg.parentElement;
      if (!stage) return;
      mainImg.remove();
      const fallback = document.createElement("div");
      fallback.className = "preview-preset-art";
      fallback.dataset.tone = preset.tone;
      fallback.innerHTML = `<div class="preview-preset-icon">${preset.icon}</div><div class="preview-preset-title">${escapeHtml(preset.label)}</div><span class="preview-preset-key">Image unavailable</span>`;
      stage.prepend(fallback);
    });
  }

  $("property-preview-footer").innerHTML = `
    <div class="action-row">
      <button class="btn btn-primary btn-sm js-prop-edit" data-id="${escapeHtml(prop.id)}" type="button">✏️ Edit</button>
      <button class="btn btn-secondary btn-sm js-prop-verify" data-id="${escapeHtml(prop.id)}" type="button">${prop.verifiedListing ? "Unverify" : "✓ Verify"}</button>
      <button class="btn btn-secondary btn-sm js-prop-status" data-id="${escapeHtml(prop.id)}" type="button">${prop.status === "ACTIVE" ? "Close" : "Reopen"}</button>
    </div>
    <button class="btn btn-danger btn-sm js-prop-delete" data-id="${escapeHtml(prop.id)}" type="button">🗑 Delete</button>
  `;

  if (!reloadContact && state.previewContactData !== undefined) {
    renderPreviewContactBox(state.previewContactData);
  } else {
    loadPreviewContactVault(prop.id);
  }
}

async function loadPreviewContactVault(propertyId) {
  const reqId = ++state.previewContactReqId;
  try {
    const contact = await getPropertyContact(propertyId);
    if (reqId !== state.previewContactReqId || state.previewPropertyId !== propertyId) return;
    state.previewContactData = contact || null;
    renderPreviewContactBox(state.previewContactData);
  } catch (err) {
    if (reqId !== state.previewContactReqId) return;
    const box = $("preview-contact-card");
    if (box) {
      box.innerHTML = `<div class="cell-sub">Unable to load contact vault: ${escapeHtml(err?.message || "Error")}</div>`;
    }
  }
}

async function handlePropertyActionClick(btn) {
  const prop = state.properties.find((p) => p.id === btn.dataset.id);
  if (!prop) return;

  if (btn.classList.contains("js-prop-contact")) {
    openContactVault(prop);
  } else if (btn.classList.contains("js-prop-edit")) {
    openPropertyModal(prop);
  } else if (btn.classList.contains("js-prop-verify")) {
    try {
      const next = await togglePropertyVerified(prop.id, prop.verifiedListing);
      logAudit(
        "PROPERTY_VERIFY",
        "PROPERTY",
        prop.id,
        `${next ? "Verified" : "Unverified"} listing "${prop.title}"`,
        `Location: ${prop.location}`
      );
      showToast(next ? "Marked as Verified Listing" : "Verification badge removed", "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  } else if (btn.classList.contains("js-prop-status")) {
    try {
      const next = await togglePropertyStatus(prop.id, prop.status);
      logAudit(
        "PROPERTY_STATUS",
        "PROPERTY",
        prop.id,
        `Changed "${prop.title}" status to ${next}`,
        `Previous: ${prop.status || "ACTIVE"} → New: ${next}`
      );
      showToast(`Listing is now ${next}`, "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  } else if (btn.classList.contains("js-prop-delete")) {
    if (!window.confirm(`Delete "${prop.title}" and its private contact vault permanently?`)) return;
    try {
      await deletePropertyCompletely(prop.id);
      if (state.previewPropertyId === prop.id) closePropertyPreview();
      logAudit(
        "PROPERTY_DELETE",
        "PROPERTY",
        prop.id,
        `Deleted property "${prop.title}"`,
        `${prop.location} • Owner: ${prop.ownerName || "Unknown"}`
      );
      showToast("Property deleted.", "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  }
}

// ---------------- Rendering: Users & Premium Passes ----------------

function renderUsersTable() {
  const q = ($("user-search").value || "").toLowerCase().trim();
  const roleFilter = $("user-filter-role").value;

  const list = state.users.filter((u) => {
    if (roleFilter !== "ALL" && u.role !== roleFilter) return false;
    if (q) {
      const hay = `${u.displayName || ""} ${u.email || ""} ${u.id}`.toLowerCase();
      if (!hay.includes(q)) return false;
    }
    return true;
  });

  const tbody = $("users-tbody");
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="5"><div class="empty-state">No registered users match your filter.</div></td></tr>`;
    return;
  }

  tbody.innerHTML = list
    .map((u) => {
      const listingCount = state.properties.filter((p) => p.ownerId === u.id).length;
      const roleOptions = USER_ROLES.map(
        (r) => `<option value="${r.value}" ${u.role === r.value ? "selected" : ""}>${r.label}</option>`
      ).join("");
      return `
      <tr>
        <td>
          <div class="cell-title">${escapeHtml(u.displayName || "Unnamed User")}</div>
          <div class="cell-sub">${escapeHtml(u.email || "No email")} • <code>${escapeHtml(u.id.slice(0, 10))}…</code></div>
        </td>
        <td>
          <select class="select js-user-role" data-uid="${escapeHtml(u.id)}" style="width:auto;min-height:34px;padding:4px 10px;font-size:12.5px;">
            ${roleOptions}
          </select>
        </td>
        <td><strong>${listingCount}</strong> listings</td>
        <td class="cell-sub">${escapeHtml(formatTimestamp(u.createdAt))}</td>
        <td>
          <div class="action-row">
            <button class="btn btn-secondary btn-sm js-user-pass" data-uid="${escapeHtml(u.id)}" type="button">⭐ 7-Day Pass</button>
            <button class="btn btn-danger btn-sm js-user-delete" data-uid="${escapeHtml(u.id)}" type="button">🗑 Remove</button>
          </div>
        </td>
      </tr>`;
    })
    .join("");
}

function renderPremiumTable() {
  const tbody = $("premium-tbody");
  if (!state.users.length) {
    tbody.innerHTML = `<tr><td colspan="5"><div class="empty-state">No users found yet.</div></td></tr>`;
    return;
  }

  tbody.innerHTML = state.users
    .map((u) => {
      const m = state.memberships.get(u.id);
      const isLive = Boolean(m?.premiumActive && (!m.expiresAtMillis || m.expiresAtMillis > Date.now()));
      return `
      <tr>
        <td>
          <div class="cell-title">${escapeHtml(u.displayName || "User")}</div>
          <div class="cell-sub">${escapeHtml(u.email || u.id)}</div>
        </td>
        <td>
          <span class="badge ${isLive ? "badge-active" : "badge-neutral"}">${isLive ? "⭐ ACTIVE PASS" : "Free Tier"}</span>
          <div class="cell-sub" style="margin-top:4px;">${escapeHtml(m?.planName || "No active pass")}</div>
        </td>
        <td>${m?.expiresAtMillis ? escapeHtml(formatShortDate(m.expiresAtMillis)) : "—"}</td>
        <td>
          <div class="cell-title">${m ? `₹${m.amountPaid}` : "—"}</div>
          <div class="cell-sub">${escapeHtml(m?.referenceCode || "")}</div>
        </td>
        <td>
          <div class="action-row">
            <button class="btn btn-emerald btn-sm js-user-pass" data-uid="${escapeHtml(u.id)}" type="button">${isLive ? "Extend / Edit" : "+ Grant 7-Day Pass"}</button>
            ${isLive ? `<button class="btn btn-danger btn-sm js-quick-revoke" data-uid="${escapeHtml(u.id)}" type="button">Revoke</button>` : ""}
          </div>
        </td>
      </tr>`;
    })
    .join("");
}

// ---------------- Rendering: Chats ----------------

function renderChatsTable() {
  const q = ($("chat-search").value || "").toLowerCase().trim();
  const list = state.chats.filter((c) => {
    if (!q) return true;
    const hay = `${c.propertyTitle} ${c.buyerName} ${c.ownerName} ${c.lastMessage}`.toLowerCase();
    return hay.includes(q);
  });

  $("chats-count-badge").textContent = `${list.length} Threads`;
  const tbody = $("chats-tbody");
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="5"><div class="empty-state">No buyer-owner chats yet.</div></td></tr>`;
    return;
  }

  tbody.innerHTML = list
    .map(
      (c) => `
    <tr>
      <td><div class="cell-title">${escapeHtml(c.propertyTitle || "Property")}</div></td>
      <td>
        <div class="cell-title">Buyer: ${escapeHtml(c.buyerName || "Buyer")}</div>
        <div class="cell-sub">Owner: ${escapeHtml(c.ownerName || "Owner")}</div>
      </td>
      <td class="cell-sub" style="max-width:280px;">${escapeHtml(c.lastMessage || "—")}</td>
      <td class="cell-sub">${escapeHtml(formatTimestamp(c.updatedAt))}</td>
      <td>
        <div class="action-row">
          <button class="btn btn-primary btn-sm js-chat-open" data-id="${escapeHtml(c.id)}" type="button">💬 Inspect</button>
          <button class="btn btn-danger btn-sm js-chat-delete" data-id="${escapeHtml(c.id)}" type="button">🗑</button>
        </div>
      </td>
    </tr>`
    )
    .join("");
}

// ---------------- Modal Openers & Handlers ----------------

let modalPickerMap = null;
let modalPickerMarker = null;

function syncPropertyFormTypeFields() {
  const type = $("pf-type").value;
  const priceLabel = $("pf-price-label");
  if (priceLabel) {
    priceLabel.textContent =
      type === "RENT"
        ? "Monthly Rent (INR) *"
        : type === "HEAVY_DEPOSIT"
          ? "Heavy Deposit (INR) *"
          : "Price (INR) *";
  }
  const showFloor = type === "RENT" || type === "HEAVY_DEPOSIT";
  $("pf-floor-field")?.classList.toggle("hidden", !showFloor);
  $("pf-deposit-field")?.classList.toggle("hidden", type !== "RENT");
  $("pf-monthly-rent-field")?.classList.toggle("hidden", type !== "HEAVY_DEPOSIT");
}

function setModalMapPin(lat, lng, panMap = false) {
  const safeLat = Number(lat);
  const safeLng = Number(lng);
  if (Number.isNaN(safeLat) || Number.isNaN(safeLng)) return;
  $("pf-lat").value = safeLat.toFixed(5);
  $("pf-lng").value = safeLng.toFixed(5);

  if (modalPickerMap && window.L) {
    if (!modalPickerMarker) {
      modalPickerMarker = window.L.marker([safeLat, safeLng], { draggable: true }).addTo(modalPickerMap);
      modalPickerMarker.on("dragend", () => {
        const pos = modalPickerMarker.getLatLng();
        $("pf-lat").value = pos.lat.toFixed(5);
        $("pf-lng").value = pos.lng.toFixed(5);
      });
    } else {
      modalPickerMarker.setLatLng([safeLat, safeLng]);
    }
    if (panMap) {
      modalPickerMap.setView([safeLat, safeLng], 14);
    }
  }
}

function toggleModalMapPicker(forceVisible = null) {
  const wrap = $("pf-map-picker-wrap");
  if (!wrap) return;
  const shouldShow = forceVisible !== null ? forceVisible : wrap.classList.contains("hidden");
  wrap.classList.toggle("hidden", !shouldShow);
  if (!shouldShow || !window.L) return;

  const latVal = Number($("pf-lat").value);
  const lngVal = Number($("pf-lng").value);
  const hasCoords = $("pf-lat").value !== "" && $("pf-lng").value !== "" && !Number.isNaN(latVal) && !Number.isNaN(lngVal);
  const fallback = resolveIndianCoordinates($("pf-location").value || "Mumbai");
  const startLat = hasCoords ? latVal : fallback.lat;
  const startLng = hasCoords ? lngVal : fallback.lng;

  if (!modalPickerMap) {
    modalPickerMap = window.L.map("pf-map-canvas", { zoomControl: true }).setView([startLat, startLng], hasCoords ? 14 : 11);
    window.L.tileLayer("https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png", {
      attribution: "&copy; OpenStreetMap &copy; CARTO",
      maxZoom: 18
    }).addTo(modalPickerMap);
    modalPickerMap.on("click", (evt) => {
      setModalMapPin(evt.latlng.lat, evt.latlng.lng, false);
    });
  } else {
    modalPickerMap.setView([startLat, startLng], hasCoords ? 14 : 11);
  }

  if (hasCoords) {
    setModalMapPin(startLat, startLng, false);
  }
  setTimeout(() => modalPickerMap?.invalidateSize(), 120);
}

async function openPropertyModal(prop = null) {
  $("property-form").reset();
  $("pf-map-picker-wrap")?.classList.add("hidden");
  $("pf-id").value = prop?.id || "";
  $("pf-owner-id").value = prop?.ownerId || state.adminUser?.uid || "";
  $("modal-prop-title").textContent = prop ? "Edit Property Listing" : "Add New Property Listing";

  if (prop) {
    $("pf-title").value = prop.title || "";
    $("pf-type").value = prop.listingType || "SALE";
    $("pf-category").value = prop.propertyCategory || "APARTMENT";
    $("pf-price").value = prop.price || "";
    $("pf-location").value = prop.location || "";
    $("pf-floor").value = prop.floor || "";
    $("pf-deposit").value = prop.depositAmount > 0 ? prop.depositAmount : "";
    $("pf-monthly-rent").value = prop.monthlyRent > 0 ? prop.monthlyRent : "";
    $("pf-bedrooms").value = prop.bedrooms ?? 2;
    $("pf-area").value = prop.areaSqFt ?? 0;
    $("pf-building-age").value = prop.buildingAge || "";
    $("pf-lift").value = prop.hasLift === true ? "true" : "false";
    $("pf-lat").value = prop.latitude ?? "";
    $("pf-lng").value = prop.longitude ?? "";
    $("pf-owner-name").value = prop.ownerName || "";
    $("pf-status").value = prop.status || "ACTIVE";
    $("pf-photo").value = Array.isArray(prop.photoKeys) ? prop.photoKeys[0] || "" : "";
    $("pf-description").value = prop.description || "";
    $("pf-verified").checked = Boolean(prop.verifiedListing);

    const contact = await getPropertyContact(prop.id);
    $("pf-phone").value = contact?.contactPhone || "";
    $("pf-email").value = contact?.contactEmail || "";
    $("pf-whatsapp").checked = contact ? Boolean(contact.whatsappEnabled) : true;
  } else {
    $("pf-owner-name").value = state.adminUser?.displayName || "Property Broker Admin";
    $("pf-email").value = state.adminUser?.email || ADMIN_EMAIL;
    $("pf-lift").value = "false";
    $("pf-verified").checked = true;
    $("pf-whatsapp").checked = true;
  }
  syncPropertyFormTypeFields();
  updatePricePreview();
  openModal("modal-property");
}

function updatePricePreview() {
  const val = Number($("pf-price").value);
  const type = $("pf-type").value;
  $("pf-price-preview").textContent = val > 0 ? `(${formatINR(val, type)})` : "";
}

async function openContactVault(prop) {
  $("contact-vault-body").innerHTML = `<div class="empty-state">Decrypting private contact vault...</div>`;
  openModal("modal-contact");
  try {
    const c = await getPropertyContact(prop.id);
    if (!c) {
      $("contact-vault-body").innerHTML = `<div class="empty-state">No private contact document found for this listing. Use <strong>Edit</strong> to add owner phone/email.</div>`;
      return;
    }
    const cleanPhone = (c.contactPhone || "").replace(/[^0-9+]/g, "");
    const waDigits = cleanPhone.replace(/^\+/, "");
    $("contact-vault-body").innerHTML = `
      <div style="display:flex;flex-direction:column;gap:12px;">
        <div><div class="cell-sub">Property</div><div class="cell-title">${escapeHtml(prop.title)}</div></div>
        <div><div class="cell-sub">Owner / Broker</div><div class="cell-title">${escapeHtml(prop.ownerName || "Owner")}</div></div>
        <div><div class="cell-sub">Phone Number</div><div class="cell-title" style="font-size:18px;">${escapeHtml(c.contactPhone)}</div></div>
        <div><div class="cell-sub">Email</div><div class="cell-title">${escapeHtml(c.contactEmail || "Not provided")}</div></div>
        <div><span class="badge ${c.whatsappEnabled ? "badge-active" : "badge-neutral"}">${c.whatsappEnabled ? "✓ WhatsApp Enabled" : "WhatsApp Disabled"}</span></div>
        <div class="action-row" style="margin-top:6px;">
          <a class="btn btn-primary btn-sm" href="tel:${escapeHtml(cleanPhone)}">📞 Call Owner</a>
          ${c.whatsappEnabled && waDigits ? `<a class="btn btn-emerald btn-sm" target="_blank" rel="noopener" href="https://wa.me/${escapeHtml(waDigits)}">💬 WhatsApp</a>` : ""}
          <button id="btn-copy-phone" class="btn btn-secondary btn-sm" type="button">📋 Copy Phone</button>
        </div>
      </div>`;
    $("btn-copy-phone")?.addEventListener("click", async () => {
      await navigator.clipboard.writeText(c.contactPhone || "");
      showToast("Phone number copied!", "success");
    });
  } catch (err) {
    $("contact-vault-body").innerHTML = `<div class="empty-state">Failed to read contact vault: ${escapeHtml(err.message)}</div>`;
  }
}

function openPassModal(uid) {
  const user = state.users.find((u) => u.id === uid);
  if (!user) return;
  const mem = state.memberships.get(uid);
  $("pass-uid").value = uid;
  $("pass-user-name").textContent = user.displayName || "User";
  $("pass-user-email").textContent = user.email || uid;
  $("pass-plan").value = mem?.planName || "7-Day Premium Pass";
  $("pass-days").value = String(PREMIUM_PASS_DAYS_DEFAULT);
  $("pass-amount").value = String(mem?.amountPaid ?? PREMIUM_PASS_PRICE_DEFAULT);
  $("pass-ref").value = mem?.referenceCode || `ADMIN-${Date.now().toString().slice(-6)}`;
  $("pass-active").checked = true;
  openModal("modal-pass");
}

function openChatInspector(chatId) {
  const chat = state.chats.find((c) => c.id === chatId);
  if (!chat) return;
  state.activeChatId = chatId;
  $("chat-modal-title").textContent = chat.propertyTitle || "Chat Conversation";
  $("chat-modal-sub").textContent = `Buyer: ${chat.buyerName || "Buyer"} ↔ Owner: ${chat.ownerName || "Owner"}`;
  $("chat-messages-list").innerHTML = `<div class="empty-state">Loading live messages...</div>`;
  openModal("modal-chat");

  if (state.chatMsgUnsub) state.chatMsgUnsub();
  state.chatMsgUnsub = subscribeChatMessages(
    chatId,
    (msgs) => {
      const box = $("chat-messages-list");
      if (!msgs.length) {
        box.innerHTML = `<div class="empty-state">No messages in this thread yet.</div>`;
        return;
      }
      box.innerHTML = msgs
        .map(
          (m) => `
        <div class="chat-bubble ${m.senderId === chat.ownerId ? "owner" : ""}">
          <div class="chat-bubble-meta">
            <span>${escapeHtml(m.senderName || "User")}</span>
            <span>${escapeHtml(formatTimestamp(m.createdAt))}</span>
          </div>
          <div class="chat-bubble-text">${escapeHtml(m.text)}</div>
        </div>`
        )
        .join("");
      box.scrollTop = box.scrollHeight;
    },
    (err) => showToast(`Failed to load messages: ${err.message}`, "error")
  );
}

// ---------------- Event Bindings ----------------

function bindEvents() {
  $("btn-theme-toggle").addEventListener("click", toggleTheme);
  $("btn-mobile-menu").addEventListener("click", () => {
    $("sidebar").classList.toggle("open");
    $("sidebar-backdrop").classList.toggle("open");
  });
  $("sidebar-backdrop").addEventListener("click", closeMobileSidebar);

  document.querySelectorAll(".nav-item").forEach((btn) => {
    btn.addEventListener("click", () => switchView(btn.dataset.view));
  });
  document.querySelectorAll("[data-jump]").forEach((btn) => {
    btn.addEventListener("click", () => switchView(btn.dataset.jump));
  });
  document.querySelectorAll(".js-close-modal").forEach((btn) => {
    btn.addEventListener("click", () => closeModal(btn.dataset.modal));
  });

  // Google Sign-In & Sign-Out
  $("btn-google-signin").addEventListener("click", async () => {
    const errEl = $("login-error");
    errEl.classList.add("hidden");
    const btn = $("btn-google-signin");
    btn.disabled = true;
    try {
      await signInAdminWithGoogle();
    } catch (err) {
      let msg = err?.message || "Sign-in failed.";
      if (err?.code === "auth/unauthorized-domain") {
        msg = `Domain "${window.location.hostname}" is not authorized yet. Open Firebase Console → Authentication → Settings → Authorized domains and add "${window.location.hostname}".`;
      }
      errEl.textContent = msg;
      errEl.classList.remove("hidden");
    } finally {
      btn.disabled = false;
    }
  });

  $("btn-signout").addEventListener("click", async () => {
    clearSubscriptions();
    await signOutAdmin();
    showToast("Signed out safely.");
  });

  // Quick Add Property buttons
  $("btn-quick-add").addEventListener("click", () => openPropertyModal(null));
  $("btn-add-prop-2").addEventListener("click", () => openPropertyModal(null));
  $("pf-price").addEventListener("input", updatePricePreview);
  $("pf-type").addEventListener("change", () => {
    syncPropertyFormTypeFields();
    updatePricePreview();
  });
  $("btn-auto-coords").addEventListener("click", () => {
    const { lat, lng } = resolveIndianCoordinates($("pf-location").value);
    setModalMapPin(lat, lng, true);
    showToast(`Coordinates set (${lat}, ${lng})`, "success");
  });
  $("btn-toggle-modal-map")?.addEventListener("click", () => toggleModalMapPicker());
  $("btn-clear-modal-coords")?.addEventListener("click", () => {
    $("pf-lat").value = "";
    $("pf-lng").value = "";
    if (modalPickerMarker && modalPickerMap) {
      modalPickerMap.removeLayer(modalPickerMarker);
      modalPickerMarker = null;
    }
    showToast("Map pin cleared.");
  });

  // Save Property Form
  $("property-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const btn = $("btn-save-property");
    btn.disabled = true;
    try {
      const existingId = $("pf-id").value || null;
      const title = $("pf-title").value.trim();
      const location = $("pf-location").value.trim();
      const price = $("pf-price").value;
      const listingType = $("pf-type").value;
      const savedId = await savePropertyWithContact({
        existingId,
        existingOwnerId: $("pf-owner-id").value || null,
        adminUid: state.adminUser?.uid,
        form: {
          title,
          listingType,
          propertyCategory: $("pf-category").value,
          price,
          location,
          floor: $("pf-floor").value,
          depositAmount: $("pf-deposit").value,
          monthlyRent: $("pf-monthly-rent").value,
          bedrooms: $("pf-bedrooms").value,
          areaSqFt: $("pf-area").value,
          buildingAge: $("pf-building-age").value,
          hasLift: $("pf-lift").value === "true",
          latitude: $("pf-lat").value,
          longitude: $("pf-lng").value,
          ownerName: $("pf-owner-name").value,
          status: $("pf-status").value,
          contactPhone: $("pf-phone").value,
          contactEmail: $("pf-email").value,
          photo: $("pf-photo").value,
          description: $("pf-description").value,
          whatsappEnabled: $("pf-whatsapp").checked,
          verifiedListing: $("pf-verified").checked
        }
      });
      logAudit(
        existingId ? "PROPERTY_EDIT" : "PROPERTY_CREATE",
        "PROPERTY",
        savedId,
        existingId ? `Edited property "${title}"` : `Added property "${title}"`,
        `${getListingTypeLabel(listingType)} • ${formatINR(price, listingType)} • ${location}`
      );
      closeModal("modal-property");
      showToast("Property saved live to Firebase!", "success");
    } catch (err) {
      showToast(`Save failed: ${err.message}`, "error");
    } finally {
      btn.disabled = false;
    }
  });

  // Property filters & table delegation
  ["prop-search", "prop-filter-type", "prop-filter-category", "prop-filter-status"].forEach((id) => {
    $(id).addEventListener("input", renderPropertiesTable);
    $(id).addEventListener("change", renderPropertiesTable);
  });

  $("prop-live-sync")?.addEventListener("change", (e) => {
    state.liveSyncEnabled = Boolean(e.target.checked);
    localStorage.setItem(LIVE_SYNC_STORAGE_KEY, state.liveSyncEnabled ? "1" : "0");
    if (state.liveSyncEnabled) {
      startPropertyLiveSync({ immediatePoll: true });
      showToast(`Live Sync enabled — polling Firestore every ${LIVE_SYNC_INTERVAL_SEC}s.`, "success");
    } else {
      stopPropertyLiveSync();
      showToast("Live Sync paused.");
    }
  });

  $("prop-price-chips").addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    state.priceBand = chip.dataset.price;
    $("prop-price-chips").querySelectorAll(".chip").forEach((c) => c.classList.toggle("active", c === chip));
    renderPropertiesTable();
  });

  $("properties-tbody").addEventListener("click", async (e) => {
    const btn = e.target.closest("button");
    if (btn) {
      await handlePropertyActionClick(btn);
      return;
    }
    const row = e.target.closest("tr[data-id]");
    if (row?.dataset.id) {
      openPropertyPreview(row.dataset.id);
    }
  });

  // Side-panel Property Preview interactions
  $("btn-close-preview")?.addEventListener("click", closePropertyPreview);
  $("property-preview-backdrop")?.addEventListener("click", closePropertyPreview);
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && state.previewPropertyId) {
      closePropertyPreview();
    }
  });

  $("property-preview-body")?.addEventListener("click", async (e) => {
    const copyBtn = e.target.closest(".js-preview-copy-phone");
    if (copyBtn) {
      try {
        await navigator.clipboard.writeText(copyBtn.dataset.phone || "");
        showToast("Phone number copied!", "success");
      } catch {
        showToast("Unable to copy phone number.", "error");
      }
      return;
    }

    const prop = state.properties.find((p) => p.id === state.previewPropertyId);
    if (!prop) return;
    const total = getPropertyMediaKeys(prop).length;
    if (total <= 1) return;

    if (e.target.closest(".js-preview-prev")) {
      state.previewImageIndex = (state.previewImageIndex - 1 + total) % total;
      renderPropertyPreview({ reloadContact: false });
    } else if (e.target.closest(".js-preview-next")) {
      state.previewImageIndex = (state.previewImageIndex + 1) % total;
      renderPropertyPreview({ reloadContact: false });
    } else {
      const thumb = e.target.closest(".js-preview-thumb");
      if (thumb) {
        state.previewImageIndex = Number(thumb.dataset.index) || 0;
        renderPropertyPreview({ reloadContact: false });
      }
    }
  });

  $("property-preview-footer")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button");
    if (!btn) return;
    await handlePropertyActionClick(btn);
  });

  // Map filters
  $("map-filter-chips").addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    $("map-filter-chips").querySelectorAll(".chip").forEach((c) => c.classList.toggle("active", c === chip));
    mapManager.setFilter(chip.dataset.mapType);
  });
  $("btn-map-fit").addEventListener("click", () => mapManager.fitAll());

  // Users filters & actions
  $("user-search").addEventListener("input", renderUsersTable);
  $("user-filter-role").addEventListener("change", renderUsersTable);
  $("users-tbody").addEventListener("change", async (e) => {
    if (!e.target.classList.contains("js-user-role")) return;
    const uid = e.target.dataset.uid;
    const targetUser = state.users.find((u) => u.id === uid);
    const prevRole = targetUser?.role || "BUYER";
    const newRole = e.target.value;
    try {
      await updateUserRole(uid, newRole);
      logAudit(
        "USER_ROLE",
        "USER",
        uid,
        `Updated role for ${targetUser?.displayName || uid} to ${newRole}`,
        `${targetUser?.email || ""} • ${prevRole} → ${newRole}`
      );
      showToast("User role updated!", "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  });
  $("users-tbody").addEventListener("click", async (e) => {
    const btn = e.target.closest("button");
    if (!btn) return;
    const uid = btn.dataset.uid;
    const targetUser = state.users.find((u) => u.id === uid);
    if (btn.classList.contains("js-user-pass")) {
      openPassModal(uid);
    } else if (btn.classList.contains("js-user-delete")) {
      if (!window.confirm("Remove this user's Firestore profile & pass record?")) return;
      try {
        await deleteUserProfile(uid);
        logAudit(
          "USER_DELETE",
          "USER",
          uid,
          `Removed user profile "${targetUser?.displayName || uid}"`,
          targetUser?.email || ""
        );
        showToast("User profile removed.", "success");
      } catch (err) {
        showToast(err.message, "error");
      }
    }
  });

  // Premium table & form
  $("btn-refresh-passes").addEventListener("click", async () => {
    await refreshMemberships();
    showToast("Premium pass statuses refreshed.", "success");
  });
  $("premium-tbody").addEventListener("click", async (e) => {
    const btn = e.target.closest("button");
    if (!btn) return;
    const uid = btn.dataset.uid;
    const targetUser = state.users.find((u) => u.id === uid);
    if (btn.classList.contains("js-user-pass")) {
      openPassModal(uid);
    } else if (btn.classList.contains("js-quick-revoke")) {
      if (!window.confirm("Revoke this user's 7-Day Premium Pass immediately?")) return;
      try {
        await revokeUserPremiumPass(uid);
        logAudit(
          "PASS_REVOKE",
          "MEMBERSHIP",
          uid,
          `Revoked 7-Day Pass for ${targetUser?.displayName || uid}`,
          targetUser?.email || ""
        );
        await refreshMemberships();
        showToast("Premium pass revoked.", "success");
      } catch (err) {
        showToast(err.message, "error");
      }
    }
  });
  $("pass-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const uid = $("pass-uid").value;
    const targetUser = state.users.find((u) => u.id === uid);
    const days = $("pass-days").value;
    const amountPaid = $("pass-amount").value;
    const referenceCode = $("pass-ref").value;
    const premiumActive = $("pass-active").checked;
    try {
      await saveUserPremiumPass({
        userId: uid,
        planName: $("pass-plan").value,
        days,
        amountPaid,
        referenceCode,
        premiumActive
      });
      logAudit(
        premiumActive ? "PASS_GRANT" : "PASS_REVOKE",
        "MEMBERSHIP",
        uid,
        `${premiumActive ? "Granted" : "Deactivated"} ${days}-Day Pass for ${targetUser?.displayName || uid}`,
        `₹${amountPaid} • Ref: ${referenceCode}`
      );
      closeModal("modal-pass");
      await refreshMemberships();
      showToast("7-Day Premium Pass updated live!", "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  });
  $("btn-revoke-pass").addEventListener("click", async () => {
    const uid = $("pass-uid").value;
    const targetUser = state.users.find((u) => u.id === uid);
    try {
      await revokeUserPremiumPass(uid);
      logAudit(
        "PASS_REVOKE",
        "MEMBERSHIP",
        uid,
        `Revoked 7-Day Pass for ${targetUser?.displayName || uid}`,
        targetUser?.email || ""
      );
      closeModal("modal-pass");
      await refreshMemberships();
      showToast("Premium pass revoked.", "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  // Chats
  $("chat-search").addEventListener("input", renderChatsTable);
  $("chats-tbody").addEventListener("click", async (e) => {
    const btn = e.target.closest("button");
    if (!btn) return;
    const id = btn.dataset.id;
    const chat = state.chats.find((c) => c.id === id);
    if (btn.classList.contains("js-chat-open")) {
      openChatInspector(id);
    } else if (btn.classList.contains("js-chat-delete")) {
      if (!window.confirm("Delete this chat thread and all its messages?")) return;
      try {
        await deleteChatThread(id);
        logAudit(
          "CHAT_DELETE",
          "CHAT",
          id,
          `Deleted chat thread on "${chat?.propertyTitle || id}"`,
          `Buyer: ${chat?.buyerName || "?"} ↔ Owner: ${chat?.ownerName || "?"}`
        );
        showToast("Chat thread deleted.", "success");
      } catch (err) {
        showToast(err.message, "error");
      }
    }
  });
  $("btn-delete-chat-modal").addEventListener("click", async () => {
    if (!state.activeChatId) return;
    const chat = state.chats.find((c) => c.id === state.activeChatId);
    if (!window.confirm("Delete this chat thread and all messages permanently?")) return;
    try {
      const deletedId = state.activeChatId;
      await deleteChatThread(deletedId);
      logAudit(
        "CHAT_DELETE",
        "CHAT",
        deletedId,
        `Deleted chat thread on "${chat?.propertyTitle || deletedId}"`,
        `Buyer: ${chat?.buyerName || "?"} ↔ Owner: ${chat?.ownerName || "?"}`
      );
      closeModal("modal-chat");
      showToast("Chat thread deleted.", "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  // App Settings Form & Audit Log Sub-Tabs
  $("settings-subtabs")?.addEventListener("click", (e) => {
    const btn = e.target.closest(".subtab-btn");
    if (!btn) return;
    switchSettingsTab(btn.dataset.settingsTab || "config");
  });

  $("audit-search")?.addEventListener("input", renderAuditLogTable);
  $("audit-filter-entity")?.addEventListener("change", renderAuditLogTable);

  $("audit-autoclean-toggle")?.addEventListener("change", async (e) => {
    const enabled = Boolean(e.target.checked);
    state.auditAutoCleanEnabled = enabled;
    localStorage.setItem(AUDIT_AUTOCLEAN_STORAGE_KEY, enabled ? "1" : "0");

    try {
      await saveAppSettings({ auditAutoClean30Days: enabled }, state.adminUser?.email);
    } catch (err) {
      console.warn("Could not persist auditAutoClean30Days to Firestore:", err?.message || err);
    }

    if (enabled) {
      showToast(`Audit Log Auto-Clean enabled — entries older than ${AUDIT_RETENTION_DAYS} days will be pruned automatically.`, "success");
      startAuditAutoCleanScheduler({ runNow: true });
    } else {
      stopAuditAutoCleanScheduler();
      showToast("Audit Log Auto-Clean turned off.");
    }
  });

  $("btn-prune-audit-now")?.addEventListener("click", async () => {
    if (state.auditAutoCleanInFlight) return;
    if (!window.confirm(`Delete all audit log entries older than ${AUDIT_RETENTION_DAYS} days from Firestore?`)) return;
    await runAuditAutoClean({ manual: true });
  });

  $("btn-export-audit")?.addEventListener("click", () => {
    downloadCsv(
      `property-broker-audit-log-${new Date().toISOString().slice(0, 10)}.csv`,
      ["Timestamp", "Action", "Entity_Type", "Entity_ID", "Summary", "Details", "Admin_Email"],
      getFilteredAuditLogs().map((e) => [
        formatTimestamp(e.createdAt), e.action, e.entityType, e.entityId,
        e.summary, e.details || "", e.adminEmail || ADMIN_EMAIL
      ])
    );
    showToast("Audit Log CSV downloaded.", "success");
  });

  $("btn-clear-audit")?.addEventListener("click", async () => {
    if (!state.auditLogs.length) return;
    if (!window.confirm(`Permanently clear all ${state.auditLogs.length} audit log entries?`)) return;
    try {
      await clearAuditLogs(state.auditLogs.map((l) => l.id));
      showToast("Audit log cleared.", "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  $("settings-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const latestVersion = $("cfg-latest-version").value.trim() || "4.0";
    const maintenanceMode = $("cfg-maintenance").checked;
    try {
      await saveAppSettings(
        {
          latestVersion,
          minSupportedVersion: $("cfg-min-version").value.trim() || "2.0",
          premiumPriceInr: Number($("cfg-premium-price").value) || 999,
          premiumDurationDays: Number($("cfg-premium-days").value) || 7,
          premiumActivationDate: $("cfg-launch-date").value || "2027-01-01",
          supportEmail: $("cfg-support-email").value.trim(),
          supportWhatsapp: $("cfg-support-whatsapp").value.trim(),
          playStoreUrl: $("cfg-play-url").value.trim(),
          announcementBanner: $("cfg-announcement").value.trim(),
          maintenanceMode
        },
        state.adminUser?.email
      );
      logAudit(
        "SETTINGS_UPDATE",
        "SETTINGS",
        "settings",
        `Updated live app config (v${latestVersion})`,
        `Maintenance: ${maintenanceMode ? "ON" : "OFF"} • Pass: ₹${$("cfg-premium-price").value || 999}`
      );
      showToast("Live app settings saved to Firestore!", "success");
    } catch (err) {
      showToast(`Failed to save settings: ${err.message}`, "error");
    }
  });

  // CSV Exports
  $("btn-export-props").addEventListener("click", () => {
    downloadCsv(
      `property-broker-listings-${new Date().toISOString().slice(0, 10)}.csv`,
      ["ID", "Title", "Type", "Category", "Price_INR", "Floor", "Deposit_INR", "Monthly_Rent_INR", "Building_Age", "Lift", "Location", "BHK", "Area_SqFt", "Owner", "Status", "Verified"],
      getFilteredProperties().map((p) => [
        p.id, p.title, p.listingType, p.propertyCategory, p.price,
        p.floor || "", p.depositAmount || 0, p.monthlyRent || 0,
        p.buildingAge || "", p.hasLift === true ? "Yes" : p.hasLift === false ? "No" : "",
        p.location, p.bedrooms, p.areaSqFt, p.ownerName, p.status, p.verifiedListing ? "Yes" : "No"
      ])
    );
    showToast("Properties CSV downloaded.", "success");
  });

  $("btn-export-users").addEventListener("click", () => {
    downloadCsv(
      `property-broker-users-${new Date().toISOString().slice(0, 10)}.csv`,
      ["UID", "Name", "Email", "Role", "Listings"],
      state.users.map((u) => [
        u.id, u.displayName, u.email, u.role,
        state.properties.filter((p) => p.ownerId === u.id).length
      ])
    );
    showToast("Users CSV downloaded.", "success");
  });

  // Analytics range chips & CSV export
  $("analytics-range-chips")?.addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    $("analytics-range-chips").querySelectorAll(".chip").forEach((c) => c.classList.toggle("active", c === chip));
    analyticsManager.setRange(Number(chip.dataset.days) || 30);
  });

  $("btn-export-analytics")?.addEventListener("click", () => {
    analyticsManager.render({
      users: state.users,
      properties: state.properties,
      chats: state.chats
    });
    downloadCsv(
      `property-broker-analytics-${new Date().toISOString().slice(0, 10)}.csv`,
      ["Date", "New_Signups", "Cumulative_Users", "Daily_Active_Users", "DAU_3Day_Avg"],
      analyticsManager.getExportRows()
    );
    showToast("Analytics CSV downloaded.", "success");
  });
}

// ---------------- Auth Bootstrap ----------------

function initApp() {
  initTheme();
  initPropertyLiveSync();
  initAuditAutoClean();
  initTableHeaderTooltips();
  bindEvents();
  updateCountdown();
  setInterval(updateCountdown, 60000);

  checkRedirectSignIn().catch((err) => {
    if (err?.message) {
      $("login-error").textContent = err.message;
      $("login-error").classList.remove("hidden");
    }
  });

  subscribeAuth(async (user) => {
    if (!user) {
      state.adminUser = null;
      clearSubscriptions();
      $("app-shell").classList.add("hidden");
      $("login-gate").classList.remove("hidden");
      return;
    }

    if (!isAuthorizedAdmin(user)) {
      clearSubscriptions();
      await signOutAdmin();
      $("login-error").textContent =
        `Access denied for "${user.email || "unknown account"}". Please sign in with the verified admin Google account (${ADMIN_EMAIL}).`;
      $("login-error").classList.remove("hidden");
      $("app-shell").classList.add("hidden");
      $("login-gate").classList.remove("hidden");
      return;
    }

    state.adminUser = user;
    $("admin-name").textContent = user.displayName || "Admin";
    $("admin-email").textContent = user.email || ADMIN_EMAIL;
    $("admin-avatar").textContent = (user.displayName || "A").charAt(0).toUpperCase();

    $("login-gate").classList.add("hidden");
    $("app-shell").classList.remove("hidden");
    startLiveSubscriptions();
  });
}

initApp();
