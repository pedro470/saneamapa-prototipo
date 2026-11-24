/**
 * SaneaMapaTipos — ícone, cor e texto curto de cada tipo de ocorrência (RNF02) e de cada nível
 * de urgência (RF006). Usado no app do morador, no fiscal, no painel e na legenda do mapa.
 */
window.SaneaMapaTipos = (function () {
  const TIPOS = [
    {
      id: 'CASA_SEM_AGUA',
      nome: 'Casa sem ligação de água',
      curto: 'Sem água',
      cor: '#2e51b9',
      // casa com gota
      desenho: '<path d="M3 11.5 12 4l9 7.5"/><path d="M5 10v10h14V10"/><path d="M12 11c-1.4 1.9-2.3 3.1-2.3 4.3a2.3 2.3 0 0 0 4.6 0c0-1.2-.9-2.4-2.3-4.3z"/>'
    },
    {
      id: 'AGUA_FORA_PADRAO',
      nome: 'Água fora do padrão',
      curto: 'Água ruim',
      cor: '#97570d',
      // gota com alerta
      desenho: '<path d="M12 3c-3.2 4.2-6 7.6-6 11a6 6 0 0 0 12 0c0-3.4-2.8-6.8-6-11z"/><path d="M12 10v4"/><path d="M12 17h.01"/>'
    },
    {
      id: 'RUA_SEM_ESGOTO',
      nome: 'Rua sem rede de esgoto',
      curto: 'Sem esgoto',
      cor: '#5c33a8',
      // cano interrompido
      desenho: '<path d="M2 9h7v6H2"/><path d="M22 9h-7v6h7"/><path d="M11 10.5l2 3"/><path d="M13 10.5l-2 3"/>'
    },
    {
      id: 'ESGOTO_CEU_ABERTO',
      nome: 'Esgoto a céu aberto',
      curto: 'Esgoto aberto',
      cor: '#890028',
      // ondas
      desenho: '<path d="M3 9c1.5-1.5 3-1.5 4.5 0s3 1.5 4.5 0 3-1.5 4.5 0 3 1.5 4.5 0"/><path d="M3 15c1.5-1.5 3-1.5 4.5 0s3 1.5 4.5 0 3-1.5 4.5 0 3 1.5 4.5 0"/>'
    }
  ];

  const URGENCIAS = [
    { id: 'CRITICA', nome: 'Crítica', cor: '#b91c1c' },
    { id: 'ELEVADA', nome: 'Elevada', cor: '#d97706' },
    { id: 'NORMAL', nome: 'Normal', cor: '#ffffff' }
  ];

  function porId(id) {
    return TIPOS.find(t => t.id === id) || { id, nome: id, curto: id, cor: '#1f2937', desenho: '<circle cx="12" cy="12" r="6"/>' };
  }

  function urgencia(id) {
    return URGENCIAS.find(u => u.id === id) || null;
  }

  /** SVG decorativo (aria-hidden): o texto do tipo sempre acompanha o ícone. */
  function icone(id, tamanho = 24, cor = 'currentColor') {
    return `<svg class="icone-tipo" width="${tamanho}" height="${tamanho}" viewBox="0 0 24 24" fill="none" stroke="${cor}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">${porId(id).desenho}</svg>`;
  }

  return { TIPOS, URGENCIAS, porId, urgencia, icone };
})();
