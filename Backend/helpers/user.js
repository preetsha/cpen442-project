const User = require("../models/user");

module.exports = {
    findUserWithUuid: async (uuid) => {
        let myUser = await User.findOne({ uuid: uuid }).catch(() => null);
        return myUser;
    },
    findUserWithEncPhone: async (encryptedPhone) => {
        let myUser = await User.findOne({ phone: encryptedPhone }).catch(() => null);
        return myUser;
    }
}