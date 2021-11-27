const User = require("../models/user");
const UserHelper = require("../helpers/user.js");
const crypto = require("crypto");
const AES = require("../plugins/aes");

module.exports = {
    // This is just an example, we do not want to keep this function in the final implementation
    getUser: async (req, res) => {
        try {
            let myUser = await UserHelper.findUserWithUuid(req.body.uuid);

            if (myUser) {
                res.status(200).send(myUser);
            }
            else {
                res.status(404).send({ "message": "User does not exist" });
            }
        }
        catch (err) {
            console.log(err);
            res.status(400).send({ "message": "Invalid parameters" });
        }
    },
    initRegistration: async (req, res) => {
        const phoneNumber = req.body.phone_number;
        const paddedPhone = ("0".repeat(16) + phoneNumber).slice(-16); // Pad phone number to 16 chars for encryption

        // Encrypt the phone number using the phone symmetric key
        const encryptedPhone = AES.encrypt(paddedPhone, process.env.PHONE_IV, process.env.KEY);

        // Compute hash of uuid and salt, picking a salt that allows for no hash collisions
        let salt = crypto.randomInt(1000000000);
        let uuid = "";
        
        // Compute hash of phone number and salt, which is used as the UUID
        do {
            // NOTE: The invariant is that all UUID's will be unique
            salt = (salt + 1) % 1000000000;
            let sha = crypto.createHmac('sha256', String(salt));
            
            sha.update(phoneNumber);
            uuid = sha.digest("hex");

            // Check if the UUID is already in use
            let uuidUser = await UserHelper.findUserWithUuid(req.body.uuid)

        } // Pick a new salt until we get a unique UUID
        while (uuidUser);

        // Generate nonce
        // const oneTimePass = crypto.randomInt(0, 1000000) // 6 digit number
        const oneTimePass = 101234 // 6 digit number
        const oneTimePassString = oneTimePass.toString().padStart(6, "0");
        
        // TODO Send SMS message
        console.log(`Call F(x) to send SMS message: ${oneTimePassString}`);
        
        // Create unverified user
        const d = new Date();
        let newUser = {
            uuid: uuid, // hash of phone number + salt
            phone: encryptedPhone,
            salt: salt,
            shared_secret: null,
            session_key: null, 
            session_key_last_established: null,
            time_requested_verification: d.getTime(),
            expected_nonce: oneTimePassString,
            time_completed_verification: null,
            is_active_user: true,
            is_verified: false,
        }
        await User.create(newUser);

        res.status(201).send({ 
            "message": `TODO Call function to send ${oneTimePassString} to ${phoneNumber}`,
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
        const paddedPhone = ("0".repeat(16) + phoneNumber).slice(-16); 
        const encryptedPhone = AES.encrypt(paddedPhone, process.env.PHONE_IV, process.env.KEY);
        
        // Find user corresponding to the phone number
        const user = await findUserWithEncPhone(encryptedPhone);

        if (user) {
            // Confirm the code matches
            if (user.expected_nonce != oneTimePass) {
                console.log("Nonce value mismatch!");
                res.status(401).send({});
                return;
            }
            // Extracting and saving the shared secret
            user.shared_secret = sharedSecret;

            // Update verif complete time, set verified, set is_active_user
            d = new Date();
            user.is_verified = true;
            user.time_completed_verification = d.getTime();
            await user.save();

            const unencryptedResponse = { 
                phone: phoneNumber, 
                salt: user.salt,
            }

            // Encrypt and send the response object
            const paddedSharedSecret = (sharedSecret + "0".repeat(32)).slice(0, 32);
            const encryptedResponse = AES.encrypt(JSON.stringify(unencryptedResponse), process.env.REGISTRATION_IV, paddedSharedSecret);
            res.status(201).send(unencryptedResponse); // TODO Send back encryptedResponse
        }
        else {
            res.status(404).send({});
        }
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
        const uInPayload = JSON.parse(inPayload); // Remove this, uncomment below
        // const uInPayload = JSON.parse(AES.decrypt(inPayload, process.env.DIFFIE_IV, sharedSecret))

        // Stop if uuid in payload does not match sender
        if (uuid != uInPayload.uuid) {
            console.log(`UUID mismatch in payload.`);
            res.status(401).send({});
        }
        
        // Pick b, generate g^ab mod p AND g^b mod p
        const g = "05" // TODO change later
        const p = "17" // is 23 in decimal
        const gaModP = uInPayload.keyhalf;

        const serverDiffie = crypto.createDiffieHellman(p, "hex", g, "hex");
        const gbModP = serverDiffie.generateKeys();

        // Save the session key;
        const sessionKey = serverDiffie.computeSecret(Buffer.from(gaModP, "hex"));
        user.session_key = sessionKey.toString("hex");

        // Create and encrypt payload containing g^b mod p and R_A
        const uOutPayload = JSON.stringify({
            "nonce": rA,
            "keyhalf": gbModP.toString("hex")
        });
        
        const outPayload = uOutPayload; // Remove this, uncomment below
        // const outPayload = AES.encrypt(uOutPayload, process.env.DIFFIE_IV, sharedSecret);

        // Add expected R_B to user obj
        const rB = crypto.randomInt(1, 10000);
        user.expected_nonce = rB;
        
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
        if (String(user.expected_nonce) != uInPayload.nonce) {
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
    }
    // Twilio body goes here
}