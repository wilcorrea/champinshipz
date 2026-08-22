import factory from 'prompt-sync';

const prompt = factory();

const name = prompt("Informe o nome: ");
console.log("O nome é:", name);
