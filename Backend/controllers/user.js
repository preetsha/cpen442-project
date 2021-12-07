const User = require("../models/user");
const UserHelper = require("../helpers/user.js");
const crypto = require("crypto");
// const AES = require("../plugins/aes");
var CryptoJS = require("crypto-js");
const TwilioHelper = require("../plugins/sms");
const { createUnverifiedUser } = require("../helpers/user.js");

// Add phone number to trust/spam list
const addToPhoneList = async (user, phone, list_type) => {
    if (["trust", "spam"].indexOf(list_type) == -1) throw "Invalid List!";

    const list = (list_type === "trust") ? user.trusted_numbers : user.spam_numbers;
    if (!list.includes(phone)) {
        list.push(phone);
    }
    await user.save();
}
// Remove phone number from trust/spam list
const removeFromPhoneList = async (user, phone, list_type) => {
    if (["trust", "spam"].indexOf(list_type) == -1) throw "Invalid List!";

    const list = (list_type === "trust") ? user.trusted_numbers : user.spam_numbers;
    const index = list.indexOf(phone);
    if (index > -1) {
        list.splice(index, 1);
    }
    await user.save();
}

// Update trust/spam with phone number accordingly
const updatePhoneLists = async (req, res, command) => {
    // Use UUID to find the user
    const uuid = String(req.body.uuid);
    const phone = String(req.body.phone);
    
    // Check user exists
    const user = await UserHelper.findUserWithUuid(uuid);
    if (!user) { res.status(400).send({}); return; }

    // Encrypt the phone #
    // const encryptedPhone = phone; // TODO encrypt
    const encryptedPhone = CryptoJS.AES.encrypt(phone, process.env.KEY).toString();
    // const decryptedPhone = CryptoJS.AES.decrypt(encryptedPhone, process.env.KEY).toString(CryptoJS.enc.Utf8);

    // Update lists
    switch (command) {
        case "trust": {
            await UserHelper.createNonUserNumber(encryptedPhone);
            await removeFromPhoneList(user, encryptedPhone, "spam");
            await addToPhoneList(user, encryptedPhone, "trust");
            res.status(200).send({"message": `Marked ${phone} as trusted`});
            break;
        }
        case "spam": {
            await UserHelper.createNonUserNumber(encryptedPhone);
            await removeFromPhoneList(user, encryptedPhone, "trust");
            await addToPhoneList(user, encryptedPhone, "spam");
            res.status(200).send({"message": `Marked ${phone} as spam`});
            break;
        }
        case "rmtrust": {
            await removeFromPhoneList(user, encryptedPhone, "trust");
            res.status(200).send({"message": `Removed ${phone} from trusted`});
            break;
        }
        case "rmspam": {
            await removeFromPhoneList(user, encryptedPhone, "spam");
            res.status(200).send({"message": `Removed ${phone} from spam`});
            break;
        }
        default: throw "Invalid command!";
    }
}

// Get a list of OTHER users that trust the ORIGINAL user
const getBidirectionalTrusts = async (originalUser, cachedUsers) => {
    // For each number on the user's trusted list
    const userNumber = originalUser.phone;
    const bidirectionalTrust = []

    if (originalUser.trusted_numbers.length == 0) return bidirectionalTrust;

    for (const trustedNumber of originalUser.trusted_numbers) {
        // Get that user obj
        const trustedUser = await getUserWithNumberCached(trustedNumber, cachedUsers);
        
        if (trustedUser.account_status == "verified" && trustedUser.trusted_numbers.includes(userNumber)) {
            bidirectionalTrust.push(trustedUser.phone);
        }
    }
    return bidirectionalTrust;
}

const getUserWithNumberCached = async (phoneNumber, cachedUsers) => {
    // Attempt to retrive user from cache
    if (phoneNumber in cachedUsers) {
        return cachedUsers[phoneNumber];
    }
    // Add user to cache
    const user = await UserHelper.findUserWithEncPhone(phoneNumber);
    cachedUsers[phoneNumber] = user;

    return user;
}


