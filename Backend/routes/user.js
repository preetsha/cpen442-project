const express = require('express');
const auth = require('../middleware/auth');
const router = express.Router();

const userController = require('../controllers/user');

// Todo add middleware
router.get('/', userController.getUser);

router.post('/initreg', userController.initRegistration);

router.post('/finreg', userController.finishRegistration);

router.post('/initgetkey', userController.initGetSessionKey);

router.post('/fingetkey', userController.finishGetSessionKey);

router.post('/test', auth.verifyMessage, (req, res) => { 
    console.log("SUCCESS");
    res.status(200).send({"message": "success"});
});
/*
router.post('/request ', userController.request);

router.post('/verify', userController.verify);

router.post('/reset', userController.reset);
*/

module.exports = router;