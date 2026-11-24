/**
 * SaneaMapaGrafo — desenha o grafo de conhecimento do painel em SVG, sem biblioteca externa.
 *
 * O desenho usa um leiaute de forças (repulsão entre nós, mola nas arestas) calculado uma vez
 * por conjunto de nós e guardado em memória, para a tela não "pular" a cada redesenho.
 * Cada nó é um botão focável: o conteúdo completo do nó selecionado aparece em texto ao lado,
 * então o desenho nunca é a única forma de ler o grafo (RNF04).
 */
window.SaneaMapaGrafo = (function () {
  // Aparência por classe da ontologia. A forma repete a informação da cor.
  const ESTILO = {
    PONTO:                  { cor: 'var(--accent)',   raio: 11, forma: 'circulo',  rotular: true },
    RELATO:                 { cor: 'var(--cat-3)',    raio: 6,  forma: 'circulo',  rotular: false },
    EVIDENCIA_FOTO:         { cor: 'var(--cat-5)',    raio: 4,  forma: 'circulo',  rotular: false },
    EVIDENCIA_VIDEO:        { cor: 'var(--cat-5)',    raio: 4,  forma: 'circulo',  rotular: false },
    EVIDENCIA_AUDIO:        { cor: 'var(--cat-5)',    raio: 4,  forma: 'circulo',  rotular: false },
    VISTORIA:               { cor: 'var(--positivo)', raio: 7,  forma: 'losango',  rotular: false },
    FISCAL:                 { cor: 'var(--cat-2)',    raio: 9,  forma: 'losango',  rotular: true },
    BAIRRO:                 { cor: 'var(--cat-1)',    raio: 10, forma: 'quadrado', rotular: true },
    SETOR_CENSITARIO:       { cor: 'var(--cat-4)',    raio: 7,  forma: 'quadrado', rotular: false },
    CAMADA_VULNERABILIDADE: { cor: 'var(--atencao)',  raio: 9,  forma: 'quadrado', rotular: true },
    TIPO_OCORRENCIA:        { cor: 'var(--ink)',      raio: 9,  forma: 'triangulo', rotular: true },
    SERVICO_SANEAMENTO:     { cor: 'var(--ink)',      raio: 12, forma: 'triangulo', rotular: true }
  };
  const PADRAO = { cor: 'var(--soft)', raio: 6, forma: 'circulo', rotular: false };
  const LARGURA = 960;
  const ALTURA = 600;

  const leiautes = {};   // assinatura dos nós -> { id: {x, y} }
  const vistas = {};     // containerId -> { assinatura, x, y, w, h }

  const esc = s => String(s ?? '').replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m]));

  function estilo(classe) {
    return ESTILO[classe] || PADRAO;
  }

  /** Leiaute de forças determinístico: mesma entrada, mesmo desenho. */
  function calcularLeiaute(nos, arestas) {
    const n = nos.length;
    const pos = nos.map((no, i) => {
      const ang = i * 2.399963; // ângulo áureo: espalha sem sobrepor
      const r = 30 + 14 * Math.sqrt(i);
      return { x: Math.cos(ang) * r, y: Math.sin(ang) * r, vx: 0, vy: 0 };
    });
    const indice = {};
    nos.forEach((no, i) => { indice[no.id] = i; });
    const ligas = arestas
      .map(a => [indice[a.origem], indice[a.destino]])
      .filter(([a, b]) => a !== undefined && b !== undefined);

    const passos = n > 400 ? 120 : 320;
    for (let passo = 0; passo < passos; passo++) {
      const calor = 1 - passo / passos;
      for (let i = 0; i < n; i++) {
        for (let j = i + 1; j < n; j++) {
          let dx = pos[i].x - pos[j].x;
          let dy = pos[i].y - pos[j].y;
          let d2 = dx * dx + dy * dy;
          if (d2 < 0.01) { dx = 0.1 * (i - j); dy = 0.1; d2 = 0.02; }
          const f = 9000 / d2;
          const d = Math.sqrt(d2);
          const fx = (dx / d) * f;
          const fy = (dy / d) * f;
          pos[i].vx += fx; pos[i].vy += fy;
          pos[j].vx -= fx; pos[j].vy -= fy;
        }
      }
      for (const [a, b] of ligas) {
        const dx = pos[b].x - pos[a].x;
        const dy = pos[b].y - pos[a].y;
        const d = Math.sqrt(dx * dx + dy * dy) || 0.01;
        const f = (d - 110) * 0.05;
        const fx = (dx / d) * f;
        const fy = (dy / d) * f;
        pos[a].vx += fx; pos[a].vy += fy;
        pos[b].vx -= fx; pos[b].vy -= fy;
      }
      for (let i = 0; i < n; i++) {
        // Gravidade fraca mantém componentes soltos perto do centro.
        pos[i].vx -= pos[i].x * 0.008;
        pos[i].vy -= pos[i].y * 0.008;
        const v = Math.sqrt(pos[i].vx * pos[i].vx + pos[i].vy * pos[i].vy) || 0.01;
        const limite = 45 * calor + 0.5;
        const escala = Math.min(1, limite / v);
        pos[i].x += pos[i].vx * escala;
        pos[i].y += pos[i].vy * escala;
        pos[i].vx = 0; pos[i].vy = 0;
      }
    }
    const saida = {};
    nos.forEach((no, i) => { saida[no.id] = { x: pos[i].x, y: pos[i].y }; });
    return saida;
  }

  function enquadrar(coords, ids) {
    let x0 = Infinity, y0 = Infinity, x1 = -Infinity, y1 = -Infinity;
    ids.forEach(id => {
      const p = coords[id];
      x0 = Math.min(x0, p.x); y0 = Math.min(y0, p.y);
      x1 = Math.max(x1, p.x); y1 = Math.max(y1, p.y);
    });
    if (!isFinite(x0)) return { x: -LARGURA / 2, y: -ALTURA / 2, w: LARGURA, h: ALTURA };
    const margem = 60;
    let w = Math.max(x1 - x0 + margem * 2, 240);
    let h = Math.max(y1 - y0 + margem * 2, 150);
    // Mantém a proporção do quadro para o desenho ocupar a área toda.
    if (w / h > LARGURA / ALTURA) h = w * ALTURA / LARGURA; else w = h * LARGURA / ALTURA;
    return { x: (x0 + x1) / 2 - w / 2, y: (y0 + y1) / 2 - h / 2, w, h };
  }

  function formaSvg(e, x, y, extra) {
    const r = e.raio;
    if (e.forma === 'quadrado') return `<rect x="${x - r}" y="${y - r}" width="${r * 2}" height="${r * 2}" rx="2" ${extra}/>`;
    if (e.forma === 'losango') return `<path d="M${x} ${y - r * 1.25}L${x + r * 1.25} ${y}L${x} ${y + r * 1.25}L${x - r * 1.25} ${y}Z" ${extra}/>`;
    if (e.forma === 'triangulo') return `<path d="M${x} ${y - r * 1.2}L${x + r * 1.15} ${y + r * 0.85}L${x - r * 1.15} ${y + r * 0.85}Z" ${extra}/>`;
    return `<circle cx="${x}" cy="${y}" r="${r}" ${extra}/>`;
  }

  /** Miniatura da forma e cor de uma classe, para a legenda. */
  function amostra(classe) {
    const e = { ...estilo(classe), raio: 6 };
    return `<svg width="18" height="18" viewBox="-9 -9 18 18" aria-hidden="true" focusable="false">${formaSvg(e, 0, 0, `fill="${e.cor}"`)}</svg>`;
  }

  /**
   * @param {object} opcoes
   *   containerId, grafo {nos, arestas}, classesOcultas (Set), selecionadoId, onSelecionar(id)
   */
  function desenhar({ containerId, grafo, classesOcultas, selecionadoId, onSelecionar }) {
    const container = document.getElementById(containerId);
    if (!container || !grafo) return;
    const ocultas = classesOcultas || new Set();

    // O leiaute considera o grafo inteiro: esconder uma classe não reposiciona o resto.
    const assinatura = grafo.nos.map(n => n.id).join('|') + '#' + grafo.arestas.length;
    if (!leiautes[assinatura]) leiautes[assinatura] = calcularLeiaute(grafo.nos, grafo.arestas);
    const coords = leiautes[assinatura];

    const nos = grafo.nos.filter(n => !ocultas.has(n.classe));
    const visiveis = new Set(nos.map(n => n.id));
    const arestas = grafo.arestas.filter(a => visiveis.has(a.origem) && visiveis.has(a.destino));

    if (!nos.length) {
      container.innerHTML = `<p class="t-pequeno" style="padding:24px; color:var(--soft);">Nenhum nó para mostrar com as classes escolhidas.</p>`;
      return;
    }

    const vizinhos = new Set();
    if (selecionadoId && visiveis.has(selecionadoId)) {
      vizinhos.add(selecionadoId);
      arestas.forEach(a => {
        if (a.origem === selecionadoId) vizinhos.add(a.destino);
        if (a.destino === selecionadoId) vizinhos.add(a.origem);
      });
    }
    const haFoco = vizinhos.size > 0;

    const chaveVista = assinatura + '#' + [...ocultas].sort().join(',');
    let vista = vistas[containerId];
    if (!vista || vista.chave !== chaveVista) {
      vista = { chave: chaveVista, ...enquadrar(coords, nos.map(n => n.id)) };
      vistas[containerId] = vista;
    }

    const linhas = arestas.map(a => {
      const o = coords[a.origem], d = coords[a.destino];
      const ativa = haFoco && (a.origem === selecionadoId || a.destino === selecionadoId);
      const opac = haFoco ? (ativa ? 0.95 : 0.12) : 0.45;
      return `<line x1="${o.x.toFixed(1)}" y1="${o.y.toFixed(1)}" x2="${d.x.toFixed(1)}" y2="${d.y.toFixed(1)}"
        stroke="${ativa ? 'var(--ink)' : 'var(--line-strong)'}" stroke-width="${ativa ? 1.8 : 1.1}" stroke-opacity="${opac}"
        ${a.inferida ? 'stroke-dasharray="4 3"' : ''} vector-effect="non-scaling-stroke"><title>${esc(a.rotulo)}${a.inferida ? ' (inferida)' : ''}</title></line>`;
    }).join('');

    const rotulosAresta = haFoco ? arestas.filter(a => a.origem === selecionadoId || a.destino === selecionadoId).map(a => {
      const o = coords[a.origem], d = coords[a.destino];
      return `<text class="grafo-rotulo-aresta" x="${((o.x + d.x) / 2).toFixed(1)}" y="${((o.y + d.y) / 2).toFixed(1)}" text-anchor="middle">${esc(a.rotulo)}</text>`;
    }).join('') : '';

    const pontos = nos.map(n => {
      const e = estilo(n.classe);
      const p = coords[n.id];
      const sel = n.id === selecionadoId;
      const apagado = haFoco && !vizinhos.has(n.id);
      const urg = n.atributos && n.atributos._urgencia;
      const anel = sel ? 'stroke="var(--ink)" stroke-width="3"'
        : urg === 'CRITICA' ? 'stroke="var(--critico)" stroke-width="3"'
        : urg === 'ELEVADA' ? 'stroke="var(--atencao)" stroke-width="3"'
        : 'stroke="var(--surface)" stroke-width="1.5"';
      const mostrarRotulo = e.rotular || sel || (haFoco && vizinhos.has(n.id));
      return `<g class="grafo-no" data-id="${esc(n.id)}" role="button" tabindex="0" aria-pressed="${sel}"
          aria-label="${esc(n.rotulo)}" opacity="${apagado ? 0.25 : 1}">
        <title>${esc(n.rotulo)}</title>
        ${formaSvg(e, +p.x.toFixed(1), +p.y.toFixed(1), `fill="${e.cor}" ${anel}`)}
        ${mostrarRotulo ? `<text class="grafo-rotulo" x="${p.x.toFixed(1)}" y="${(p.y + e.raio * 1.25 + 3).toFixed(1)}" text-anchor="middle" dominant-baseline="hanging">${esc(n.rotulo)}</text>` : ''}
      </g>`;
    }).join('');

    container.innerHTML = `
      <svg class="grafo-svg" viewBox="${vista.x} ${vista.y} ${vista.w} ${vista.h}" preserveAspectRatio="xMidYMid meet"
           role="group" aria-label="Grafo de conhecimento com ${nos.length} nós e ${arestas.length} relações">
        <g>${linhas}</g><g>${rotulosAresta}</g><g>${pontos}</g>
      </svg>
      <div class="grafo-zoom">
        <button type="button" class="botao" data-zoom="0.8" aria-label="Aproximar">+</button>
        <button type="button" class="botao" data-zoom="1.25" aria-label="Afastar">−</button>
        <button type="button" class="botao" data-zoom="0" aria-label="Enquadrar o grafo inteiro">Enquadrar</button>
      </div>`;

    const svg = container.querySelector('svg');
    // Os rótulos mantêm o tamanho na tela (11 px) em qualquer zoom: --k converte px em unidades do viewBox.
    const aplicarVista = () => {
      svg.setAttribute('viewBox', `${vista.x} ${vista.y} ${vista.w} ${vista.h}`);
      const largura = svg.clientWidth || LARGURA;
      const altura = svg.clientHeight || ALTURA;
      svg.style.setProperty('--k', Math.max(vista.w / largura, vista.h / altura).toFixed(3));
    };
    aplicarVista();

    function zoom(fator, cx, cy) {
      const nx = cx ?? vista.x + vista.w / 2;
      const ny = cy ?? vista.y + vista.h / 2;
      const w = Math.min(Math.max(vista.w * fator, 120), 12000);
      const real = w / vista.w;
      vista.x = nx - (nx - vista.x) * real;
      vista.y = ny - (ny - vista.y) * real;
      vista.w = w;
      vista.h *= real;
      aplicarVista();
    }

    container.querySelectorAll('[data-zoom]').forEach(b => b.addEventListener('click', () => {
      const f = Number(b.dataset.zoom);
      if (f === 0) { Object.assign(vista, enquadrar(coords, nos.map(n => n.id))); aplicarVista(); } else zoom(f);
    }));

    svg.addEventListener('wheel', ev => {
      ev.preventDefault();
      const r = svg.getBoundingClientRect();
      const cx = vista.x + ((ev.clientX - r.left) / r.width) * vista.w;
      const cy = vista.y + ((ev.clientY - r.top) / r.height) * vista.h;
      zoom(ev.deltaY > 0 ? 1.15 : 0.87, cx, cy);
    }, { passive: false });

    // Arrastar o fundo move a vista; um clique sem arrasto num nó seleciona.
    let arrasto = null;
    svg.addEventListener('pointerdown', ev => {
      arrasto = { x: ev.clientX, y: ev.clientY, vx: vista.x, vy: vista.y, moveu: false };
    });
    svg.addEventListener('pointermove', ev => {
      if (!arrasto) return;
      const dx = ev.clientX - arrasto.x, dy = ev.clientY - arrasto.y;
      if (Math.abs(dx) + Math.abs(dy) > 4) arrasto.moveu = true;
      if (!arrasto.moveu) return;
      const r = svg.getBoundingClientRect();
      vista.x = arrasto.vx - dx * (vista.w / r.width);
      vista.y = arrasto.vy - dy * (vista.h / r.height);
      aplicarVista();
    });
    const soltar = ev => {
      const foiArrasto = arrasto && arrasto.moveu;
      arrasto = null;
      if (foiArrasto || ev.type !== 'pointerup') return;
      const g = ev.target.closest && ev.target.closest('.grafo-no');
      if (g && onSelecionar) onSelecionar(g.dataset.id === selecionadoId ? null : g.dataset.id);
    };
    svg.addEventListener('pointerup', soltar);
    svg.addEventListener('pointerleave', soltar);
    svg.addEventListener('keydown', ev => {
      if (ev.key !== 'Enter' && ev.key !== ' ') return;
      const g = ev.target.closest && ev.target.closest('.grafo-no');
      if (!g || !onSelecionar) return;
      ev.preventDefault();
      onSelecionar(g.dataset.id === selecionadoId ? null : g.dataset.id);
    });
  }

  return { desenhar, amostra, estilo };
})();
