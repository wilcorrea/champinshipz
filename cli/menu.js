import { select } from '@inquirer/prompts';
import chalk from 'chalk';
import { api } from './lib/api.js';
import { renderStandings, renderBracket } from './lib/render.js';

const EXIT = Symbol('exit');
const BACK = Symbol('back');

async function pickChampionship(championships) {
  return select({
    message: 'Qual campeonato?',
    choices: [
      ...championships.map((championship) => ({
        name: `${championship.name} ${chalk.gray(`(${championship.type})`)}`,
        value: championship.slug,
      })),
      { name: chalk.gray('Sair'), value: EXIT },
    ],
  });
}

async function pickView(championship) {
  if (championship.type !== 'CUP') {
    return 'standings';
  }

  return select({
    message: 'O que você quer ver?',
    choices: [
      { name: 'Fase de grupos', value: 'standings' },
      { name: 'Chaveamento', value: 'bracket' },
      { name: chalk.gray('Voltar'), value: BACK },
    ],
  });
}

export async function runMenu() {
  while (true) {
    const championships = await api.championships();

    if (championships.length === 0) {
      console.log(chalk.yellow('\n  Nenhum campeonato cadastrado ainda.\n'));
      return;
    }

    const slug = await pickChampionship(championships);

    if (slug === EXIT) {
      console.log(chalk.gray('\n  Até mais.\n'));
      return;
    }

    const championship = championships.find((candidate) => candidate.slug === slug);
    const view = await pickView(championship);

    if (view === BACK) {
      continue;
    }

    const output =
      view === 'bracket' ? renderBracket(await api.bracket(slug)) : renderStandings(await api.standings(slug));

    console.log(`\n${chalk.bold(championship.name)}\n\n${output}\n`);
  }
}
