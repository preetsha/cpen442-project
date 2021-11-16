const mongoose = require("mongoose");
const { Schema } = mongoose;

const userSchema = new Schema({
    uuid: String, // String is shorthand for {type: String}
    session_key: String,
    account_created: Date, // Basically a timestamp 
    account_last_active: Date,
    recent_message_count: Number, // For rate limiting
    time_of_establishment: Date,
    users_trusted: [String] // List of UUIDs
});

const User = mongoose.model("users", userSchema);
module.exports = User;