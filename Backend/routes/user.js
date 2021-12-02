const express = require('express');
const auth = require('../middleware/auth');
const router = express.Router();

const userController = require('../controllers/user');

// Todo add middleware
router.post('/initreg', userController.initRegistration);

router.post('/finreg', userController.finishRegistration);

router.post('/initgetkey', userController.initGetSessionKey);
router.post('/fingetkey', userController.finishGetSessionKey);
router.get('/keystatus', userController.isKeyExpired)

router.post('/test', auth.verifyMessage, (req, res) => { 
    console.log("SUCCESS");
    res.status(200).send({"message": "success"});
});

// Endpoints to update user's list numbers marked as "trusted" or "spam"
router.put('/trust', userController.markTrusted);

router.put('/spam', userController.markSpam);
router.delete('/trust', userController.removeTrusted);
router.delete('/spam', userController.removeSpam);

router.post('/known', userController.checkIfKnown);
router.post('/trust', userController.getTrustScore);


module.exports = router;