const express = require('express');
const router = express.Router();

const userController = require('../controllers/user');

// Todo add middleware
router.get('/', userController.getUser);

router.post('/', userController.createUser);

router.post('/initreg', userController.requestRegistration);

router.post('/finreg', userController.finishRegistration);

/*
router.post('/request ', userController.request);

router.post('/verify', userController.verify);

router.post('/reset', userController.reset);
*/

module.exports = router;