const express = require("express");
const router = express.Router();

const twilio = require('twilio');

const accountSid = process.env.TWILIO_ACCOUNT_SID; 
const authToken = process.env.TWILIO_AUTH_TOKEN;

const client = new twilio(accountSid, authToken);



/* Returns all lists where the userID field matches the user making the request */
router.get("/", async (req, res) => { 
	console.log("Received Debug Request");
	message = await client.messages.create(to = "+17783250393", from_ = "+12367065369", body = "PLEASE");
	/*client.messages.create({
		body: 'Ahoy, friend!',
		to: '+17783250393', 
		from: '+12367065369' 
	})
	.then((message) => console.log(message.status));*/
	
	res.status(200).send(message);
});

module.exports = router;