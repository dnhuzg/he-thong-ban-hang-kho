// Tương tác giao diện dùng chung, không chứa logic nghiệp vụ.

document.addEventListener("DOMContentLoaded", () => {
  const app = document.querySelector(".app");

  // Sidebar dạng ngăn kéo trên màn hình nhỏ
  document.querySelectorAll("[data-sidebar-toggle]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const open = app.classList.toggle("is-sidebar-open");
      btn.setAttribute("aria-expanded", String(open));
    });
  });

  document.querySelectorAll("[data-sidebar-close]").forEach((el) => {
    el.addEventListener("click", () => {
      app.classList.remove("is-sidebar-open");
      document
        .querySelectorAll("[data-sidebar-toggle]")
        .forEach((btn) => btn.setAttribute("aria-expanded", "false"));
    });
  });

  // Ô chọn bộ lọc tự gửi form khi đổi giá trị (nút Lọc vẫn dùng được khi tắt JavaScript)
  document.querySelectorAll("select[data-autosubmit]").forEach((select) => {
    select.addEventListener("change", () => select.form.requestSubmit());
  });

  // Nhãn trạng thái bên cạnh ô chọn đổi theo lựa chọn hiện tại
  document.querySelectorAll("select[data-badge-target]").forEach((select) => {
    const badge = document.getElementById(select.dataset.badgeTarget);
    select.addEventListener("change", () => {
      badge.textContent = select.options[select.selectedIndex].text;
      badge.classList.toggle("badge--success", select.value === "ACTIVE");
    });
  });

  // Cây nhóm hàng: mở / đóng nhóm cha, ẩn mọi nhóm con cháu khi nhóm cha đang đóng.
  // Trạng thái nhớ trong trình duyệt (tiện cho người xem, không ảnh hưởng dữ liệu).
  document.querySelectorAll("table[data-tree]").forEach((table) => {
    const key = "oms.categoryTree.collapsed";
    const rows = [...table.querySelectorAll("tbody tr[data-cat-id]")];
    const parentOf = new Map(rows.map((r) => [r.dataset.catId, r.dataset.parentId]));
    let collapsed = new Set();
    try {
      collapsed = new Set(JSON.parse(localStorage.getItem(key) || "[]"));
    } catch (e) {
      collapsed = new Set();
    }

    const hiddenByAncestor = (id) => {
      for (let p = parentOf.get(id); p; p = parentOf.get(p)) {
        if (collapsed.has(p)) {
          return true;
        }
      }
      return false;
    };

    const refresh = () => {
      rows.forEach((row) => {
        row.hidden = hiddenByAncestor(row.dataset.catId);
        const btn = row.querySelector("[data-tree-toggle]");
        if (btn) {
          btn.setAttribute("aria-expanded", String(!collapsed.has(row.dataset.catId)));
        }
      });
    };

    table.addEventListener("click", (event) => {
      const btn = event.target.closest("[data-tree-toggle]");
      if (!btn) {
        return;
      }
      const id = btn.closest("tr").dataset.catId;
      if (collapsed.has(id)) {
        collapsed.delete(id);
      } else {
        collapsed.add(id);
      }
      try {
        localStorage.setItem(key, JSON.stringify([...collapsed]));
      } catch (e) {
        // trình duyệt chặn lưu trữ: vẫn dùng được, chỉ không nhớ lần sau
      }
      refresh();
    });

    refresh();
  });

  // Ô tick đầu cột bật / tắt mọi ô cùng tên (chọn tất cả đại lý khi chuyển giao)
  document.querySelectorAll("input[data-check-all]").forEach((all) => {
    const boxes = document.querySelectorAll('input[name="' + all.dataset.checkAll + '"]');
    all.addEventListener("change", () => boxes.forEach((box) => { box.checked = all.checked; }));
  });

  // Hiện / ẩn mật khẩu
  document.querySelectorAll("[data-toggle-password]").forEach((btn) => {
    const input = document.getElementById(btn.dataset.togglePassword);
    btn.addEventListener("click", () => {
      const show = input.type === "password";
      input.type = show ? "text" : "password";
      (btn.querySelector("[data-toggle-label]") || btn).textContent = show ? "Ẩn" : "Hiện";
      btn.setAttribute("aria-pressed", String(show));
    });
  });

  // Giữ phiên khi người dùng đang gõ / thao tác mà chưa gửi request nào, để không mất đơn đang nhập dở (S1-02).
  // Phiên hết hạn sau 30 phút không có request; ping 5 phút một lần nếu có thao tác.
  const ctx = document.body.dataset.ctx;
  if (app && ctx !== undefined) {
    let active = false;
    ["keydown", "pointerdown", "input"].forEach((type) =>
      document.addEventListener(type, () => { active = true; }, { passive: true }));

    const ping = () => fetch(ctx + "/session/ping", { credentials: "same-origin", cache: "no-store" })
      .then((res) => {
        if (res.status === 401) {
          showSessionExpired();
          return;
        }
        // Đã đăng nhập lại (có thể ở tab khác): cập nhật token CSRF cho các form để lưu được
        const token = res.headers.get("X-CSRF-Token");
        if (token) {
          document.querySelectorAll('input[name="_csrf"]').forEach((input) => { input.value = token; });
        }
        document.querySelectorAll("[data-session-expired]").forEach((el) => el.remove());
      });

    setInterval(() => {
      if (!active) {
        return;
      }
      active = false;
      ping().catch(() => { active = true; }); // mất mạng: thử lại ở lượt sau
    }, 5 * 60 * 1000);

    // Quay lại tab sau khi đăng nhập lại ở tab khác
    window.addEventListener("focus", () => {
      if (document.querySelector("[data-session-expired]")) {
        ping().catch(() => {});
      }
    });
  }

  function showSessionExpired() {
    const content = document.querySelector(".content");
    if (!content || content.querySelector("[data-session-expired]")) {
      return;
    }
    const alert = document.createElement("div");
    alert.className = "alert alert--warning";
    alert.setAttribute("role", "alert");
    alert.dataset.sessionExpired = "";
    alert.textContent = "Phiên đăng nhập đã hết hạn. Nội dung bạn đang nhập vẫn còn trên trang này — "
      + "hãy mở trang đăng nhập ở tab mới, đăng nhập lại rồi quay lại để lưu.";
    content.prepend(alert);
  }

  // Chính sách chiết khấu: chỉ hiện ô chọn sản phẩm hoặc nhóm hàng theo phạm vi đã chọn
  document.querySelectorAll("select[data-scope-switch]").forEach((select) => {
    const apply = () => document.querySelectorAll("[data-scope-field]").forEach((el) => {
      el.hidden = el.dataset.scopeField !== select.value;
    });
    select.addEventListener("change", apply);
    apply();
  });

  // Gợi ý khi gõ: ô có data-live-search="id-vùng-kết-quả" tải danh sách từ data-url của vùng đó sau khi ngừng gõ một chút.
  // Không có JavaScript thì vẫn dùng nút Tìm / Enter như thường.
  document.querySelectorAll("[data-live-search]").forEach((input) => {
    const box = document.getElementById(input.dataset.liveSearch);
    if (!box) {
      return;
    }
    let timer = null;
    let controller = null;
    input.addEventListener("input", () => {
      clearTimeout(timer);
      timer = setTimeout(async () => {
        const q = input.value.trim();
        if (controller) {
          controller.abort();
        }
        if (q === "") {
          box.innerHTML = "";
          return;
        }
        controller = new AbortController();
        try {
          const res = await fetch(box.dataset.url + "&q=" + encodeURIComponent(q), {
            credentials: "same-origin",
            signal: controller.signal,
          });
          if (res.redirected || !res.ok) {
            // hết phiên: tải lại để chuyển về trang đăng nhập; lỗi khác thì giữ nguyên kết quả cũ
            if (res.redirected) {
              location.reload();
            }
            return;
          }
          box.innerHTML = await res.text();
        } catch (e) {
          // bị huỷ vì đã gõ tiếp, hoặc mất mạng: bỏ qua
        }
      }, 250);
    });
  });

  // Mở / đóng <dialog>
  document.querySelectorAll("[data-dialog-open]").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.getElementById(btn.dataset.dialogOpen).showModal();
    });
  });

  document.querySelectorAll("[data-dialog-close]").forEach((btn) => {
    btn.addEventListener("click", () => btn.closest("dialog").close());
  });
});
