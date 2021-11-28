const mongoose = require("mongoose");
const { Schema } = mongoose;

const userSchema = new Schema({
    uuid: String,
    phone: String,
    salt: Number,
    shared_secret: String,
    session_key: String,
    session_key_last_established: Date, 
    expected_nonce: Number,
    time_requested_verification: Date,
    time_completed_verification: Date,
    expected_nonce: String,
    is_active_user: Boolean, // False if this is a non-user number someone trusts
    is_verified: Boolean,
    trusted_numbers: [ String ],
    spam_numbers: [ String ]
});

const User = mongoose.model("users", userSchema);
module.exports = User;