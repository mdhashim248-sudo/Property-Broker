/**
 * Property Broker Admin — D3.js Analytics Manager
 * Visualizes live User Growth Trends & Daily Active User (DAU) counts directly from Firestore data.
 */

const ROLE_COLORS = {
  BUYER: "#2563eb",
  OWNER: "#059669",
  RENTER: "#d97706",
  BOTH: "#7c3aed"
};

const ROLE_LABELS = {
  BUYER: "Buyer",
  OWNER: "Owner / Broker",
  RENTER: "Renter",
  BOTH: "Buyer & Owner"
};

function toDateSafe(ts) {
  if (!ts) return null;
  if (typeof ts.toDate === "function") return ts.toDate();
  if (typeof ts === "number") return new Date(ts);
  if (typeof ts === "object" && typeof ts.seconds === "number") {
    return new Date(ts.seconds * 1000);
  }
  const d = new Date(ts);
  return Number.isNaN(d.getTime()) ? null : d;
}

function toDayKey(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function formatAxisLabel(date) {
  return date.toLocaleDateString("en-IN", { day: "2-digit", month: "short" });
}

function getCssVar(name, fallback) {
  const val = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return val || fallback;
}

export class AnalyticsManager {
  constructor() {
    this.rangeDays = 30;
    this.lastPayload = { users: [], properties: [], chats: [] };
    this.lastSeries = [];
    this.tooltip = null;
    this.resizeTimer = null;

    window.addEventListener("resize", () => {
      clearTimeout(this.resizeTimer);
      this.resizeTimer = setTimeout(() => {
        const view = document.getElementById("view-analytics");
        if (view && !view.classList.contains("hidden")) {
          this.render(this.lastPayload);
        }
      }, 150);
    });
  }

  ensureTooltip() {
    if (this.tooltip) return this.tooltip;
    let el = document.getElementById("d3-analytics-tooltip");
    if (!el) {
      el = document.createElement("div");
      el.id = "d3-analytics-tooltip";
      el.className = "d3-tooltip hidden";
      document.body.appendChild(el);
    }
    this.tooltip = el;
    return el;
  }

  showTooltip(event, html) {
    const tip = this.ensureTooltip();
    tip.innerHTML = html;
    tip.classList.remove("hidden");
    const pad = 14;
    const x = Math.min(window.innerWidth - tip.offsetWidth - pad, event.clientX + pad);
    const y = Math.max(pad, event.clientY - tip.offsetHeight - pad);
    tip.style.left = `${Math.max(pad, x)}px`;
    tip.style.top = `${y}px`;
  }

  hideTooltip() {
    if (this.tooltip) this.tooltip.classList.add("hidden");
  }

  setRange(days) {
    this.rangeDays = Number(days) || 30;
    this.render(this.lastPayload);
  }

  computeSeries({ users = [], properties = [], chats = [] }) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const days = [];
    const byKey = new Map();
    for (let i = this.rangeDays - 1; i >= 0; i--) {
      const dt = new Date(today);
      dt.setDate(today.getDate() - i);
      const key = toDayKey(dt);
      const bucket = {
        key,
        date: dt,
        label: formatAxisLabel(dt),
        newUsers: 0,
        cumulativeUsers: 0,
        activeUids: new Set(),
        dau: 0,
        dauAvg3: 0
      };
      days.push(bucket);
      byKey.set(key, bucket);
    }

    const startTime = days[0].date.getTime();
    let baselineUsers = 0;

    // 1. User Growth & Profile Activity (/users)
    users.forEach((u) => {
      const uid = u.id || u.userId;
      const created = toDateSafe(u.createdAt);
      const updated = toDateSafe(u.updatedAt);

      if (created) {
        const cDay = new Date(created.getFullYear(), created.getMonth(), created.getDate());
        if (cDay.getTime() < startTime) {
          baselineUsers += 1;
        } else {
          const b = byKey.get(toDayKey(cDay));
          if (b) {
            b.newUsers += 1;
            if (uid) b.activeUids.add(uid);
          }
        }
      } else {
        // Legacy profile without createdAt belongs to initial baseline
        baselineUsers += 1;
      }

      if (updated && uid) {
        const uKey = toDayKey(updated);
        byKey.get(uKey)?.activeUids.add(uid);
      }
    });

    // 2. Listing Activity (/properties)
    properties.forEach((p) => {
      const uid = p.ownerId;
      if (!uid) return;
      [p.createdAt, p.updatedAt].forEach((ts) => {
        const dt = toDateSafe(ts);
        if (dt) byKey.get(toDayKey(dt))?.activeUids.add(uid);
      });
    });

    // 3. Chat Activity (/chats)
    chats.forEach((c) => {
      const participants = [c.buyerId, c.ownerId].filter(Boolean);
      [c.createdAt, c.updatedAt].forEach((ts) => {
        const dt = toDateSafe(ts);
        if (!dt) return;
        const b = byKey.get(toDayKey(dt));
        if (b) participants.forEach((uid) => b.activeUids.add(uid));
      });
    });

    // Cumulative totals & 3-day moving average for DAU
    let runningTotal = baselineUsers;
    days.forEach((d, idx) => {
      runningTotal += d.newUsers;
      d.cumulativeUsers = runningTotal;
      d.dau = d.activeUids.size;
      const slice = days.slice(Math.max(0, idx - 2), idx + 1);
      d.dauAvg3 = Number((slice.reduce((acc, item) => acc + item.dau, 0) / slice.length).toFixed(2));
    });

    return days;
  }

  getExportRows() {
    return this.lastSeries.map((d) => [
      d.key,
      d.newUsers,
      d.cumulativeUsers,
      d.dau,
      d.dauAvg3
    ]);
  }

  render(payload) {
    this.lastPayload = payload || { users: [], properties: [], chats: [] };
    const series = this.computeSeries(this.lastPayload);
    this.lastSeries = series;

    this.updateKpis(series, this.lastPayload.users || []);

    if (typeof window.d3 === "undefined") {
      ["chart-user-growth", "chart-dau", "chart-role-mix"].forEach((id) => {
        const el = document.getElementById(id);
        if (el) el.innerHTML = `<div class="empty-state">D3.js library failed to load. Check internet connection.</div>`;
      });
      return;
    }

    this.renderUserGrowthChart(series);
    this.renderDauChart(series);
    this.renderRoleMixChart(this.lastPayload.users || []);
  }

  updateKpis(series, users) {
    const totalUsers = users.length;
    const newInRange = series.reduce((acc, d) => acc + d.newUsers, 0);
    const todayDau = series.length ? series[series.length - 1].dau : 0;
    const peakDau = series.reduce((m, d) => Math.max(m, d.dau), 0);
    const avgDau = series.length
      ? (series.reduce((acc, d) => acc + d.dau, 0) / series.length).toFixed(1)
      : "0.0";
    const stickiness = totalUsers > 0 ? Math.round((Number(avgDau) / totalUsers) * 100) : 0;

    const set = (id, val) => {
      const el = document.getElementById(id);
      if (el) el.textContent = String(val);
    };
    set("an-kpi-total", totalUsers);
    set("an-kpi-new", `+${newInRange}`);
    set("an-kpi-new-sub", `Joined in last ${this.rangeDays} days`);
    set("an-kpi-dau-today", todayDau);
    set("an-kpi-dau-peak", `Peak DAU: ${peakDau} users`);
    set("an-kpi-dau-avg", avgDau);
    set("an-kpi-stickiness", `${stickiness}% avg daily engagement`);
  }

  renderUserGrowthChart(series) {
    const d3 = window.d3;
    const container = document.getElementById("chart-user-growth");
    if (!container) return;
    container.innerHTML = "";

    const width = Math.max(320, container.clientWidth || 560);
    const height = 290;
    const margin = { top: 20, right: 24, bottom: 38, left: 46 };
    const innerW = width - margin.left - margin.right;
    const innerH = height - margin.top - margin.bottom;

    const primary = getCssVar("--primary", "#1e3a8a");
    const emerald = getCssVar("--emerald", "#059669");
    const muted = getCssVar("--text-muted", "#64748b");
    const border = getCssVar("--border", "#e2e8f0");

    const svg = d3
      .select(container)
      .append("svg")
      .attr("width", width)
      .attr("height", height)
      .attr("viewBox", `0 0 ${width} ${height}`)
      .style("display", "block");

    const defs = svg.append("defs");
    const grad = defs
      .append("linearGradient")
      .attr("id", "pbGrowthAreaGrad")
      .attr("x1", "0%")
      .attr("y1", "0%")
      .attr("x2", "0%")
      .attr("y2", "100%");
    grad.append("stop").attr("offset", "0%").attr("stop-color", primary).attr("stop-opacity", 0.3);
    grad.append("stop").attr("offset", "100%").attr("stop-color", primary).attr("stop-opacity", 0.02);

    const g = svg.append("g").attr("transform", `translate(${margin.left},${margin.top})`);

    const x = d3
      .scaleBand()
      .domain(series.map((d) => d.key))
      .range([0, innerW])
      .padding(0.28);

    const maxY = Math.max(4, d3.max(series, (d) => d.cumulativeUsers) || 0);
    const y = d3.scaleLinear().domain([0, maxY * 1.15]).nice().range([innerH, 0]);

    // Gridlines
    g.append("g")
      .attr("class", "d3-grid")
      .call(d3.axisLeft(y).ticks(5).tickSize(-innerW).tickFormat(""))
      .call((sel) => sel.select(".domain").remove())
      .call((sel) => sel.selectAll("line").attr("stroke", border).attr("stroke-dasharray", "3,3"));

    // Area under cumulative line
    const area = d3
      .area()
      .x((d) => x(d.key) + x.bandwidth() / 2)
      .y0(innerH)
      .y1((d) => y(d.cumulativeUsers))
      .curve(d3.curveMonotoneX);

    g.append("path").datum(series).attr("fill", "url(#pbGrowthAreaGrad)").attr("d", area);

    // Daily new signups bars
    g.selectAll(".signup-bar")
      .data(series)
      .enter()
      .append("rect")
      .attr("class", "signup-bar")
      .attr("x", (d) => x(d.key))
      .attr("y", (d) => y(d.newUsers))
      .attr("width", x.bandwidth())
      .attr("height", (d) => Math.max(0, innerH - y(d.newUsers)))
      .attr("rx", 3)
      .attr("fill", emerald)
      .attr("opacity", 0.72);

    // Cumulative growth line
    const line = d3
      .line()
      .x((d) => x(d.key) + x.bandwidth() / 2)
      .y((d) => y(d.cumulativeUsers))
      .curve(d3.curveMonotoneX);

    g.append("path")
      .datum(series)
      .attr("fill", "none")
      .attr("stroke", primary)
      .attr("stroke-width", 2.75)
      .attr("d", line);

    // Data dots for smaller windows
    if (series.length <= 31) {
      g.selectAll(".growth-dot")
        .data(series)
        .enter()
        .append("circle")
        .attr("cx", (d) => x(d.key) + x.bandwidth() / 2)
        .attr("cy", (d) => y(d.cumulativeUsers))
        .attr("r", 3.2)
        .attr("fill", primary)
        .attr("stroke", "#ffffff")
        .attr("stroke-width", 1.5);
    }

    // Axes
    const tickEvery = Math.max(1, Math.ceil(series.length / 7));
    const xAxis = d3
      .axisBottom(x)
      .tickValues(series.filter((_, idx) => idx % tickEvery === 0 || idx === series.length - 1).map((d) => d.key))
      .tickFormat((key) => series.find((d) => d.key === key)?.label || key);

    g.append("g")
      .attr("transform", `translate(0,${innerH})`)
      .call(xAxis)
      .call((sel) => sel.select(".domain").attr("stroke", border))
      .call((sel) => sel.selectAll("text").attr("fill", muted).style("font-size", "11px"));

    g.append("g")
      .call(d3.axisLeft(y).ticks(5).tickFormat(d3.format("d")))
      .call((sel) => sel.select(".domain").remove())
      .call((sel) => sel.selectAll("text").attr("fill", muted).style("font-size", "11px"));

    // Interactive hover bands
    g.selectAll(".hover-band")
      .data(series)
      .enter()
      .append("rect")
      .attr("x", (d) => x(d.key))
      .attr("y", 0)
      .attr("width", x.bandwidth() + x.step() * 0.28)
      .attr("height", innerH)
      .attr("fill", "transparent")
      .style("cursor", "crosshair")
      .on("mousemove", (event, d) => {
        this.showTooltip(
          event,
          `<strong>${d.label} (${d.key})</strong>
           <div>Total Users: <strong>${d.cumulativeUsers}</strong></div>
           <div>New Signups: <strong>+${d.newUsers}</strong></div>`
        );
      })
      .on("mouseleave", () => this.hideTooltip());
  }

  renderDauChart(series) {
    const d3 = window.d3;
    const container = document.getElementById("chart-dau");
    if (!container) return;
    container.innerHTML = "";

    const width = Math.max(320, container.clientWidth || 560);
    const height = 290;
    const margin = { top: 20, right: 24, bottom: 38, left: 46 };
    const innerW = width - margin.left - margin.right;
    const innerH = height - margin.top - margin.bottom;

    const emerald = getCssVar("--emerald", "#059669");
    const amber = getCssVar("--amber", "#d97706");
    const muted = getCssVar("--text-muted", "#64748b");
    const border = getCssVar("--border", "#e2e8f0");

    const svg = d3
      .select(container)
      .append("svg")
      .attr("width", width)
      .attr("height", height)
      .attr("viewBox", `0 0 ${width} ${height}`)
      .style("display", "block");

    const g = svg.append("g").attr("transform", `translate(${margin.left},${margin.top})`);

    const x = d3
      .scaleBand()
      .domain(series.map((d) => d.key))
      .range([0, innerW])
      .padding(0.26);

    const maxDau = Math.max(4, d3.max(series, (d) => d.dau) || 0);
    const y = d3.scaleLinear().domain([0, maxDau * 1.2]).nice().range([innerH, 0]);

    // Gridlines
    g.append("g")
      .call(d3.axisLeft(y).ticks(5).tickSize(-innerW).tickFormat(""))
      .call((sel) => sel.select(".domain").remove())
      .call((sel) => sel.selectAll("line").attr("stroke", border).attr("stroke-dasharray", "3,3"));

    // DAU vertical bars
    g.selectAll(".dau-bar")
      .data(series)
      .enter()
      .append("rect")
      .attr("class", "dau-bar")
      .attr("x", (d) => x(d.key))
      .attr("y", (d) => y(d.dau))
      .attr("width", x.bandwidth())
      .attr("height", (d) => Math.max(0, innerH - y(d.dau)))
      .attr("rx", 4)
      .attr("fill", emerald)
      .attr("opacity", 0.85)
      .style("cursor", "pointer")
      .on("mousemove", (event, d) => {
        this.showTooltip(
          event,
          `<strong>${d.label} (${d.key})</strong>
           <div>Daily Active Users: <strong>${d.dau}</strong></div>
           <div>3-Day Avg: <strong>${d.dauAvg3}</strong></div>`
        );
      })
      .on("mouseleave", () => this.hideTooltip());

    // 3-Day Moving Average trend line
    const avgLine = d3
      .line()
      .x((d) => x(d.key) + x.bandwidth() / 2)
      .y((d) => y(d.dauAvg3))
      .curve(d3.curveMonotoneX);

    g.append("path")
      .datum(series)
      .attr("fill", "none")
      .attr("stroke", amber)
      .attr("stroke-width", 2.5)
      .attr("stroke-dasharray", "5,3")
      .attr("d", avgLine);

    // Axes
    const tickEvery = Math.max(1, Math.ceil(series.length / 7));
    const xAxis = d3
      .axisBottom(x)
      .tickValues(series.filter((_, idx) => idx % tickEvery === 0 || idx === series.length - 1).map((d) => d.key))
      .tickFormat((key) => series.find((d) => d.key === key)?.label || key);

    g.append("g")
      .attr("transform", `translate(0,${innerH})`)
      .call(xAxis)
      .call((sel) => sel.select(".domain").attr("stroke", border))
      .call((sel) => sel.selectAll("text").attr("fill", muted).style("font-size", "11px"));

    g.append("g")
      .call(d3.axisLeft(y).ticks(5).tickFormat(d3.format("d")))
      .call((sel) => sel.select(".domain").remove())
      .call((sel) => sel.selectAll("text").attr("fill", muted).style("font-size", "11px"));
  }

  renderRoleMixChart(users) {
    const d3 = window.d3;
    const container = document.getElementById("chart-role-mix");
    const legendEl = document.getElementById("chart-role-legend");
    if (!container || !legendEl) return;
    container.innerHTML = "";
    legendEl.innerHTML = "";

    const counts = Object.keys(ROLE_LABELS)
      .map((role) => ({
        role,
        label: ROLE_LABELS[role],
        color: ROLE_COLORS[role],
        count: users.filter((u) => (u.role || "BUYER") === role).length
      }))
      .filter((d) => d.count > 0);

    if (!counts.length) {
      container.innerHTML = `<div class="empty-state">No registered users yet.</div>`;
      return;
    }

    const size = 210;
    const radius = size / 2 - 10;
    const textColor = getCssVar("--text", "#0f172a");
    const muted = getCssVar("--text-muted", "#64748b");

    const svg = d3
      .select(container)
      .append("svg")
      .attr("width", size)
      .attr("height", size)
      .attr("viewBox", `0 0 ${size} ${size}`);

    const g = svg.append("g").attr("transform", `translate(${size / 2},${size / 2})`);
    const pie = d3.pie().value((d) => d.count).sort(null);
    const arc = d3.arc().innerRadius(radius * 0.58).outerRadius(radius);

    g.selectAll("path")
      .data(pie(counts))
      .enter()
      .append("path")
      .attr("d", arc)
      .attr("fill", (d) => d.data.color)
      .attr("stroke", getCssVar("--surface", "#ffffff"))
      .attr("stroke-width", 2.5)
      .style("cursor", "pointer")
      .on("mousemove", (event, d) => {
        const pct = Math.round((d.data.count / Math.max(1, users.length)) * 100);
        this.showTooltip(
          event,
          `<strong>${d.data.label}</strong><div>${d.data.count} users (${pct}%)</div>`
        );
      })
      .on("mouseleave", () => this.hideTooltip());

    g.append("text")
      .attr("text-anchor", "middle")
      .attr("dy", "-0.15em")
      .attr("fill", textColor)
      .style("font-size", "22px")
      .style("font-weight", "800")
      .text(String(users.length));

    g.append("text")
      .attr("text-anchor", "middle")
      .attr("dy", "1.35em")
      .attr("fill", muted)
      .style("font-size", "11px")
      .style("font-weight", "600")
      .text("Total Users");

    legendEl.innerHTML = counts
      .map((c) => {
        const pct = Math.round((c.count / Math.max(1, users.length)) * 100);
        return `
          <div class="d3-legend-item">
            <span class="d3-legend-swatch" style="background:${c.color}"></span>
            <span>${c.label}</span>
            <strong>${c.count} (${pct}%)</strong>
          </div>`;
      })
      .join("");
  }
}
