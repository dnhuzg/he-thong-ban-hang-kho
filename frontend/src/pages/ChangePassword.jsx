import { useState } from 'react';
import { changePassword } from '../api/authService';

function ChangePassword() {
  const [formData, setFormData] = useState({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [message, setMessage] = useState({ type: '', text: '' });
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage({ type: '', text: '' });

    // AC1: Bắt buộc nhập mật khẩu hiện tại
    if (!formData.oldPassword.trim()) {
      setMessage({ type: 'error', text: 'Vui lòng nhập mật khẩu hiện tại!' });
      return;
    }

    // AC2: Mật khẩu mới tối thiểu 8 ký tự, có cả chữ và số
    const hasLetter = /[a-zA-Z]/.test(formData.newPassword);
    const hasNumber = /[0-9]/.test(formData.newPassword);

    if (formData.newPassword.length < 8 || !hasLetter || !hasNumber) {
      setMessage({
        type: 'error',
        text: 'Mật khẩu mới phải từ 8 ký tự trở lên, bao gồm cả chữ cái và chữ số!',
      });
      return;
    }

    if (formData.newPassword !== formData.confirmPassword) {
      setMessage({ type: 'error', text: 'Mật khẩu xác nhận không trùng khớp!' });
      return;
    }

    try {
      setLoading(true);
      await changePassword({
        oldPassword: formData.oldPassword,
        newPassword: formData.newPassword,
      });

      // AC3: Đổi xong thông báo thu hồi các phiên đăng nhập khác
      setMessage({
        type: 'success',
        text: 'Đổi mật khẩu thành công! Tất cả các phiên đăng nhập khác đã được tự động đăng xuất.',
      });
      setFormData({ oldPassword: '', newPassword: '', confirmPassword: '' });
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.response?.data?.message || 'Lỗi khi đổi mật khẩu. Vui lòng kiểm tra lại mật khẩu cũ.',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '450px', margin: '40px auto', padding: '24px', border: '1px solid #e5e7eb', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
      <h2 style={{ marginTop: 0, marginBottom: '8px' }}>Đổi mật khẩu</h2>
      <p style={{ color: '#6b7280', fontSize: '14px', marginBottom: '20px' }}>
        Chủ động đổi mật khẩu để bảo vệ tài khoản sau khi được cấp mật khẩu tạm.
      </p>

      {message.text && (
        <div style={{ padding: '12px', marginBottom: '16px', borderRadius: '6px', fontSize: '14px', backgroundColor: message.type === 'error' ? '#fee2e2' : '#dcfce7', color: message.type === 'error' ? '#991b1b' : '#166534', border: `1px solid ${message.type === 'error' ? '#fca5a5' : '#86efac'}` }}>
          {message.text}
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div>
          <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px', fontSize: '14px' }}>
            Mật khẩu hiện tại <span style={{ color: 'red' }}>*</span>
          </label>
          <input
            type="password"
            name="oldPassword"
            value={formData.oldPassword}
            onChange={handleChange}
            required
            placeholder="Nhập mật khẩu đang dùng"
            style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ccc', boxSizing: 'border-box' }}
          />
        </div>

        <div>
          <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px', fontSize: '14px' }}>
            Mật khẩu mới <span style={{ color: 'red' }}>*</span>
          </label>
          <input
            type="password"
            name="newPassword"
            value={formData.newPassword}
            onChange={handleChange}
            required
            placeholder="Tối thiểu 8 ký tự, có chữ và số"
            style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ccc', boxSizing: 'border-box' }}
          />
        </div>

        <div>
          <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px', fontSize: '14px' }}>
            Nhập lại mật khẩu mới <span style={{ color: 'red' }}>*</span>
          </label>
          <input
            type="password"
            name="confirmPassword"
            value={formData.confirmPassword}
            onChange={handleChange}
            required
            placeholder="Xác nhận lại mật khẩu mới"
            style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ccc', boxSizing: 'border-box' }}
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          style={{ padding: '12px', backgroundColor: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 'bold', marginTop: '8px' }}
        >
          {loading ? 'Đang xử lý...' : 'Đổi mật khẩu'}
        </button>
      </form>
    </div>
  );
}

export default ChangePassword;