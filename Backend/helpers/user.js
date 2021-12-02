const User = require("../models/user");
const crypto = require("crypto")

const createNonceString = () => {
    const oneTimePass = crypto.randomInt(0, 1000000) // 6 digit number
    // const oneTimePass = 101234 // 6 digit number
    return oneTimePass.toString().padStart(6, "0");
};

const getUniqueUuidAndSalt = async (phone) => {
    let salt = crypto.randomInt(1000000000);
    do {
        salt = (salt + 1) % 1000000000;
        let sha = crypto.createHmac('sha256', String(salt));
        
        sha.update(phone);
        uuid = sha.digest("hex");

        // Check if the UUID is already in use
        uuidUser = await findUserWithUuid(uuid);
    } // Pick a new salt until we get a unique UUID
    while (uuidUser);

    return [uuid, salt];
};

const findUserWithUuid = async (uuid) => {
    const myUser = await User.findOne({ uuid: uuid }).catch(() => null);
    return myUser;
};

const findUserWithEncPhone = async (encryptedPhone) => {
    const myUser = await User.findOne({ phone: encryptedPhone }).catch(() => null);
    return myUser;
};

const createNonUserNumber = async (encryptedPhone) => {
    // Check if there is a user already with that phone number
    const myUser = await findUserWithEncPhone(encryptedPhone);
    if (myUser) return myUser;

    // Create the user
    const newUser = {
        "phone": encryptedPhone,
        "detected_recent_messages": 0,
        "account_status": "inactive"
    }
    return await User.create(newUser);
};

const convertNonUserToUnverified = async (encryptedPhone) => {
    const myUser = await findUserWithEncPhone(encryptedPhone);
    if (!myUser) throw "ERROR: No user to convert";

    myUser.account_status = "unverified";
    myUser.nonce_expected = createNonceString();
    myUser.nonce_attempts_left = 3;
    myUser.time_requested_verification = Date.now();

    return await myUser.save();
};

const revertUnverifiedToNonUser = async (encryptedPhone) => {
    const myUser = await findUserWithEncPhone(encryptedPhone);
    if (!myUser) throw "ERROR: No user to convert";

    myUser.account_status = "inactive";
    myUser.nonce_expected = undefined;
    myUser.nonce_attempts_left = undefined;
    myUser.time_requested_verification = undefined;

    return await myUser.save();
};

const createUnverifiedUser = async (encryptedPhone) => {
    const myUser = await findUserWithEncPhone(encryptedPhone);
    if (myUser) throw "ERROR: User with number already exists";

    const newUser = {
        "phone": encryptedPhone,
        "detected_recent_messages": 0,
        "account_status": "unverified",
        "nonce_expected": createNonceString(),
        "nonce_attempts_left": 3,
        "time_requested_verification": Date.now(),
    }
    return await User.create(newUser);
};

const beginVerifyingAgain = async (encryptedPhone) => {
    const myUser = await findUserWithEncPhone(encryptedPhone);
    if (!myUser || myUser.account_status !== "verified") throw "ERROR: No user to or re-verify";

    myUser.nonce_expected = createNonceString();
    myUser.nonce_attempts_left = 3;
    myUser.time_requested_verification = Date.now();

    return await myUser.save();
};

// Convert unverified to verified, or regenerate verified
// account's uuid, salt, shared secret (and clear session key)
// Returns a user if verified, returns null
const verifyUser = async (encryptedPhone, nonce, secret) => {
    const myUser = await findUserWithEncPhone(encryptedPhone);
    if (!myUser || myUser.account_status === "inactive") return null;
    
    // Check nonce for match
    if (myUser.nonce_expected !== String(nonce)) {
        if (myUser.nonce_attempts_left > 1) {
            myUser.nonce_attempts_left -= 1;
            await myUser.save();
            return null;
        }
        // Ran out of nonce attempts
        else if (myUser.account_status === "unverified") {
            await revertUnverifiedToNonUser(encryptedPhone);
            return "ABORT";
        }
        else {
            myUser.nonce_expected = null;
            myUser.nonce_attempts_left = 0;
            await myUser.save();
            return "ABORT";
        }
    }
    // Pick salt and generate unique uuid
    const decryptedPhone = encryptedPhone; // Todo Decrypt
    const [uuid, salt] = await getUniqueUuidAndSalt(decryptedPhone);
    
    myUser.uuid = uuid;
    myUser.account_status = "verified";
    // Phone number already up-to-date
    myUser.salt = salt;
    myUser.shared_secret = secret;
    myUser.session_key = null;
    myUser.session_key_last_established = null;
    // Don't clear time_requested_verification
    myUser.nonce_expected = null;
    myUser.nonce_attempts_left = 0;
    
    return await myUser.save();
};

module.exports = {
    findUserWithUuid,
    findUserWithEncPhone,
    createNonUserNumber,
    convertNonUserToUnverified,
    revertUnverifiedToNonUser,
    createUnverifiedUser,
    beginVerifyingAgain,
    verifyUser,
}