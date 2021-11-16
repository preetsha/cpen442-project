const mongoose = require("mongoose");

const ENDPOINT = process.env.DB_ADDR;

mongoose.connect(ENDPOINT, {
    useNewUrlParser: true,
    useUnifiedTopology: true,
    user: process.env.DB_USER,
    pass: process.env.DB_PASS
});

mongoose.connection.once("open", () => {
    console.log(`Connected to database at ${ENDPOINT}`);
});