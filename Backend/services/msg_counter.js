const User = require("../models/user");

const resetMessages = async () => {
    console.log("Reseting counters...")
    await User.updateMany({},
        { $set: { detected_recent_messages: 0 } },
    );
}


// Reset the counter every hour
resetMessages();
setInterval(resetMessages, 1000 * 60 * 60);