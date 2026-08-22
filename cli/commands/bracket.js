import { api } from '../lib/api.js';
import { renderBracket } from '../lib/render.js';

export async function bracketCommand(slug) {
  const bracket = await api.bracket(slug);
  console.log(`\n${renderBracket(bracket)}\n`);
}
