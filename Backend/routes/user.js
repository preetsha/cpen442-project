const express = require('express');
const auth = require('../middleware/auth');
const router = express.Router();;
const crypto = require("crypto");
const AES = require("../plugins/aes");

const userController = require('../controllers/user');
const userHelper = require('../helpers/user');

// Todo add middleware
router.post('/initreg', userController.initRegistration);

router.post('/finreg', userController.finishRegistration);

router.post('/initgetkey', auth.verifyReqWithShared, userController.initGetSessionKey);
router.post('/fingetkey', auth.verifyReqWithShared, userController.finishGetSessionKey);
router.get('/keystatus', auth.verifyReqWithShared, userController.isKeyExpired)

router.post('/test', auth.verifyReqWithShared, (req, res) => {
    console.log("SUCCESS")
    res.status(200).send({ "message": "SUCCESS" });
});
router.post('/testEncrypt', (req, res) => {
    const json = {
        "message": "test1",
        "okay": "now this is epic"
    }
    const encrypted = AES.encryptJSON(json, "3OIHaGC8QjAxfBwCCBo+3w==");
    console.log(encrypted)
    res.status(200).send({ "message": encrypted });
});
// Pass a message you want formatted, timestamp will be updated, hash will be added,
// and the payload will be encrypted. DELETE THIS AFTER DEVELOPMENT
router.post('/debug', async (req, res) => {
    d = new Date()
    const message = req.body.message;
    
    const user = await userHelper.findUserWithUuid(message.uuid);
    if (!user) {
        res.send({"message": "No user matches the supplied uuid"});
        return;
    }

    message.timestamp = d.getTime();

    const sha = crypto.createHash("sha256");
    sha.update(JSON.stringify(message));
    const hash = sha.digest("hex");

    const payload = {
        "message": message, 
        "hash": hash
    }

    const payloadBuffer = Buffer.from(JSON.stringify(payload), "utf-8")
    // const key = "H+UYf8Cajus6doDsHry+BQ=="
    const key = user.shared_secret;
    const cipher = crypto.createCipheriv("aes-192-ecb", key, null);
    const payload_str = Buffer.concat([cipher.update(payloadBuffer), cipher.final()]).toString("base64");

    console.log(payload_str);
    res.send({ "message": payload_str })
})

// Endpoints to update user's list numbers marked as "trusted" or "spam"
router.put('/trust', auth.verifyReqWithSession, userController.markTrusted);

router.put('/spam', auth.verifyReqWithSession, userController.markSpam);
router.delete('/trust', auth.verifyReqWithSession, userController.removeTrusted);
router.delete('/spam', auth.verifyReqWithSession, userController.removeSpam);

router.get('/known', auth.verifyReqWithSession, userController.checkIfKnown);
router.get('/trust', auth.verifyReqWithSession, userController.getTrustScore);


module.exports = router;