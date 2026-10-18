(() => {
    "use strict";

    // Số liệu do server gắn vào data-values / data-labels của từng biểu đồ (GHTK-80).
    const ns = "http://www.w3.org/2000/svg";
    const byId = id => document.getElementById(id);
    const values = root => (root.dataset.values || "").split(",").filter(v => v !== "").map(Number);
    // Trục hiển thị theo triệu đồng cho gọn
    const million = value => new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(value / 1e6);

    function svgNode(name, attributes = {}, text = null) {
        const element = document.createElementNS(ns, name);
        Object.entries(attributes).forEach(([key, value]) =>
            element.setAttribute(key, String(value))
        );
        if (text !== null) element.textContent = String(text);
        return element;
    }

    function empty(root) {
        const note = document.createElement("p");
        note.className = "mdash-empty";
        note.textContent = "Chưa có hoá đơn trong tháng.";
        root.replaceChildren(note);
    }

    function renderDailyChart() {
        const root = byId("mdash-daily-chart");
        if (!root) return;
        const daily = values(root);
        if (!daily.some(v => v > 0)) return empty(root);

        const svg = svgNode("svg", {
            viewBox: "0 0 620 270",
            "aria-hidden": "true",
            focusable: "false"
        });

        const left = 48, top = 22, width = 550, height = 195;
        const max = Math.max(...daily);
        for (let tick = 0; tick <= 4; tick++) {
            const y = top + height - tick * height / 4;
            svg.appendChild(svgNode("line", {
                x1: left, y1: y, x2: left + width, y2: y,
                class: "grid-line"
            }));
            svg.appendChild(svgNode("text", {
                x: 38, y: y + 4, "text-anchor": "end",
                class: "axis-label"
            }, million(max * tick / 4)));
        }

        const step = daily.length > 1 ? width / (daily.length - 1) : 0;
        const points = daily.map((value, index) => ({
            x: left + index * step,
            y: top + height - value / max * height
        }));

        svg.appendChild(svgNode("polyline", {
            points: points.map(p => `${p.x},${p.y}`).join(" "),
            class: "chart-line"
        }));

        const labelEvery = Math.ceil(points.length / 12);
        points.forEach((point, index) => {
            svg.appendChild(svgNode("circle", {
                cx: point.x, cy: point.y, r: 4,
                class: "chart-dot"
            }));
            if (index % labelEvery === 0 || index === points.length - 1) {
                svg.appendChild(svgNode("text", {
                    x: point.x, y: 245,
                    "text-anchor": "middle", class: "axis-label"
                }, index + 1));
            }
        });

        root.replaceChildren(svg);
    }

    function renderRegionChart() {
        const root = byId("mdash-region-chart");
        if (!root) return;
        const regions = values(root);
        const names = (root.dataset.labels || "").split("|");
        if (!regions.length) return empty(root);

        const svg = svgNode("svg", {
            viewBox: "0 0 430 270",
            "aria-hidden": "true",
            focusable: "false"
        });

        const baseline = 217;
        const max = Math.max(...regions);
        const slot = 385 / regions.length;
        const barWidth = Math.min(65, slot * 0.6);

        regions.forEach((value, index) => {
            const x = 25 + index * slot + (slot - barWidth) / 2;
            const height = value / max * 175;
            svg.appendChild(svgNode("rect", {
                x, y: baseline - height,
                width: barWidth, height,
                rx: 5, class: "chart-bar"
            }));
            svg.appendChild(svgNode("text", {
                x: x + barWidth / 2, y: baseline - height - 10,
                "text-anchor": "middle", class: "axis-label"
            }, `${million(value)} tr`));
            svg.appendChild(svgNode("text", {
                x: x + barWidth / 2, y: 242,
                "text-anchor": "middle", class: "axis-label"
            }, names[index] || ""));
        });

        svg.appendChild(svgNode("line", {
            x1: 25, y1: baseline, x2: 410, y2: baseline,
            class: "grid-line"
        }));
        root.replaceChildren(svg);
    }

    function init() {
        renderDailyChart();
        renderRegionChart();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init, { once: true });
    } else {
        init();
    }
})();
