import axiosClient from './axiosClient';

// Hàm xử lý Đăng xuất an toàn
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