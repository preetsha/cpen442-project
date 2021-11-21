const User = require('../models/user');
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