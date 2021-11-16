const express = require("express");
const router = express.Router();

/* Returns all lists where the userID field matches the user making the request */
router.get("/", (req, res) => { 
	console.log("Received Debug Request");
	res.status(200).send({message: "OK"});
});

module.exports = router;