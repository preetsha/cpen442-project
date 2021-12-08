const userHelper = require("../helpers/user");
const crypto = require("crypto");

// Check that object has each property in list
const objectHasProperties = (myObj, pList) => {
    if (typeof (myObj) !== "object") return false;
    for (let i = 0; i < pList.length; i++) {
        if (typeof (pList[i]) !== "string") return false;
        if (!myObj.hasOwnProperty(pList[i])) return false;
    }
    return true;
}

// Return true if uuid or payload are NOT strings
const argsAreBad = (res, uuid, e_payload) => {
    if (typeof (uuid) != "string" || typeof (e_payload) != "string") {
        // res.status(400).send({ "message": "Invalid parameters" });
		console.log("UUID or e_payload are not a string");
        res.status(400).send({ "message": "UUID or e_payload are not a string" });
        return true;
    }
    return false;
}

// Return true if there is no VERIFIED user that matches the uuid
const notVerifiedUser = (res, user) => {
    if (!user || user.account_status !== "verified") {
        // Do not expose additional information
        console.log("No verified user matches uuid");
        // res.status(400).send({ "message": "Invalid parameters" });
        res.status(400).send({ "message": "No verified user matching uuid" });
        return true;
    }
    return false;
}

// Attempt to parse and return the payload as a JSON, return null if error
const parsePayload = (res, payload_string) => {
    try {
        const payload = JSON.parse(payload_string);
        //console.log(payload)
        if (!objectHasProperties(payload, ["message", "hash"])) {
            throw "Missing message or hash";
        }
        if (!objectHasProperties(payload.message, ["uuid", "timestamp", "endpoint"])) {
            throw "Missing UUID, Timestamp, or Endpoint";
        }
        return payload
    }
    catch (e) {
        console.log(e);
        res.status(400).send({ "message": "Invalid parameters" });
        return null;
    }
}

// Return true if the message UUID matches the sender, the timestamp is
// recent, and the path is correct
const isMessageGood = (res, message, uuid) => {
    d = new Date();
    console.log(d.getTime())

    if (message.uuid !== uuid) {
        console.log("UUID mismatch");
        res.status(400).send({ "message": "Invalid parameters" });
        return false;
    }
    if (d.getTime() - message.timestamp > 600000) { // add check for path 
        console.log("Timestamp invalid");
        res.status(400).send({ "message": "Invalid parameters" });
        return false;
    }
    return true;
}

// Check that message hash matches the supplied hash (integrity check)
const doesHashMatch = (res, message, hash) => {
    const sha = crypto.createHash("sha256");
    sha.update(JSON.stringify(message));
    const message_hash = sha.digest("hex");
    if (message_hash !== hash) {
        console.log("Hash mismatch!");
        res.status(400).send({ "message": "Invalid parameters" });
        return false;
    }
    return true;
}

// Attempt to decrypt the payload
const decryptPayload = (res, encrypted_payload, key, key_encoding) => {
    try {
        const cipherBuffer = Buffer.from(encrypted_payload, "base64")
		console.log(key);
        const cipher = crypto.createDecipheriv("aes-192-ecb", Buffer.from(key, key_encoding), null);
        const payload_str = Buffer.concat([cipher.update(cipherBuffer), cipher.final()]).toString("utf8");
        return payload_str;
    }
    catch (e) {
        console.log(`Could not decrypt with supplied key:\n ${e}`);
        res.status(400).send({});
        return null;
    }
}

const verifyPayload = (req, res, uuid, encrypted_payload, key, key_encoding = "utf-8") => {
    const payload_str = decryptPayload(res, encrypted_payload, key, key_encoding);
    if (!payload_str) return false;

    // Attempt to parse the payload as a JSON object
    const payload = parsePayload(res, payload_str);
    if (!payload) return false;

    // Check the UUID, timestamp, and path all match
    const message = payload.message;
    if (!isMessageGood(res, message, uuid)) return false;


    // Check that the payload hash matches the message's SHA256 hash
    // if (!doesHashMatch(res, message, payload.hash)) return false; TODO: reenable later and fix pls

    res.locals.message = message;
    return true;
}

module.exports = {
    verifyReqWithShared: async (req, res, next) => {
        const uuid = req.body.uuid;
        const encrypted_payload = req.body.e_payload;

        // Validate the arguments
        if (argsAreBad(res, uuid, encrypted_payload)) return;

        // Check that the VERIFIED user exists
        const user = await userHelper.findUserWithUuid(uuid);
        if (notVerifiedUser(res, user)) return;

        const key = user.shared_secret;

        if (!verifyPayload(req, res, uuid, encrypted_payload, key)) return;

        // Allow execution of the actual endpoint
        next();
    },
    verifyReqWithSession: async (req, res, next) => {
        const uuid = req.body.uuid;
        const encrypted_payload = req.body.e_payload;

        // Validate the arguments
        if (argsAreBad(res, uuid, encrypted_payload)) return;

        // Check that the VERIFIED user exists
        const user = await userHelper.findUserWithUuid(uuid);
        if (notVerifiedUser(res, user)) return;

        const key = user.session_key;
        
        if (!verifyPayload(req, res, uuid, encrypted_payload, key)) return;

        // Allow execution of the actual endpoint
        next();
    }
}
