const express = require('express');
const router = express.Router();

const userController = require('../controllers/user');

// Todo add middleware
router.get('/', userController.getUser);

router.post('/', userController.createUser);

module.exports = router;