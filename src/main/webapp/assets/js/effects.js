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