const calculateTrustScore = async (userMainNumber, userOtherNumber, numLayers, cachedUsers, initialUser) => {
    /*
    Calculates the trust score for all the users in the trusted/spam list with relation to userOther and calls recursive call with num_layers - 1
    */
    const POSITIVE_EDGE = 1;
    const NEGATIVE_EDGE = -2;
    
    let trustScore = 0
    let multiplier;
    switch (numLayers) {
        case 3: multiplier = 3; break;
        case 2: multiplier = 2; break;
        default: multiplier = 1;
    }
    
    const thisUser = await getUserWithNumberCached(userMainNumber, cachedUsers);
    if (thisUser.trusted_numbers.includes(userOtherNumber)) {
        trustScore += POSITIVE_EDGE * multiplier;
    }
    else if (thisUser.spam_numbers.includes(userOtherNumber)) {
        trustScore += NEGATIVE_EDGE * multiplier;
    }

    if (numLayers == 1) return trustScore;

    // For each trusted number that also trusts the user back
    let biTrustList = await getBidirectionalTrusts(thisUser, cachedUsers); // Note: Pass in user object
    for (const trustedNumber of biTrustList) {
        if (trustedNumber != userOtherNumber && trustedNumber != initialUser) {
            trustScore += await calculateTrustScore(trustedNumber, userOtherNumber, numLayers-1, cachedUsers, initialUser)
        }
    }
    return trustScore;  
}


/*
    Main call:
    calculateTrustScore(userMain, userOther, 3);
*/

