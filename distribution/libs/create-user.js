db = db.getSiblingDB("thermostat")
var v = db.version()
var majorVersion = v.substring(0, v.indexOf('.'))
var minorMicro = v.substr(v.indexOf('.') + 1)
var minorVersion = minorMicro.substr(0, minorMicro.indexOf('.'))
if ( majorVersion < 2 || ( majorVersion == 2 && minorVersion <= 2 ) ) {
    // mongodb version 2.2 and below don't have the third argument.
    // this should create the user as read + write.
    db.addUser("$USERNAME","$PASSWORD")
} else if ( majorVersion == 2 && minorVersion <= 4 ) {
    db.addUser({ user: "$USERNAME", pwd: "$PASSWORD", roles: [ "readWrite" ] })
} else if ( majorVersion == 2 ) {
    db.createUser({ user: "$USERNAME", pwd: "$PASSWORD", roles: [ "readWrite" ] })
} else if ( majorVersion == 3 ) {
    db = db.getSiblingDB("admin")
    db.createUser({ user: "thermostat-admin", pwd: "$PASSWORD", roles: [ "dbOwner", "userAdminAnyDatabase" ] })
    db.auth("thermostat-admin", "$PASSWORD")
    db = db.getSiblingDB("thermostat")
    db.createUser({ user: "$USERNAME", pwd: "$PASSWORD", roles: [ "readWrite" ] })
} else {
    throw "Unknown mongo version: " + v
}
