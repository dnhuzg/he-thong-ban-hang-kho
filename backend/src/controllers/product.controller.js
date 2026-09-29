const pool = require('../config/database')

const getProducts = async (req, res, next) => {
  try {
    const [products] = await pool.query('SELECT * FROM products')

    res.status(200).json(products)
  } catch (error) {
    next(error)
  }
}

module.exports = { getProducts }
