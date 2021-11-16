require("dotenv").config();

// Initialize Express
const app = require("express")();
const bodyParser = require("body-parser");
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Import the database connection
// require("./database/mongodb");

// Import routes
const debugRouter = require("./routes/debug");
app.use("/debug", debugRouter);

// Listen on PORT
let server = app.listen(8080, function () { // TODO Read process.env.PORT
    let host = server.address().address;
    let port = server.address().port;
    console.log("Listening at http://%s:%s", host, port);
});