(() => {
  "use strict";

  const root = document.getElementById("aging-screen");
  if (!root) return;

  const $ = (id) => document.getElementById(id);
  const dayMs = 86400000;
  const today = new Date();
  const todayUTC = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
  const iso = (offset) => new Date(todayUTC + offset * dayMs).toISOString().slice(0, 10);
  const dateUTC = (value) => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return NaN;
    const stamp = Date.parse(`${value}T00:00:00Z`);
    return Number.isFinite(stamp) && new Date(stamp).toISOString().slice(0, 10) === value ? stamp : NaN;
  };
  const money = (value) => `${new Intl.NumberFormat("vi-VN").format(value)} ₫`;
  const displayDate = (value) => value.split("-").reverse().join("/");
  const escapeHtml = (value) => String(value).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
  const bucketLabels = ["0–30 ngày", "31–60 ngày", "61–90 ngày", "Trên 90 ngày"];

  // Dữ liệu minh họa; nhân viên phụ trách được gắn rõ theo hồ sơ đại lý mẫu.
  // Bản FE này chưa đọc công nợ thật từ MySQL.
  const invoices = [
    { agency: "DL-001", name: "Đại lý Minh Anh", employeeId: "NV-001", employeeName: "Nguyễn Văn An", invoice: "HD-2401", issued: iso(-12), due: iso(5), amount: 8200000, paid: 3000000 },
    { agency: "DL-001", name: "Đại lý Minh Anh", employeeId: "NV-001", employeeName: "Nguyễn Văn An", invoice: "HD-2367", issued: iso(-48), due: iso(-18), amount: 11500000, paid: 5000000 },
    { agency: "DL-002", name: "Đại lý Thái Nguyên", employeeId: "NV-002", employeeName: "Trần Thu Hà", invoice: "HD-2314", issued: iso(-77), due: iso(-46), amount: 9600000, paid: 2400000 },
    { agency: "DL-002", name: "Đại lý Thái Nguyên", employeeId: "NV-002", employeeName: "Trần Thu Hà", invoice: "HD-2408", issued: iso(-8), due: iso(22), amount: 4200000, paid: 0 },
    { agency: "DL-003", name: "Đại lý Hương Giang", employeeId: "NV-003", employeeName: "Phạm Quốc Bảo", invoice: "HD-2278", issued: iso(-112), due: iso(-82), amount: 14500000, paid: 4500000 },
    { agency: "DL-004", name: "Đại lý Bắc Ninh", employeeId: "NV-001", employeeName: "Nguyễn Văn An", invoice: "HD-2355", issued: iso(-34), due: iso(-4), amount: 6800000, paid: 1800000 },
    { agency: "DL-005", name: "Đại lý Thành Công", employeeId: "NV-002", employeeName: "Trần Thu Hà", invoice: "HD-2202", issued: iso(-126), due: iso(-96), amount: 7200000, paid: 7200000 }
  ];

  const filters = $("aging-filters");
  const asOf = $("aging-as-of");
  asOf.value = iso(0);
  let displayed = [];
  let staffDisplayed = [];
  let displayTotals = null;
  let highestRisk = null;

  const groupFor = (days) => days <= 30 ? 0 : days <= 60 ? 1 : days <= 90 ? 2 : 3;
  const emptyTotals = () => ({ amount: 0, paid: 0, balance: 0, overdueBalance: 0, buckets: [0, 0, 0, 0] });
  const riskSort = (a, b) => b.overdueBalance - a.overdueBalance || b.buckets[3] - a.buckets[3] || b.maxOverdueDays - a.maxOverdueDays || b.balance - a.balance;

  function render() {
    const stamp = dateUTC(asOf.value);
    if (!Number.isFinite(stamp)) return;
    const search = $("aging-search").value.trim().toLocaleLowerCase("vi-VN");
    const selectedBucket = $("aging-bucket").value;
    const overdueOnly = $("aging-overdue").checked;
    const grouped = new Map();
    const staffGroups = new Map();
    const totals = emptyTotals();

    invoices.forEach((invoice) => {
      const issued = dateUTC(invoice.issued);
      const due = dateUTC(invoice.due);
      const balance = invoice.amount - invoice.paid;
      if (issued > stamp || balance <= 0) return;
      const ageDays = Math.max(0, Math.floor((stamp - issued) / dayMs));
      const bucket = groupFor(ageDays);
      const isOverdue = due < stamp;
      const daysPastDue = isOverdue ? Math.max(1, Math.floor((stamp - due) / dayMs)) : 0;
      const searchText = `${invoice.agency} ${invoice.name} ${invoice.invoice} ${invoice.employeeId} ${invoice.employeeName}`.toLocaleLowerCase("vi-VN");
      if (search && !searchText.includes(search)) return;
      if (selectedBucket !== "" && bucket !== Number(selectedBucket)) return;
      if (overdueOnly && !isOverdue) return;

      if (!grouped.has(invoice.agency)) {
        grouped.set(invoice.agency, {
          agency: invoice.agency, name: invoice.name,
          employeeId: invoice.employeeId, employeeName: invoice.employeeName,
          ...emptyTotals(), overdue: false, overdueInvoices: 0, maxOverdueDays: 0, invoices: []
        });
      }
      const agency = grouped.get(invoice.agency);
      agency.amount += invoice.amount;
      agency.paid += invoice.paid;
      agency.balance += balance;
      agency.buckets[bucket] += balance;
      agency.overdue ||= isOverdue;
      agency.invoices.push(invoice.invoice);
      if (isOverdue) {
        agency.overdueBalance += balance;
        agency.overdueInvoices += 1;
        agency.maxOverdueDays = Math.max(agency.maxOverdueDays, daysPastDue);
      }

      if (!staffGroups.has(invoice.employeeId)) {
        staffGroups.set(invoice.employeeId, {
          employeeId: invoice.employeeId, employeeName: invoice.employeeName,
          ...emptyTotals(), agencies: new Set(), overdueInvoices: 0, maxOverdueDays: 0
        });
      }
      const staff = staffGroups.get(invoice.employeeId);
      staff.amount += invoice.amount;
      staff.paid += invoice.paid;
      staff.balance += balance;
      staff.buckets[bucket] += balance;
      staff.agencies.add(invoice.agency);
      if (isOverdue) {
        staff.overdueBalance += balance;
        staff.overdueInvoices += 1;
        staff.maxOverdueDays = Math.max(staff.maxOverdueDays, daysPastDue);
      }

      totals.amount += invoice.amount;
      totals.paid += invoice.paid;
      totals.balance += balance;
      totals.buckets[bucket] += balance;
      if (isOverdue) totals.overdueBalance += balance;
    });

    displayed = [...grouped.values()].sort(riskSort);
    staffDisplayed = [...staffGroups.values()].sort((a, b) => b.overdueBalance - a.overdueBalance || b.balance - a.balance || a.employeeName.localeCompare(b.employeeName, "vi"));
    highestRisk = displayed.filter((agency) => agency.overdueBalance > 0).sort(riskSort)[0] || null;
    displayTotals = totals;

    $("aging-total").textContent = money(totals.balance);
    totals.buckets.forEach((value, i) => {
      $(`aging-bucket-${i}`).textContent = money(value);
      $(`aging-table-${i}`).textContent = money(value);
      $(`aging-staff-${i}`).textContent = money(staffDisplayed.reduce((sum, staff) => sum + staff.buckets[i], 0));
    });
    $("aging-table-total").textContent = money(totals.balance);
    $("aging-count").textContent = `${displayed.length} đại lý`;
    $("aging-footer-count").textContent = `Hiển thị ${displayed.length} đại lý`;
    $("aging-balance").textContent = `Đối chiếu: ${money(totals.balance)} = ${money(totals.buckets.reduce((sum, value) => sum + value, 0))}`;
    $("aging-empty").hidden = displayed.length !== 0;

    if (highestRisk) {
      $("aging-risk-name").textContent = `${highestRisk.name} (${highestRisk.agency})`;
      $("aging-risk-meta").textContent = `Nhân viên phụ trách: ${highestRisk.employeeName} · ${highestRisk.overdueInvoices} hoá đơn quá hạn · Tổng còn nợ ${money(highestRisk.balance)}`;
      $("aging-risk-balance").textContent = money(highestRisk.overdueBalance);
      $("aging-risk-days").textContent = `Khoản quá hạn lâu nhất: ${highestRisk.maxOverdueDays} ngày`;
    } else {
      $("aging-risk-name").textContent = "Không có đại lý quá hạn trong bộ lọc";
      $("aging-risk-meta").textContent = "Thử bỏ bộ lọc quá hạn hoặc thay đổi ngày đối chiếu.";
      $("aging-risk-balance").textContent = money(0);
      $("aging-risk-days").textContent = "";
    }

    $("aging-rows").innerHTML = displayed.map((agency) => `<tr class="${highestRisk && agency.agency === highestRisk.agency ? "aging-table__risk-row" : ""}">
      <th scope="row"><span class="aging-table__name">${escapeHtml(agency.name)}</span><span class="aging-table__meta">${escapeHtml(agency.agency)} · ${escapeHtml(agency.invoices.join(", "))}</span></th>
      <td>${escapeHtml(agency.employeeName)}</td>
      <td class="aging-number">${money(agency.amount)}</td><td class="aging-number">${money(agency.paid)}</td>
      <td class="aging-number aging-table__balance">${money(agency.balance)}</td>
      ${agency.buckets.map((value) => `<td class="aging-number">${money(value)}</td>`).join("")}
      <td class="${agency.overdue ? "aging-table__late" : ""}">${agency.overdue ? `Quá hạn ${agency.maxOverdueDays} ngày` : "Chưa quá hạn"}</td>
    </tr>`).join("");
    $("aging-mobile").innerHTML = displayed.map((agency) => `<article class="aging-mobile__card ${highestRisk && agency.agency === highestRisk.agency ? "aging-mobile__card--risk" : ""}">
      <strong>${escapeHtml(agency.name)}</strong><small>${escapeHtml(agency.agency)} · ${escapeHtml(agency.employeeName)}</small>
      <div class="aging-mobile__values"><span>Còn nợ</span><strong>${money(agency.balance)}</strong></div>
      <small>${agency.overdue ? `Nợ quá hạn ${money(agency.overdueBalance)} · lâu nhất ${agency.maxOverdueDays} ngày` : "Chưa có khoản quá hạn"}</small>
      <small>0–30: ${money(agency.buckets[0])} · 31–60: ${money(agency.buckets[1])}<br>61–90: ${money(agency.buckets[2])} · &gt;90: ${money(agency.buckets[3])}</small>
    </article>`).join("");

    $("aging-staff-count").textContent = `${staffDisplayed.length} nhân viên`;
    $("aging-staff-overdue-total").textContent = money(staffDisplayed.reduce((sum, staff) => sum + staff.overdueBalance, 0));
    $("aging-staff-balance-total").textContent = money(staffDisplayed.reduce((sum, staff) => sum + staff.balance, 0));
    $("aging-staff-rows").innerHTML = staffDisplayed.map((staff) => `<tr>
      <td>${escapeHtml(staff.employeeId)}</td><th scope="row">${escapeHtml(staff.employeeName)}</th>
      <td class="aging-number">${staff.agencies.size}</td><td class="aging-number ${staff.overdueBalance > 0 ? "aging-table__late" : ""}">${money(staff.overdueBalance)}</td>
      ${staff.buckets.map((value) => `<td class="aging-number">${money(value)}</td>`).join("")}
      <td class="aging-number aging-table__balance">${money(staff.balance)}</td>
    </tr>`).join("");
    $("aging-staff-empty").hidden = staffDisplayed.length !== 0;
  }

  filters.addEventListener("input", render);
  filters.addEventListener("change", render);
  filters.addEventListener("reset", () => { window.setTimeout(() => { asOf.value = iso(0); render(); }, 0); });

  // Xuất XLSX thật, dùng chung tiện ích OOXML nội bộ đã có trên trang sổ chi tiết công nợ.
  $("aging-export").addEventListener("click", () => {
    if (!displayTotals || !window.OMSXlsx || typeof window.OMSXlsx.build !== "function") {
      $("aging-export-feedback").textContent = "Không thể tạo tệp Excel. Vui lòng tải lại trang và thử lại.";
      return;
    }
    const rows = [
      ["BÁO CÁO CÔNG NỢ QUÁ HẠN THEO TUỔI NỢ · DỮ LIỆU MẪU"],
      ["Ngày đối chiếu", displayDate(asOf.value)],
      ["Bộ lọc nhóm tuổi nợ", $("aging-bucket").selectedOptions[0].textContent, "Chỉ xem quá hạn", $("aging-overdue").checked ? "Có" : "Không", "Từ khoá", $("aging-search").value.trim() || "Tất cả"],
      [],
      ["TỔNG HỢP THEO NHÓM TUỔI NỢ"],
      ["Tổng còn nợ (VND)", displayTotals.balance, "Nợ quá hạn (VND)", displayTotals.overdueBalance],
      ["Nhóm tuổi nợ", "Số dư (VND)"],
      ...bucketLabels.map((label, i) => [label, displayTotals.buckets[i]]),
      [],
      ["ĐẠI LÝ CÓ RỦI RO CAO NHẤT"],
      ["Mã đại lý", highestRisk ? highestRisk.agency : ""],
      ["Tên đại lý", highestRisk ? highestRisk.name : "Không có đại lý quá hạn"],
      ["Nhân viên phụ trách", highestRisk ? `${highestRisk.employeeId} · ${highestRisk.employeeName}` : ""],
      ["Dư nợ quá hạn (VND)", highestRisk ? highestRisk.overdueBalance : 0],
      ["Số hoá đơn quá hạn", highestRisk ? highestRisk.overdueInvoices : 0],
      ["Khoản quá hạn lâu nhất (ngày)", highestRisk ? highestRisk.maxOverdueDays : 0],
      [],
      ["TỔNG HỢP THEO NHÂN VIÊN PHỤ TRÁCH"],
      ["Mã nhân viên", "Nhân viên phụ trách", "Số đại lý", "Nợ quá hạn (VND)", "0–30 (VND)", "31–60 (VND)", "61–90 (VND)", ">90 (VND)", "Tổng còn nợ (VND)"],
      ...staffDisplayed.map((staff) => [staff.employeeId, staff.employeeName, staff.agencies.size, staff.overdueBalance, ...staff.buckets, staff.balance]),
      ["TỔNG", "", staffDisplayed.reduce((sum, staff) => sum + staff.agencies.size, 0), staffDisplayed.reduce((sum, staff) => sum + staff.overdueBalance, 0), ...[0, 1, 2, 3].map((i) => staffDisplayed.reduce((sum, staff) => sum + staff.buckets[i], 0)), staffDisplayed.reduce((sum, staff) => sum + staff.balance, 0)],
      [],
      ["CHI TIẾT CÔNG NỢ THEO ĐẠI LÝ"],
      ["Mã đại lý", "Tên đại lý", "Mã nhân viên", "Nhân viên phụ trách", "Hoá đơn", "Phải thu (VND)", "Đã thu (VND)", "Còn nợ (VND)", "0–30 (VND)", "31–60 (VND)", "61–90 (VND)", ">90 (VND)", "Nợ quá hạn (VND)", "Số ngày quá hạn lớn nhất"],
      ...displayed.map((agency) => [agency.agency, agency.name, agency.employeeId, agency.employeeName, agency.invoices.join(", "), agency.amount, agency.paid, agency.balance, ...agency.buckets, agency.overdueBalance, agency.maxOverdueDays]),
      ["TỔNG", "", "", "", "", displayTotals.amount, displayTotals.paid, displayTotals.balance, ...displayTotals.buckets, displayTotals.overdueBalance, ""]
    ];
    try {
      const bytes = window.OMSXlsx.build(rows);
      const url = URL.createObjectURL(new Blob([bytes], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = `bao-cao-cong-no-tuoi-no-${asOf.value}.xlsx`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 60000);
      $("aging-export-feedback").textContent = "Đã tạo tệp Excel theo bộ lọc đang áp dụng.";
    } catch (error) {
      console.error("Không thể xuất báo cáo tuổi nợ", error);
      $("aging-export-feedback").textContent = "Xuất Excel thất bại. Vui lòng tải lại trang và thử lại.";
    }
  });

  render();
})();
