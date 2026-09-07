
const { createApp } = Vue;

const textos = {
    pt: {
        titulo: "Classificação",
    },
    en: {
        titulo: "Standings",
    }
}

createApp({
    mounted () {
        document.addEventListener('click', this.handleClickOutside);
        this.carregarResultados();
    },
    beforeUnmount() {
        document.removeEventListener('click', this.handleClickOutside);
    },
    methods: {
        handleClickOutside (event) {
            const classesDoSeletor = [
                'seletor-de-idioma__botao',
                'seletor-de-idioma__botao__texto',
                'nome-grupo__icone',
            ];

            if (! classesDoSeletor.some((classe) => event.target.classList.contains(classe))) {
                this.seletorDeIdioma.visivel = false;
            }
        },
        async carregarResultados(){
            try{
            const resposta = await fetch('http://localhost:8080/api/v1/resultados/1');
            const dados = await resposta.json();
            this.grupos = dados.grupos;
            } catch (erro) {
                console.error("Erro ao carregar resultados:", erro);
            }
        },
        salvarAlteracoes(grupo) {
            console.log(grupo);
            this.modal.selecionado = undefined;
            this.carregarResultados();
        },
        copiar(object) {
            return JSON.parse(JSON.stringify(object));
        },
        alternarResultado(ultimos, resultado, index) {
            const proximos = {
                vitoria: 'empate',
                empate: 'derrota',
                derrota: 'tbd',
                tbd: 'vitoria',
            }
            const novoResultado = proximos[resultado];
            ultimos[index] = novoResultado;
        },
    },
    computed: {
        titulo () {
            return textos[this.seletorDeIdioma.selecionado].titulo;
        },
        selecionado () {
            return this.seletorDeIdioma.idiomas
                    .find((idioma) => idioma.codigo === this.seletorDeIdioma.selecionado)
                    ?.nome ?? 'Idioma';
        }
    },
    data() {
        return {
            seletorDeIdioma: {
                visivel: false,
                selecionado: "pt",
                idiomas: [
                    { codigo: "pt", nome: "Português" },
                    { codigo: "en", nome: "English" },
                ]
            },
            modal: {
                selecionado: undefined,
            },
            grupos: [],
        };
    },
}).mount("#app");