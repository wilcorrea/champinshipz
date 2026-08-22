#!/usr/bin/env node
import { Command } from 'commander';
import chalk from 'chalk';
import { listCommand } from './commands/list.js';
import { standingsCommand } from './commands/standings.js';
import { bracketCommand } from './commands/bracket.js';
import { runMenu } from './menu.js';

function run(command) {
  return async (...args) => {
    try {
      await command(...args);
    } catch (error) {
      if (error.name === 'ExitPromptError') {
        return;
      }
      console.error(chalk.red(`\n  ${error.message}\n`));
      process.exitCode = 1;
    }
  };
}

const program = new Command();

program
  .name('championshipz')
  .description('Consulta campeonatos do Championshipz no terminal')
  .version('0.1.0');

program.command('list').description('Lista todos os campeonatos').action(run(listCommand));

program
  .command('standings')
  .argument('<slug>', 'slug do campeonato')
  .description('Mostra a classificação: tabela única em liga, uma por grupo em copa')
  .action(run(standingsCommand));

program
  .command('bracket')
  .argument('<slug>', 'slug do campeonato')
  .description('Mostra o chaveamento do mata-mata')
  .action(run(bracketCommand));

if (process.argv.length <= 2) {
  run(runMenu)();
} else {
  program.parse();
}
