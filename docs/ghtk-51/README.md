# GHTK-51 · Kho và vị trí lưu trữ

Trang `/inventory/warehouses` đọc và ghi MySQL qua `WarehousePageServlet -> WarehouseAdminService -> WarehouseAdminDao`. Hỗ trợ thêm/sửa kho, khu/kệ và gán kho phục vụ đại lý; mọi thay đổi có audit log. Cần chạy migration `030_kho_va_vi_tri.sql`.
