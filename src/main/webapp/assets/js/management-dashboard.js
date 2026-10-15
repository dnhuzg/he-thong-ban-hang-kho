(() => {
    "use strict";

    // DỮ LIỆU MINH HỌA. Không kết nối cơ sở dữ liệu/API.
    const demo = {
        today: 18500000,
        month: 486000000,
        approval: 12,
        dispatch: 8,
        stock: 17,
        daily: [11, 14, 12, 19, 16, 23, 18, 21, 26, 22, 29, 31],
        regions: [
            { name: "Miền Bắc", value: 186 },
            { name: "Miền Trung", value: 112 },
            { name: "Miền Nam", value: 188 }
        ],
        overdue: [
            { agency: "Đại lý minh họa A", region: "Miền Bắc", days: 18, amount: 42000000 },
            { agency: "Đại lý minh họa B", region: "Miền Nam", days: 12, amount: 26500000 },
            { agency: "Đại lý minh họa C", region: "Miền Trung", days: 7, amount: 13700000 },
            { agency: "Đại lý minh họa D", region: "Miền Bắc", days: 4, amount: 8200000 }
        ]
    };

    const money = value => new Intl.NumberFormat("vi-VN").format(value);
    const number = value => new Intl.NumberFormat("vi-VN").format(value);
    const byId = id => document.getElementById(id);
    const ns = "http://www.w3.org/2000/svg";

    function svgNode(name, attributes = {}, text = null) {
        const element = document.createElementNS(ns, name);
        Object.entries(attributes).forEach(([key, value]) =>
            element.setAttribute(key, String(value))
        );
        if (text !== null) element.textContent = String(text);
        return element;
    }

    function renderDailyChart() {
        const root = byId("mdash-daily-chart");
        if (!root) return;

        const svg = svgNode("svg", {
            viewBox: "0 0 620 270",
            "aria-hidden": "true",
            focusable: "false"
        });

        const left = 48, top = 22, width = 550, height = 195;
        const max = 40;
        for (let tick = 0; tick <= 4; tick++) {
            const y = top + height - tick * height / 4;
            svg.appendChild(svgNode("line", {
                x1: left, y1: y, x2: left + width, y2: y,
                class: "grid-line"
            }));
            svg.appendChild(svgNode("text", {
                x: 38, y: y + 4, "text-anchor": "end",
                class: "axis-label"
            }, tick * 10));
        }

        const points = demo.daily.map((value, index) => ({
            x: left + index * width / (demo.daily.length - 1),
            y: top + height - value / max * height
        }));

        svg.appendChild(svgNode("polyline", {
            points: points.map(p => `${p.x},${p.y}`).join(" "),
            class: "chart-line"
        }));

        points.forEach((point, index) => {
            svg.appendChild(svgNode("circle", {
                cx: point.x, cy: point.y, r: 4,
                class: "chart-dot"
            }));
            if (index % 2 === 0 || index === points.length - 1) {
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

        const svg = svgNode("svg", {
            viewBox: "0 0 430 270",
            "aria-hidden": "true",
            focusable: "false"
        });

        const baseline = 217;
        const max = 220;

        demo.regions.forEach((region, index) => {
            const x = 52 + index * 125;
            const height = region.value / max * 175;
            svg.appendChild(svgNode("rect", {
                x, y: baseline - height,
                width: 65, height,
                rx: 5, class: "chart-bar"
            }));
            svg.appendChild(svgNode("text", {
                x: x + 32.5, y: baseline - height - 10,
                "text-anchor": "middle", class: "axis-label"
            }, `${region.value} tr`));
            svg.appendChild(svgNode("text", {
                x: x + 32.5, y: 242,
                "text-anchor": "middle", class: "axis-label"
            }, region.name));
        });

        svg.appendChild(svgNode("line", {
            x1: 25, y1: baseline, x2: 410, y2: baseline,
            class: "grid-line"
        }));
        root.replaceChildren(svg);
    }

    function renderOverdue() {
        const tbody = byId("mdash-overdue-body");
        if (!tbody) return;

        const rows = [...demo.overdue].sort(
            (a, b) => b.amount - a.amount
        );
        tbody.replaceChildren();

        rows.forEach(item => {
            const row = document.createElement("tr");
            [item.agency, item.region, `${number(item.days)} ngày`,
                `${money(item.amount)} ₫`].forEach(value => {
                const cell = document.createElement("td");
                cell.textContent = value;
                row.appendChild(cell);
            });
            tbody.appendChild(row);
        });
    }

    function init() {
        const totalDebt = demo.overdue.reduce(
            (total, item) => total + item.amount, 0
        );

        const values = {
            "mdash-today": `${money(demo.today)} ₫`,
            "mdash-month": `${money(demo.month)} ₫`,
            "mdash-approval": number(demo.approval),
            "mdash-dispatch": number(demo.dispatch),
            "mdash-debt": `${money(totalDebt)} ₫`,
            "mdash-stock": number(demo.stock)
        };

        Object.entries(values).forEach(([id, value]) => {
            const element = byId(id);
            if (element) element.textContent = value;
        });

        renderDailyChart();
        renderRegionChart();
        renderOverdue();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init, { once: true });
    } else {
        init();
    }
})();