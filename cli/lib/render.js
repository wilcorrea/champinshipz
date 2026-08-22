import Table from 'cli-table3';
import chalk from 'chalk';

const COLUMN_LABELS = {
  matchesPlayed: 'J',
  wins: 'V',
  draws: 'E',
  losses: 'D',
  goalsFor: 'GP',
  goalsAgainst: 'GC',
  goalDifference: 'SG',
  points: 'P',
  form: 'UR',
};

const COLUMN_DESCRIPTIONS = {
  matchesPlayed: 'Jogos',
  wins: 'Vitórias',
  draws: 'Empates',
  losses: 'Derrotas',
  goalsFor: 'Gols pró',
  goalsAgainst: 'Gols contra',
  goalDifference: 'Saldo de gols',
  points: 'Pontos',
  form: 'Últimos resultados',
};

const FORM_DOTS = {
  WIN: chalk.green('●'),
  DRAW: chalk.yellow('●'),
  LOSS: chalk.red('●'),
};

const STATUS_STYLES = {
  QUALIFIED: chalk.green.bold,
  ELIMINATED: chalk.red.dim,
};

const ROUND_LABELS = {
  ROUND_OF_32: '32 avos de final',
  ROUND_OF_16: 'Oitavas de final',
  QUARTER_FINAL: 'Quartas de final',
  SEMI_FINAL: 'Semifinais',
  THIRD_PLACE: 'Disputa de 3º lugar',
  FINAL: 'Final',
};

const SIDE_WIDTH = 20;

function formatForm(form) {
  if (!form || form.length === 0) {
    return chalk.gray('—');
  }
  return form.map((result) => FORM_DOTS[result] ?? chalk.gray('○')).join(' ');
}

function statValue(row, column) {
  const value = row[column];
  if (column === 'goalDifference' && value > 0) {
    return `+${value}`;
  }
  return String(value);
}

function renderLegend(columns) {
  const glossary = columns
    .map((column) => `${chalk.bold(COLUMN_LABELS[column] ?? column)} ${chalk.gray(COLUMN_DESCRIPTIONS[column] ?? column)}`)
    .join(chalk.gray('  ·  '));

  const markers = [
    `${chalk.green('●')} ${chalk.gray('Vitória')}`,
    `${chalk.yellow('●')} ${chalk.gray('Empate')}`,
    `${chalk.red('●')} ${chalk.gray('Derrota')}`,
    `${chalk.green.bold('nome')} ${chalk.gray('classificado')}`,
    `${chalk.red.dim('nome')} ${chalk.gray('eliminado')}`,
  ].join(chalk.gray('  ·  '));

  return `${glossary}\n${markers}`;
}

function formatTeam(row) {
  const style = STATUS_STYLES[row.status] ?? ((value) => value);
  return `${style(row.teamName)} ${chalk.gray(row.teamCode)}`;
}

function renderRows(rows, columns) {
  const table = new Table({
    head: ['#', 'Time', ...columns.map((column) => COLUMN_LABELS[column] ?? column)].map((label) =>
      chalk.bold(label),
    ),
    colAligns: ['right', 'left', ...columns.map((column) => (column === 'form' ? 'left' : 'right'))],
    style: { head: [], border: [] },
  });

  for (const row of rows) {
    table.push([
      row.position,
      formatTeam(row),
      ...columns.map((column) => (column === 'form' ? formatForm(row.form) : statValue(row, column))),
    ]);
  }

  return table.toString();
}

export function renderStandings(standings) {
  const columns = standings.columns ?? [];

  if (standings.type === 'LEAGUE') {
    return `${renderRows(standings.table ?? [], columns)}\n\n${renderLegend(columns)}`;
  }

  const groups = standings.groups ?? [];

  if (groups.length === 0) {
    return chalk.yellow('  Nenhum grupo com jogos registrados ainda.');
  }

  const tables = groups
    .map((group) => `${chalk.bold.underline(`Grupo ${group.name}`)}\n${renderRows(group.rows, columns)}`)
    .join('\n\n');

  return `${tables}\n\n${renderLegend(columns)}`;
}

function sideLabel(team) {
  return team ? `${team.name} (${team.code})` : '—';
}

function paintSide(match, team, text) {
  if (!match.played || !match.winnerTeamId || !team) {
    return chalk.gray(text);
  }
  return match.winnerTeamId === team.teamId ? chalk.green.bold(text) : chalk.dim(text);
}

function renderMatch(match) {
  const number = chalk.gray(`#${match.matchNumber}`.padEnd(3));
  const home = paintSide(match, match.home, sideLabel(match.home).padStart(SIDE_WIDTH));
  const away = paintSide(match, match.away, sideLabel(match.away).padEnd(SIDE_WIDTH));
  const score = match.played ? chalk.bold(`${match.homeGoals} x ${match.awayGoals}`) : chalk.gray('– x –');

  const penalties =
    match.homePenalties != null ? chalk.yellow(` [pên ${match.homePenalties}-${match.awayPenalties}]`) : '';

  return `  ${number} ${home}  ${score}  ${away}${penalties}`;
}

export function renderBracket(bracket) {
  const rounds = bracket.rounds ?? [];

  if (rounds.length === 0) {
    return chalk.yellow('  Chaveamento ainda não foi gerado — a fase de grupos precisa terminar antes.');
  }

  return rounds
    .map((round) => {
      const title = ROUND_LABELS[round.round] ?? round.round;
      const matches = round.matches.map(renderMatch).join('\n');
      return `${chalk.bold.underline(title)}\n${matches}`;
    })
    .join('\n\n');
}
