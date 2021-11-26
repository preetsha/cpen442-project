const mongoose = require("mongoose");
const { Schema } = mongoose;

const userSchema = new Schema({
    uuid: String,
    phone: String,
    salt: Number,
    shared_secret: String,
    session_key: String, 
    expected_nonce: Number,
    time_requested_verification: Date,
    time_completed_verification: Date,
    expected_nonce: String,
    is_active_user: Boolean, // False if this is a non-user number someone trusts
    is_verified: Boolean,
    users_trusted: [ String ],
});

const User = mongoose.model("users", userSchema);
module.exports = User;