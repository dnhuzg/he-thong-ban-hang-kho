const pool = require('../config/database')

const getProducts = async (req, res, next) => {
  try {
    const [products] = await pool.query('SELECT * FROM products')

    res.status(200).json(products)
  } catch (error) {
    next(error)
  }
}

const createProduct = async (req, res, next) => {
  try {
    const {
      sku,
      name,
      category_id,
      base_unit,
      packaging,
      cost_price,
      image,
      status
    } = req.body

    const [result] = await pool.query(
      `INSERT INTO products
      (sku, name, category_id, base_unit, packaging, cost_price, image, status)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        sku,
        name,
        category_id,
        base_unit,
        packaging,
        cost_price,
        image,
        status
      ]
    )

    res.status(201).json({
      message: 'Thêm sản phẩm thành công',
      id: result.insertId
    })
  } catch (error) {
    next(error)
  }
}

module.exports = {
  getProducts,
  createProduct
}
