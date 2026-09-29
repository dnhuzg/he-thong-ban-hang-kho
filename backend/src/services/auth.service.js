const bcrypt = require('bcryptjs')
const jwt = require('jsonwebtoken')
const pool = require('../config/database')
const AppError = require('../errors/app-error')

const INVALID_CREDENTIALS = 'Tên đăng nhập hoặc mật khẩu không chính xác.'
const LOCKOUT_LIMIT = 5

function createAuthService({ database = pool, getSecret = () => process.env.JWT_SECRET } = {}) {
  async function login(identifier, password) {
    const secret = getSecret()
    if (typeof secret !== 'string' || secret.length < 32) {
      throw new AppError(503, 'Dịch vụ đăng nhập chưa được cấu hình.', 'AUTH_NOT_CONFIGURED')
    }

    const connection = await database.getConnection()
    let transactionOpen = false

    try {
      await connection.beginTransaction()
      transactionOpen = true

      const [users] = await connection.execute(
        `SELECT id, username, email, password_hash, is_active,
                failed_login_attempts, locked_until,
                (locked_until IS NOT NULL AND locked_until > CURRENT_TIMESTAMP(3)) AS is_locked
         FROM users
         WHERE username = ? OR email = ?
         LIMIT 1
         FOR UPDATE`,
        [identifier, identifier],
      )
      const user = users[0]

      if (!user) {
        await connection.rollback()
        transactionOpen = false
        throw new AppError(401, INVALID_CREDENTIALS, 'INVALID_CREDENTIALS')
      }

      if (Number(user.is_locked) === 1) {
        await connection.rollback()
        transactionOpen = false
        throw new AppError(401, INVALID_CREDENTIALS, 'INVALID_CREDENTIALS')
      }

      let failedAttempts = Number(user.failed_login_attempts) || 0
      if (user.locked_until) {
        failedAttempts = 0
        await connection.execute(
          'UPDATE users SET failed_login_attempts = 0, locked_until = NULL WHERE id = ?',
          [user.id],
        )
      }

      const passwordMatches = await bcrypt.compare(password, user.password_hash)
      const accountIsActive = user.is_active === true || Number(user.is_active) === 1

      if (!passwordMatches || !accountIsActive) {
        const nextAttempts = failedAttempts + 1
        await connection.execute(
          `UPDATE users
           SET failed_login_attempts = ?,
               locked_until = CASE
                 WHEN ? >= ${LOCKOUT_LIMIT} THEN DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 15 MINUTE)
                 ELSE NULL
               END
           WHERE id = ?`,
          [nextAttempts, nextAttempts, user.id],
        )
        await connection.commit()
        transactionOpen = false
        throw new AppError(401, INVALID_CREDENTIALS, 'INVALID_CREDENTIALS')
      }

      const [roles] = await connection.execute(
        `SELECT r.code, r.name
         FROM roles r
         INNER JOIN user_roles ur ON ur.role_id = r.id
         WHERE ur.user_id = ?
         ORDER BY r.name`,
        [user.id],
      )
      const [permissions] = await connection.execute(
        `SELECT DISTINCT p.code
         FROM permissions p
         INNER JOIN role_permissions rp ON rp.permission_id = p.id
         INNER JOIN user_roles ur ON ur.role_id = rp.role_id
         WHERE ur.user_id = ?
         ORDER BY p.code`,
        [user.id],
      )

      const userResponse = {
        id: String(user.id),
        username: user.username,
        email: user.email,
        roles,
        permissions: permissions.map((permission) => permission.code),
      }
      const token = jwt.sign(
        { sub: userResponse.id },
        secret,
        {
          expiresIn: process.env.JWT_EXPIRES_IN || '15m',
          issuer: 'he-thong-ban-hang-kho',
          audience: 'he-thong-ban-hang-kho',
        },
      )

      await connection.execute(
        `UPDATE users
         SET failed_login_attempts = 0, locked_until = NULL, last_login_at = CURRENT_TIMESTAMP(3)
         WHERE id = ?`,
        [user.id],
      )
      await connection.commit()
      transactionOpen = false

      return { token, user: userResponse }
    } catch (error) {
      if (transactionOpen) {
        await connection.rollback()
      }
      throw error
    } finally {
      connection.release()
    }
  }

  return { login }
}

module.exports = { createAuthService }