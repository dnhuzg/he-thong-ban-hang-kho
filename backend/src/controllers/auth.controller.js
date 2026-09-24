const bcrypt = require('bcryptjs')
const jwt = require('jsonwebtoken')
const pool = require('../config/database')

function createToken(user) {
  if (!process.env.JWT_SECRET) {
    throw new Error('JWT_SECRET is not configured')
  }

  return jwt.sign(
    {
      id: user.id,
      email: user.email,
      role: user.role,
    },
    process.env.JWT_SECRET,
    {
      expiresIn: process.env.JWT_EXPIRES_IN || '1d',
    }
  )
}

async function register(req, res) {
  try {
    const fullName = String(req.body.fullName || '').trim()
    const email = String(req.body.email || '').trim().toLowerCase()
    const password = String(req.body.password || '')

    if (!fullName || !email || !password) {
      return res.status(400).json({
        message: 'Vui lòng nhập đầy đủ họ tên, email và mật khẩu',
      })
    }

    if (password.length < 8) {
      return res.status(400).json({
        message: 'Mật khẩu phải có ít nhất 8 ký tự',
      })
    }

    const [existingUsers] = await pool.execute(
      'SELECT id FROM users WHERE email = ? LIMIT 1',
      [email]
    )

    if (existingUsers.length > 0) {
      return res.status(409).json({
        message: 'Email đã được sử dụng',
      })
    }

    const passwordHash = await bcrypt.hash(password, 12)

    const [result] = await pool.execute(
      `INSERT INTO users (full_name, email, password_hash, role)
       VALUES (?, ?, ?, 'user')`,
      [fullName, email, passwordHash]
    )

    const user = {
      id: result.insertId,
      fullName,
      email,
      role: 'user',
    }

    return res.status(201).json({
      message: 'Đăng ký thành công',
      token: createToken(user),
      user,
    })
  } catch (error) {
    console.error('Register error:', error)
    return res.status(500).json({
      message: 'Không thể đăng ký tài khoản',
    })
  }
}

async function authenticate(req, res, requiredRole = null) {
  try {
    const email = String(req.body.email || '').trim().toLowerCase()
    const password = String(req.body.password || '')

    if (!email || !password) {
      return res.status(400).json({
        message: 'Vui lòng nhập email và mật khẩu',
      })
    }

    const [users] = await pool.execute(
      `SELECT id, full_name, email, password_hash, role, status
       FROM users
       WHERE email = ?
       LIMIT 1`,
      [email]
    )

    const user = users[0]

    if (!user || !(await bcrypt.compare(password, user.password_hash))) {
      return res.status(401).json({
        message: 'Email hoặc mật khẩu không đúng',
      })
    }

    if (user.status !== 'active') {
      return res.status(403).json({
        message: 'Tài khoản đã bị khóa',
      })
    }

    if (requiredRole && user.role !== requiredRole) {
      return res.status(403).json({
        message: 'Tài khoản không có quyền quản trị',
      })
    }

    const publicUser = {
      id: user.id,
      fullName: user.full_name,
      email: user.email,
      role: user.role,
    }

    return res.json({
      message: 'Đăng nhập thành công',
      token: createToken(user),
      user: publicUser,
    })
  } catch (error) {
    console.error('Login error:', error)
    return res.status(500).json({
      message: 'Không thể đăng nhập',
    })
  }
}

function login(req, res) {
  return authenticate(req, res)
}

function adminLogin(req, res) {
  return authenticate(req, res, 'admin')
}

module.exports = {
  register,
  login,
  adminLogin,
}