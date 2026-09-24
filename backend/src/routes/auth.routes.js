const express = require('express')
const {
  register,
  login,
  adminLogin,
} = require('../controllers/auth.controller')

const router = express.Router()

router.post('/auth/register', register)
router.post('/auth/login', login)
router.post('/auth/admin/login', adminLogin)

module.exports = router