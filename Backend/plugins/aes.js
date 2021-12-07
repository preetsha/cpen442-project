const crypto = require("crypto");

module.exports = {
    encryptPhone: (phone_number) => {
        const padded_phone = String("0".repeat(16) + phone_number).slice(-16)
        const phone_buffer = Buffer.from(padded_phone, "utf-8");
        const iv = Buffer.from(process.env.PHONE_IV, "utf-8");
        const key = Buffer.from(process.env.KEY, "utf-8");

        const cipher = crypto.createCipheriv('aes-256-cbc', key, iv);
        const encrypted = cipher.update(phone_buffer, "utf-8", "hex");

        return encrypted + cipher.final("hex");
    },

    decryptPhone: (encrypted_phone_number) => {
        const iv = Buffer.from(process.env.PHONE_IV, "utf-8");
        const key = Buffer.from(process.env.KEY, "utf-8");

        const phone_buffer = Buffer.from(encrypted_phone_number, "hex");
        const cipher = crypto.createDecipheriv("aes-256-cbc", key, iv);
        const padded_phone = Buffer.concat([cipher.update(phone_buffer), cipher.final()]).toString("utf-8");

        let phone_number = padded_phone
        while (phone_number.indexOf("0") == 0) {
            phone_number = phone_number.slice(1);
        }
        return phone_number;
    },

    encryptJsonString: (json_object, key) => {
        const jsonString = JSON.stringify(json_object);
        const key_buffer = Buffer.from(key, "utf-8");

        const jsonStringBuffer = Buffer.from(jsonString, "utf-8");
        const cipher = crypto.createCipheriv("aes-192-ecb", key_buffer, null);
        const encryptedJsonString = Buffer.concat([cipher.update(jsonStringBuffer), cipher.final()]).toString("base64");

        return encryptedJsonString;
    },
    decryptJsonString: (encryptedJson, key) => {
        const key_buffer = Buffer.from(key, "utf-8");

        const encryptedJsonBuffer = Buffer.from(encryptedJson, "base64");
        const cipher = crypto.createDecipheriv("aes-192-ecb", key_buffer, null);
        const jsonString = Buffer.concat([cipher.update(encryptedJsonBuffer), cipher.final()]).toString("utf-8");

        return jsonString;
    },
}