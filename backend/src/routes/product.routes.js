const express = require('express')
const {
  getProducts,
  createProduct
} = require('../controllers/product.controller')

const router = express.Router()

router.get('/products', getProducts)

router.post('/products', createProduct)

module.exports = router