module.exports = {
    checkIfKnown: async (req, res) => {
        // Validate phone input
		
        const otherPersonPhones = req.body.phones;
		
		messageArray = [];
		
		const thisUser = await UserHelper.findUserWithUuid(req.body.uuid);
		
		if (!Array.isArray(otherPersonPhones)) {
			res.status(400).send({});
		}
		
		for (let otherPersonPhone of otherPersonPhones) {
            // const encryptedOtherPersonPhone = otherPersonPhone; // Todo encrypt
            const encryptedOtherPersonPhone = CryptoJS.AES.encrypt(otherPersonPhone, process.env.KEY).toString(CryptoJS.enc.Utf8);
			
			if (thisUser.trusted_numbers.includes(encryptedOtherPersonPhone)) {
				messageArray.push("TRUSTED");
			}
			else if (thisUser.spam_numbers.includes(encryptedOtherPersonPhone)) {
				messageArray.push("SPAM");
			}
			else {
				messageArray.push("UNKNOWN");
			}
		}
		
	
        res.status(200).send({"known": messageArray });
    },
    getTrustScore: async (req, res) => {
        const thisUser = await UserHelper.findUserWithUuid(req.body.uuid);
        if (!thisUser) {
            console.log("User matching uuid does not exist");
            res.status(400).send({});
            return;
        }
        // Validate phone input
        const otherPersonPhone = req.body.phone;

        // const encryptedOtherPersonPhone = otherPersonPhone; // Todo encrypt
        const encryptedOtherPersonPhone = CryptoJS.AES.encrypt(otherPersonPhone, process.env.KEY).toString(CryptoJS.enc.Utf8);

        if (thisUser.trusted_numbers.includes(encryptedOtherPersonPhone)) {
            res.status(200).send({ "score": 0, "message": "TRUSTED" });
            return;
        }
        else if (thisUser.spam_numbers.includes(encryptedOtherPersonPhone)) {
            res.status(200).send({ "score": -1, "message": "SPAM" });
            return;
        }

        const otherUser = await UserHelper.findUserWithEncPhone(encryptedOtherPersonPhone);
        if (!otherUser) {
            otherUser = await UserHelper.createNonUserNumber(encryptedOtherPersonPhone);
            res.status(200).send({ "score": 0 });
            return;
        }

        const thisUserPhone = thisUser.phone;
        const cachedUsers = { "thisUserPhone": thisUser } // Create dictionary to cache the users

        let trustScore = 0;
        const biTrustedUserNumbers = await getBidirectionalTrusts(thisUser, cachedUsers);
        for (const trustedNumber of biTrustedUserNumbers) {
            trustScore += await calculateTrustScore(trustedNumber, encryptedOtherPersonPhone, 3, cachedUsers, networkActivity, thisUserPhone)
        }

        // Adjust trust score based on how many NEW users this number has contacted in the last hour
        const networkActivity = Math.floor(otherUser.detected_recent_messages / 20);
        trustScore -= networkActivity;
        
        res.status(200).send({ "score": trustScore });
    },
    temp: async (req, res) => {
        res.send(await UserHelper.createNonUserNumber(req.body.phone))
    },
    initRegistration: async (req, res) => {
        const phoneNumber = req.body.phone_number;
        const paddedPhone = ("0".repeat(16) + phoneNumber).slice(-16); // Pad phone number to 16 chars for encryption

        // Encrypt the phone number using the phone symmetric key
        // const encryptedPhone = phoneNumber;
        const encryptedPhone = CryptoJS.AES.encrypt(phoneNumber, process.env.KEY).toString(CryptoJS.enc.Utf8);

        let myUser = await UserHelper.findUserWithEncPhone(encryptedPhone);
        if (!myUser) {
            myUser = await UserHelper.createUnverifiedUser(encryptedPhone);
        }
        else if (myUser.account_status == "inactive") {
            myUser = await UserHelper.convertNonUserToUnverified(encryptedPhone);
        }
        else if (myUser.account_status == "verified") {
            myUser = await UserHelper.beginVerifyingAgain(encryptedPhone);
        }
        else { (myUser.account_status == "unverified")
            // Do nothing?
        }

        if (!myUser) { res.status(400).send({}); return; }
        // time requested, expected nonce, and retries are saved
        const nonce = myUser.nonce_expected;
        TwilioHelper.sendSMS(phoneNumber, `Your TrustSMS verification code is: \n${nonce}`)

        res.status(200).send({ 
            "message": `Send one-time-pass to phone.`,
        });
    },
    finishRegistration: async (req, res) => {
        // Decrypt the message using the server's private key
        const unencryptedReq = req;
        
        const oneTimePass = unencryptedReq.body.one_time_pass;
        const sharedSecret = unencryptedReq.body.shared_secret;
        const phoneNumber = unencryptedReq.body.phone_number;

        // Encrypt the phone number using the phone symmetric key
        // Phone # padded to 16 characters
        // const encryptedPhone = phoneNumber;
        const encryptedPhone = CryptoJS.AES.encrypt(phoneNumber, process.env.KEY).toString(CryptoJS.enc.Utf8);
        
        
        // Attempt to very the user corresponding to the phone number
        const user = await UserHelper.verifyUser(encryptedPhone, oneTimePass, sharedSecret);

        if (user === "ABORT") {
            res.status(400).send({"message": "Too many incorrect attempts."});
            return;
        }
        if (!user) {
            res.status(400).send({});
            return;
        }

        const unencryptedResponse = { 
            phone: phoneNumber, 
            salt: user.salt,
        }

        // Encrypt and send the response object
        // const paddedSharedSecret = (sharedSecret + "0".repeat(32)).slice(0, 32);
        // const encryptedResponse = unencryptedResponse;
        const encryptedResponse = CryptoJS.AES.encrypt(JSON.stringify(unencryptedResponse), sharedSecret).toString(CryptoJS.enc.Utf8);

        res.status(201).send(encryptedResponse);
    },
    initGetSessionKey: async (req, res) => {
        const uuid = req.body.uuid;
        const rA = req.body.nonce;
        const inPayload = req.body.payload;
        
        // Find the user using their UUID
        const user = await User.findOne({ uuid: uuid }).catch(() => null);

        // Stop if we cannot match uuid with a user
        if (!user) {
            console.log(`No user matching UUID: ${uuid.slice(0, 32)}`);
            res.status(404).send({});
            return;
        }

        // Take their shared secret, decrypt the payload
        const sharedSecret = user.shared_secret
        // const uInPayload = JSON.parse(inPayload); // Remove this, uncomment below
        const uInPayload = JSON.parse(CryptoJS.AES.decrypt(inPayload, sharedSecret).toString(CryptoJS.enc.Utf8))        

        // Stop if uuid in payload does not match sender
        if (uuid != uInPayload.uuid) {
            console.log(`UUID mismatch in payload.`);
            res.status(401).send({});
        }
        
        const g = 5
        const p = 23

        const b = crypto.randomInt(g, p-2);

        const gaModP = Number("0x" + uInPayload.keyhalf);
        const gbModP = (BigInt(g) ** BigInt(b)) % BigInt(p);

        const sessionKey = (BigInt(gaModP) ** BigInt(b)) % BigInt(p);
        user.session_key = "0x" + sessionKey.toString(16);
        // Create and encrypt payload containing g^b mod p and R_A
        const uOutPayload = JSON.stringify({
            "nonce": rA,
            "keyhalf": "0x" + gbModP.toString(16)
        });
        
        const outPayload = uOutPayload; // Remove this, uncomment below
        // const outPayload = AES.encrypt(uOutPayload, process.env.DIFFIE_IV, sharedSecret);

        // Add expected R_B to user obj
        const rB = crypto.randomInt(1, 10000);
        user.nonce_expected = rB;
        
        await user.save();

        res.status(200).send({
            "nonce": rB,
            "payload": outPayload
        });
    },
    finishGetSessionKey: async (req, res) => {
        const uuid = req.body.uuid;
        const inPayload = req.body.payload;
        
        // Find the user using their UUID
        const user = await UserHelper.findUserWithUuid(uuid);

        // Stop if we cannot match uuid with a user
        if (!user) {
            console.log(`No user matching UUID: ${uuid.slice(0, 32)}`);
            res.status(404).send({});
            return;
        }

        // Decrypt response
        const uInPayload = JSON.parse(inPayload); // Todo do decryption

        // Stop if uuid in payload does not match sender
        if (uuid != uInPayload.uuid) {
            console.log(`UUID mismatch in payload.`);
            res.status(401).send({});
            return;
        }
        
        // Check R_B
        if (String(user.nonce_expected) != uInPayload.nonce) {
            console.log(`Nonce mismatch!`);
            res.status(401).send({});
            return;
        }

        // Update User obj
        d = new Date();
        user.session_key_last_established = d.getTime();
        await user.save();

        // Send response
        res.status(201).send({ "message": "Session key has been established"});
    },
    isKeyExpired: async (req, res) => {
        const uuid = req.body.uuid;
        const inPayload = req.body.payload;
        
        // Find the user using their UUID
        const user = await UserHelper.findUserWithUuid(uuid);

        // Stop if we cannot match uuid with a user
        if (!user) {
            console.log(`No user matching UUID: ${uuid.slice(0, 32)}`);
            res.status(404).send({});
            return;
        }
        
        d = new Date();
        const lastEstablished = user.session_key_last_established;
        const elapsed = d.getTime() - lastEstablished

        if (!lastEstablished || elapsed > 900000) { // 15 mins
            res.status(200).send({ "is_expired": true });
        }
        else {
            res.status(200).send({ "is_expired": false });
        }
    },
    markTrusted: async (req, res) => {
        updatePhoneLists(req, res, "trust");
    },
    markSpam: async (req, res) => {
        updatePhoneLists(req, res, "spam");
    },
    removeTrusted: async (req, res) => {
        updatePhoneLists(req, res, "rmtrust");
    },
    removeSpam: async (req, res) => {
        updatePhoneLists(req, res, "rmspam");
    },
}