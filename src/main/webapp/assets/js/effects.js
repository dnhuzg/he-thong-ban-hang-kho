// Hiệu ứng giao diện (ngoài chức năng). Thêm hiệu ứng mới vào CUỐI file này, mỗi hiệu ứng một khối có chú thích;
// không chứa logic nghiệp vụ. Tải sau app.js ở mọi trang.

document.addEventListener("DOMContentLoaded", () => {
  // Nút chuyển giao diện sáng / tối (trang trước đăng nhập); giao diện ban đầu do script ở đầu trang đặt
  const themeToggle = document.getElementById("theme-toggle");
  if (themeToggle) {
    themeToggle.checked = document.documentElement.dataset.theme === "light";
    themeToggle.addEventListener("change", () => {
      const theme = themeToggle.checked ? "light" : "dark";
      document.documentElement.setAttribute("data-theme", theme);
      try {
        localStorage.setItem("oms-theme", theme);
      } catch (e) {
        // trình duyệt chặn lưu trữ: vẫn đổi được trong lần xem này
      }
    });
  }
});

/*
 * Màn chờ khi gửi form hoặc chuyển trang. Chỉ hiện nếu trang chậm hơn khoảng 0,3 giây; khoá nút vừa bấm để không
 * gửi trùng. Bỏ qua form / liên kết đã có JavaScript riêng xử lý (preventDefault), mở tab mới, tải tệp
 * (thuộc tính download hoặc data-no-loading) và liên kết trong cùng trang (#...).
 */
(() => {
  const DELAY = 300;
  // Phòng khi trình duyệt không rời trang (ví dụ phản hồi là tệp tải về): tự tắt để không khoá màn hình mãi
  const GIVE_UP = 15000;
  let showTimer = null;
  let giveUpTimer = null;
  let veil = null;
  let lockedButtons = [];

  const hide = () => {
    clearTimeout(showTimer);
    clearTimeout(giveUpTimer);
    if (veil) {
      veil.remove();
      veil = null;
    }
    lockedButtons.forEach((btn) => {
      btn.disabled = false;
      btn.removeAttribute("aria-busy");
    });
    lockedButtons = [];
  };

  const show = () => {
    clearTimeout(showTimer);
    clearTimeout(giveUpTimer);
    showTimer = setTimeout(() => {
      veil = document.createElement("div");
      veil.className = "page-loading";
      veil.setAttribute("role", "status");
      veil.setAttribute("aria-label", "Đang tải");
      veil.innerHTML =
        '<div class="newtons-cradle" aria-hidden="true">' +
        '<div class="newtons-cradle__dot"></div><div class="newtons-cradle__dot"></div>' +
        '<div class="newtons-cradle__dot"></div><div class="newtons-cradle__dot"></div></div>';
      document.body.appendChild(veil);
    }, DELAY);
    giveUpTimer = setTimeout(hide, GIVE_UP);
  };

  document.addEventListener("submit", (event) => {
    const form = event.target;
    const submitter = event.submitter;
    // Đợi các xử lý submit khác chạy xong mới biết form có thật sự được gửi đi không
    setTimeout(() => {
      if (event.defaultPrevented || form.method === "dialog" || form.target === "_blank" || form.hasAttribute("data-no-loading")) {
        return;
      }
      // Khoá sau khi trình duyệt đã lấy dữ liệu form, để giá trị của nút bấm vẫn được gửi đi
      if (submitter && !submitter.disabled) {
        submitter.disabled = true;
        submitter.setAttribute("aria-busy", "true");
        lockedButtons.push(submitter);
      }
      show();
    }, 0);
  });

  document.addEventListener("click", (event) => {
    const link = event.target.closest("a[href]");
    if (!link || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
      return;
    }
    setTimeout(() => {
      if (event.defaultPrevented || link.target === "_blank" || link.hasAttribute("download") || link.hasAttribute("data-no-loading")) {
        return;
      }
      const url = new URL(link.href, location.href);
      const samePage = url.pathname === location.pathname && url.search === location.search && url.hash !== "";
      if (url.origin !== location.origin || samePage) {
        return;
      }
      show();
    }, 0);
  });

  // Quay lại bằng nút Back (trang lấy từ bộ nhớ đệm) thì tắt màn chờ còn sót
  window.addEventListener("pageshow", hide);
})();
