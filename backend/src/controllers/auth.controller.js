const AppError = require('../errors/app-error')
const { createAuthService } = require('../services/auth.service')

const authService = createAuthService()

async function login(req, res) {
  const identifier = req.body?.identifier
  const password = req.body?.password

  if (
    typeof identifier !== 'string' ||
    identifier.trim().length < 1 ||
    identifier.trim().length > 255 ||
    typeof password !== 'string' ||
    password.length < 1 ||
    password.length > 128
  ) {
    throw new AppError(400, 'Vui lòng nhập tài khoản và mật khẩu hợp lệ.', 'VALIDATION_ERROR')
  }

  const result = await authService.login(identifier.trim(), password)
  res.status(200).json(result)
}

module.exports = { login }