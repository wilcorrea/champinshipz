import { api } from '../lib/api.js';
import { renderStandings } from '../lib/render.js';

export async function standingsCommand(slug) {
  const standings = await api.standings(slug);
  console.log(`\n${renderStandings(standings)}\n`);
}
