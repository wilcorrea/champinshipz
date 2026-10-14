const express = require("express");
const app = express();
const path = require('path');
const session = require("express-session");
const FileStore = require('session-file-store')(session);
const { MongoClient } = require('mongodb');
const http = require('http');
const https = require('https');

https.globalAgent.maxSockets = 5000;
http.globalAgent.maxSockets = 5000;
app.use(express.static(path.join(__dirname, "lazer")));
app.set('view engine', 'hbs');
app.set('views', path.join(__dirname, 'views'));
app.use(express.urlencoded({ extended: true }))
app.use(
    session({
        secret: 'keyboard cat',
        store: new FileStore({
            path: './sessions',
            ttl: 3600,
            retries: 5
        }),
        resave: false,
        saveUninitialized: true,
        cookie: { httpOnly: true, sameSite: 'lax', maxAge: 1000 * 60 * 30 },
    })
);
let database;
async function connect(){
    if(database){
        return database;
    }
    const url = 'mongodb://localhost:27017';
    const client = new MongoClient(url);
    await client.connect();
    database = client.db('PIFA');
    return database;
}

app.get("/", function(request,response){
    response.render("login", {title:"Login"})
})

app.post("/login", async function(request, response){
    const database = await connect();
    const usuario = await database.collection("usuarios").findOne({email: request.body.email});
    if(request.body.senha === usuario.senha){
        //response.render("main")
        request.session.user = usuario.nome;
        request.session.save(function(erro){
            if(erro){
                response.render("login", {title:"Login", email: request.body.email, senha: request.body.senha})
                return
            }
            response.redirect("/classificacao")
        })
        return
    }
    response.render("login", {title:"Login", email: request.body.email, senha: request.body.senha})
})

app.get("/criar-conta", function(request, response){
    response.render("createaccount")
})

app.post("/criar-conta", async function(request, response){
    const database = await connect();
    const {email, senha, nome} = request.body;

    await database.collection("usuarios").insertOne({email, senha, nome});
    response.redirect("/")
})

app.get("/classificacao", function(request, response){
    response.render("main",{user: request.session.user})
})

app.get("/api/v1/resultados/1", async function(request, response){
    const database = await connect();
    const resultados = await database.collection("resultados").findOne({id: 1})
    response.send(resultados);
})

app.post("/api/v1/resultados/1", async function(request, response){
  const database = await connect();

})

app.listen(4000, function(erro) {
    if (erro) {
        console.log("Erro ao Iniciar.");
    } else {
        console.log("Servidor Iniciado.")
    }
})