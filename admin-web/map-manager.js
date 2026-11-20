/**
 * Free Interactive Property Map Manager using Leaflet + OpenStreetMap (₹0 cost, no API key needed)
 */
import { escapeHtml, formatINR, getListingTypeLabel, resolveIndianCoordinates } from "./firebase-config.js";

const TYPE_COLORS = {
  SALE: "#1E3A8A",
  RENT: "#059669",
  HEAVY_DEPOSIT: "#D97706"
};

export class PropertyMapManager {
  constructor(containerId, { onEditProperty, onViewContact }) {
    this.containerId = containerId;
    this.onEditProperty = onEditProperty;
    this.onViewContact = onViewContact;
    this.map = null;
    this.markersLayer = null;
    this.currentFilter = "ALL";
    this.lastProperties = [];
  }

  ensureInitialized() {
    if (this.map) {
      setTimeout(() => this.map.invalidateSize(), 120);
      return;
    }
    const el = document.getElementById(this.containerId);
    if (!el || typeof window.L === "undefined") return;

    this.map = window.L.map(this.containerId, {
      center: [19.0760, 72.8777],
      zoom: 11,
      zoomControl: true
    });

    window.L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(this.map);

    this.markersLayer = window.L.layerGroup().addTo(this.map);
  }

  setFilter(filterType) {
    this.currentFilter = filterType;
    this.render(this.lastProperties);
  }

  render(properties) {
    this.lastProperties = properties || [];
    this.ensureInitialized();
    if (!this.map || !this.markersLayer) return;

    this.markersLayer.clearLayers();
    const filtered = this.lastProperties.filter((p) => {
      if (this.currentFilter === "ALL") return true;
      return p.listingType === this.currentFilter;
    });

    const bounds = [];

    filtered.forEach((prop) => {
      const hasCoords =
        typeof prop.latitude === "number" &&
        typeof prop.longitude === "number" &&
        !Number.isNaN(prop.latitude) &&
        !Number.isNaN(prop.longitude) &&
        (prop.latitude !== 0 || prop.longitude !== 0);

      const coords = hasCoords
        ? { lat: prop.latitude, lng: prop.longitude }
        : resolveIndianCoordinates(prop.location);

      bounds.push([coords.lat, coords.lng]);

      const color = TYPE_COLORS[prop.listingType] || "#1E3A8A";
      const priceTag = formatINR(prop.price, prop.listingType);
      const isClosed = prop.status === "CLOSED";

      const icon = window.L.divIcon({
        className: "pb-map-pin-wrapper",
        html: `<div class="pb-map-pin ${isClosed ? "pb-map-pin-closed" : ""}" style="background:${color}">
          <span>${escapeHtml(priceTag)}</span>
        </div>`,
        iconSize: [110, 32],
        iconAnchor: [55, 16]
      });

      const marker = window.L.marker([coords.lat, coords.lng], { icon });

      const popupEl = document.createElement("div");
      popupEl.className = "pb-map-popup";
      popupEl.innerHTML = `
        <div class="pb-map-popup-head">
          <span class="badge badge-${prop.listingType?.toLowerCase() || "sale"}">${escapeHtml(getListingTypeLabel(prop.listingType))}</span>
          <span class="badge ${isClosed ? "badge-closed" : "badge-active"}">${escapeHtml(prop.status || "ACTIVE")}</span>
        </div>
        <h4>${escapeHtml(prop.title)}</h4>
        <p class="pb-map-popup-price">${escapeHtml(priceTag)}</p>
        <p class="pb-map-popup-loc">${escapeHtml(prop.location)} • ${prop.areaSqFt || 0} sq.ft</p>
        <p class="pb-map-popup-owner">Owner: ${escapeHtml(prop.ownerName || "Unknown")}</p>
        <div class="pb-map-popup-actions">
          <button type="button" class="btn btn-sm btn-secondary js-map-contact">Contact</button>
          <button type="button" class="btn btn-sm btn-primary js-map-edit">Edit</button>
        </div>
      `;

      popupEl.querySelector(".js-map-contact")?.addEventListener("click", () => {
        this.onViewContact?.(prop);
      });
      popupEl.querySelector(".js-map-edit")?.addEventListener("click", () => {
        this.onEditProperty?.(prop);
      });

      marker.bindPopup(popupEl, { maxWidth: 270 });
      marker.addTo(this.markersLayer);
    });

    if (bounds.length > 0) {
      this.map.fitBounds(bounds, { padding: [45, 45], maxZoom: 14 });
    }
    setTimeout(() => this.map.invalidateSize(), 150);
  }

  fitAll() {
    this.render(this.lastProperties);
  }
}
