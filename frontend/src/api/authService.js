import axiosClient from './axiosClient';

// 1. Hàm xử lý Đăng xuất an toàn (GHTK-3)
export const logout = async () => {
  try {
    // Gọi API xóa session/cookie ở Server
    await axiosClient.post('/auth/logout');
  } catch (error) {
    console.error('Lỗi khi đăng xuất ở Server:', error);
  } finally {
    // Xóa Token ở Client bất kể API có thành công hay không
    localStorage.removeItem('accessToken');

    // Điều hướng về trang Đăng nhập
    window.location.href = '/login';
  }
};

// 2. Hàm xử lý Đổi mật khẩu khi đang đăng nhập (GHTK-13)
export const changePassword = async ({ oldPassword, newPassword }) => {
  const response = await axiosClient.post('/auth/change-password', {
    oldPassword,
    newPassword,
  });
  return response.data;
};