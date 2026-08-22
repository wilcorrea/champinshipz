const BASE_URL = (process.env.CHAMPZ_API ?? 'http://localhost:8080').replace(/\/+$/, '');

async function get(path) {
  let response;

  try {
    response = await fetch(`${BASE_URL}${path}`);
  } catch (cause) {
    throw new Error(`Não consegui falar com a API em ${BASE_URL}. O backend está rodando?`, { cause });
  }

  if (response.status === 404) {
    throw new Error(`Não encontrado: ${path}`);
  }

  if (!response.ok) {
    throw new Error(`A API respondeu ${response.status} em ${path}`);
  }

  return response.json();
}

export const api = {
  baseUrl: BASE_URL,
  championships: () => get('/api/championships'),
  standings: (slug) => get(`/api/championships/${slug}/standings`),
  bracket: (slug) => get(`/api/championships/${slug}/bracket`),
};
