const mongoose = require("mongoose");
const { Schema } = mongoose;

const userSchema = new Schema({
    uuid: String,
    account_status: String, // verified, unverified, inactive
    phone: String,
    salt: Number,
    shared_secret: String,
    
    session_key: String,
    session_key_last_established: Date,

    time_requested_verification: Date,
    nonce_expected: String,
    nonce_attempts_left: Number,

    // time_completed_verification: Date,
    trusted_numbers: [ String ],
    spam_numbers: [ String ],

    detected_recent_messages: Number,
});

const User = mongoose.model("users", userSchema);
module.exports = User;