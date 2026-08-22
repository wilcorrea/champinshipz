import chalk from 'chalk';
import { api } from '../lib/api.js';

const TYPE_BADGES = {
  CUP: chalk.magenta('CUP   '),
  LEAGUE: chalk.blue('LEAGUE'),
};

export async function listCommand() {
  const championships = await api.championships();

  if (championships.length === 0) {
    console.log(chalk.yellow('\n  Nenhum campeonato cadastrado ainda.\n'));
    return;
  }

  console.log();
  for (const championship of championships) {
    const badge = TYPE_BADGES[championship.type] ?? championship.type;
    const teams = chalk.gray(`${championship.teams.length} times`);
    console.log(`  ${badge}  ${chalk.bold(championship.name)}  ${chalk.gray(championship.slug)}  ${teams}`);
  }
  console.log();
}
