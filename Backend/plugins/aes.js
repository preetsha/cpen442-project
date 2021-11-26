const crypto = require("crypto");

module.exports = {
    encrypt: (plaintext, ivString, keyString) => {
        if (plaintext.length < 16) 
            throw `Plaintext length ${plaintext.length} is too short!`;

        if (ivString.length != 16) 
            throw `IV string length ${ivString.length} is invalid`;

        if (keyString.length != 32) 
            throw `Keystring length ${keyString.length} is invalid`;
        
        const iv = Buffer.from(ivString, "utf-8");
        const key = Buffer.from(keyString, "utf-8");
        const cipher = crypto.createCipheriv('aes-256-cbc', key, iv);
        const encrypted = cipher.update(plaintext, "utf-8", "hex");

        return encrypted;
    },

    decrypt: (ciphertext, ivString, keyString) => {
        if (ciphertext.length < 16) 
            throw `Plaintext length ${ciphertext.length} is too short!`;

        if (ivString.length != 16) 
            throw `IV string length ${ivString.length} is invalid`;

        if (keyString.length != 32) 
            throw `Keystring length ${keyString.length} is invalid`;
        
        const iv = Buffer.from(ivString, "utf-8");
        const key = Buffer.from(keyString, "utf-8");
        const cipher = crypto.createDecipheriv('aes-256-cbc', key, iv);
        const decrypted = cipher.update(ciphertext, "utf-8", "hex");

        return decrypted;
    }
}