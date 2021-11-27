const userHelper = require("../helpers/user");
const AES = require("../plugins/aes");
const crypto = require("crypto")
// res.local.xxx for passing additional data

// Check that object has each property in list
const objectHasProperties = (myObj, pList) => {
    if (typeof (myObj) !== "object") return false;
    for (let i = 0; i < pList.length; i++) {
        if (typeof (pList[i]) !== "string") return false;
        if (!myObj.hasOwnProperty(pList[i])) return false;
    }
    return true;
}

module.exports = {
    verifyMessage: async (req, res, next) => {
        const uuid = req.body.uuid;
        const encrypted_payload = req.body.e_payload;
        console.log(req.path);
        if (typeof (uuid) != "string" || typeof (encrypted_payload) != "string") {
            res.status(400).send({ "message": "Invalid parameters" });
            return;
        }

        // Check that user exists and is verified
        const user = await userHelper.findUserWithUuid(uuid);
        if (!user || !user.is_verified) {
            // Do not expose additional information
            console.log("No verified user matches uuid");
            res.status(400).send({ "message": "Invalid parameters" });
            return;
        }

        const payload_str = encrypted_payload;
        // const payload_str = AES.decrypt(result, process.env.REGISTRATION_IV, user.shared_secret);

        // Inspect payload structure
        let payload;
        try {
            payload = JSON.parse(payload_str);
            if (!objectHasProperties(payload, ["message", "hash"])) {
                throw "Missing message or hash";
            }
            if (!objectHasProperties(payload.message, ["uuid", "timestamp", "endpoint"])) {
                throw "Missing UUID, Timestamp, or Endpoint";
            }
        }
        catch (e) {
            console.log(e);
            res.status(400).send({ "message": "Invalid parameters" });
            return;
        }
        const message = payload.message;
        d = new Date();
        if (message.uuid !== uuid || d.getTime() - message.timestamp > 60) { // add check for path 
            console.log("UUID mismatch, or timestamp invalid");
            res.status(400).send({ "message": "Invalid parameters" });
            return;
        }

        const sha = crypto.createHash("sha256");
        sha.update(JSON.stringify(message));
        const hash = sha.digest("hex");
        if (hash !== payload.hash) {
            console.log("Hash mismatch!");
            console.log(hash);
            res.status(400).send({ "message": "Invalid parameters" });
            return;
        }
        next();
    }
}
/*
{
    "uuid":
    "e_payload": {
        "message": {
            "uuid":
            "timestamp":
            "endpoint":
            "param1":
            "param2":
            "paramN":
        }
        "hash": h(message contents)
    }
}
*/