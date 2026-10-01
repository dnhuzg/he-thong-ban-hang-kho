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
