const twilio = require('twilio');

const accountSid = process.env.TWILIO_ACCOUNT_SID; 
const authToken = process.env.TWILIO_AUTH_TOKEN;

const client = new twilio(accountSid, authToken);

console.log("Received Debug Request");

module.exports = {
    sendSMS: async (recipient, message) => {
        await client.messages.create({
            body: message,
            to: `+1${recipient}`, 
            from: '+12367065369' 
        });
    }
}