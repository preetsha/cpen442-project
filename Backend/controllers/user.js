const User = require("../models/user");
const crypto = require("crypto");
/*
// Twilio Library
const Twilio = require('twilio');

// Check configuration variables
if (process.env.TWILIO_API_KEY == null ||
    process.env.TWILIO_API_SECRET == null ||
    process.env.TWILIO_ACCOUNT_SID == null ||
    process.env.VERIFICATION_SERVICE_SID == null ||
    process.env.COUNTRY_CODE == null) {
  console.log('Please copy the .env.example file to .env, ' +
                    'and then add your Twilio API Key, API Secret, ' +
                    'and Account SID to the .env file. ' +
                    'Find them on https://www.twilio.com/console');
  process.exit();
}

if (process.env.APP_HASH == null) {
  console.log('Please provide a valid Android app hash, ' +
                'in the .env file');
  process.exit();
}

if (process.env.CLIENT_SECRET == null) {
  console.log('Please provide a secret string to share, ' +
                'between the app and the server ' +
                'in the .env file');
  process.exit();
}

const configuredClientSecret = process.env.CLIENT_SECRET;

// Initialize the Twilio Client
const twilioClient = new Twilio(process.env.TWILIO_API_KEY,
    process.env.TWILIO_API_SECRET,
    {accountSid: process.env.TWILIO_ACCOUNT_SID});

const SMSVerify = require('./SMSVerify.js');
const smsVerify = new SMSVerify(twilioClient,
    process.env.APP_HASH,
    process.env.VERIFICATION_SERVICE_SID,
    process.env.COUNTRY_CODE);
*/
module.exports = {
    // This is just an example, we do not want to keep this function in the final implementation
    getUser: async (req, res) => {
        try {
            let myUser = await User.findOne({ uuid: req.body.uuid }).catch(() => null);

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
    createUser: async (req, res) => {
        res.status(200).send({ "message": "This does nothing right now :/" });
    },
    requestRegistration: async (req, res) => {
        const phoneNumber = req.body.phone_number;
        const paddedPhone = ("0000000000000000" + phoneNumber).slice(-16); // Pad phone number to 16 chars for encryption
        
        console.log(paddedPhone);
        // Encrypt the phone number using the phone symmetric key
        const iv = Buffer.from(process.env.PHONE_IV, "utf-8");
        const key = Buffer.from(process.env.PHONE_KEY, "utf-8");
        const cipher = crypto.createCipheriv('aes-256-cbc', key, iv);
        const encryptedPhone = cipher.update(paddedPhone, "utf-8", "hex");

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
            existingUser = await User.find({uuid: uuid}).catch(() => {
                console.log(`UUID Collision! ${uuid} is already used!`);
                return [];
            });
        } // Pick a new salt until we get a unique UUID
        while (existingUser.length > 0);

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

        const paddedPhone = ("0000000000000000" + phoneNumber).slice(-16); // Pad phone number to 16 chars for encryption
        console.log(paddedPhone);
        // Encrypt the phone number using the phone symmetric key
        const iv = Buffer.from(process.env.PHONE_IV, "utf-8");
        const key = Buffer.from(process.env.PHONE_KEY, "utf-8");
        const cipher = crypto.createCipheriv('aes-256-cbc', key, iv);
        const encryptedPhone = cipher.update(paddedPhone, "utf-8", "hex");
        
        // Find user corresponding to the phone number
        console.log(encryptedPhone);
        const user = await User.findOne({ phone: encryptedPhone }).catch(() => null);

        if (user) {
            // Confirm the code matches
            console.log(user)
            if (user.expected_nonce != oneTimePass) {
                console.log(user.expected_nonce);
                console.log(oneTimePass);
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

            // Send back the phone number and salt, encrypted with the shared secret
            res.status(201).send({ phone: phoneNumber, salt: user.salt })
        }
        else {
            res.status(404).send({});
        }
    },

    /*
    request: async (req, res) => {
        const clientSecret = req.body.client_secret;
        const phone = req.body.phone;

        if (clientSecret == null || phone == null) {
            // send an error saying that both client_secret and phone are required
            res.send(500, 'Both client_secret and phone are required.');
            return;
        }

        if (configuredClientSecret != clientSecret) {
            res.send(500, 'The client_secret parameter does not match.');
            return;
        }

        smsVerify.request(phone);
        res.send({
            success: true,
        });
    },
    verify: async (req, res) => {
        const clientSecret = req.body.client_secret;
        const phone = req.body.phone;
        const smsMessage = req.body.sms_message;

        if (clientSecret == null || phone == null || smsMessage == null) {
            // send an error saying that all parameters are required
            res.send(500, 'The client_secret, phone, ' +
                'and sms_message parameters are required');
            return;
        }

        if (configuredClientSecret != clientSecret) {
            res.send(500, 'The client_secret parameter does not match.');
            return;
        }

        smsVerify.verify(phone, smsMessage, function (isSuccessful) {
            if (isSuccessful) {
                res.send({
                    success: true,
                    phone: phone,
                });
            } else {
                res.send({
                    success: false,
                    msg: 'Unable to validate code for this phone number',
                });
            }
        });
    },
    reset: async (req, res) => {
        const clientSecret = req.body.client_secret;
        const phone = req.body.phone;

        if (clientSecret == null || phone == null) {
            // send an error saying that all parameters are required
            res.send(500,
                'The client_secret and phone parameters are required');
            return;
        }

        if (configuredClientSecret != clientSecret) {
            response.send(500, 'The client_secret parameter does not match.');
            return;
        }

        const isSuccessful = smsVerify.reset(phone);

        if (isSuccessful) {
            res.send({
                success: true,
                phone: phone,
            });
        } else {
            res.send({
                success: false,
                msg: 'Unable to reset code for this phone number',
            });
        }
    }
    */
}