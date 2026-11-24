/**
 * SaneaMapaGeo — Motor de mapas interativos com Leaflet + OpenStreetMap + Nominatim.
 * Não requer chave de API externa.
 */
window.SaneaMapaGeo = (function () {
  let leafletPromise = null;

  // bindPopup/bindTooltip interpretam HTML: todo texto vindo do servidor passa por aqui.
  const esc = s => String(s ?? '').replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m]));

  function calcularDistanciaMetros(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
      + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180)
      * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
  }

  function carregarScript(src) {
    return new Promise((resolve, reject) => {
      const s = document.createElement('script');
      s.src = src;
      s.async = true;
      s.onload = resolve;
      s.onerror = () => { s.remove(); reject(new Error('Falha ao carregar ' + src)); };
      document.head.appendChild(s);
    });
  }

  function carregarCss(href) {
    if (document.querySelector(`link[href="${href}"]`)) return;
    const l = document.createElement('link');
    l.rel = 'stylesheet';
    l.href = href;
    document.head.appendChild(l);
  }

  async function garantirMapa() {
    if (window.L) return window.L;
    if (!leafletPromise) {
      leafletPromise = (async () => {
        carregarCss('https://unpkg.com/leaflet@1.9.4/dist/leaflet.css');
        await carregarScript('https://unpkg.com/leaflet@1.9.4/dist/leaflet.js');
        if (!window.L) throw new Error('Leaflet não carregou');
        return window.L;
      })().catch(err => {
        leafletPromise = null; // permite tentar de novo quando a internet voltar
        throw err;
      });
    }
    return leafletPromise;
  }

  // Um mapa por contêiner: ao redesenhar a tela, o mapa anterior é desmontado (map.remove)
  // para não acumular camadas, ouvintes e tiles na memória ao longo do dia.
  const mapasAtivos = {};

  function novoMapa(L, container, containerId) {
    if (mapasAtivos[containerId]) {
      try { mapasAtivos[containerId].remove(); } catch (e) { /* contêiner já saiu da página */ }
      delete mapasAtivos[containerId];
    }
    container.innerHTML = '';
    const map = L.map(container);
    mapasAtivos[containerId] = map;
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap'
    }).addTo(map);
    return map;
  }

  /** Carrega o Leaflet; se falhar (sem internet), escreve uma alternativa no lugar do mapa e devolve null. */
  async function leafletOuAviso(container, alternativa) {
    try {
      return await garantirMapa();
    } catch (e) {
      container.innerHTML = `<div class="mapa-indisponivel" role="status">Mapa indisponível sem internet. ${esc(alternativa || '')}</div>`;
      return null;
    }
  }

  async function geocodificarReverso(lat, lng) {
    try {
      const resp = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${encodeURIComponent(lat)}&lon=${encodeURIComponent(lng)}&accept-language=pt-BR`
      );
      if (resp.ok) {
        const data = await resp.json();
        const a = data.address || {};
        return {
          logradouro: a.road || a.pedestrian || a.residential || a.footway || '',
          bairro: a.suburb || a.neighbourhood || a.city_district || a.town || '',
          enderecoCompleto: data.display_name || ''
        };
      }
    } catch (e) { /* sem internet: o morador preenche rua e bairro à mão */ }
    return { logradouro: '', bairro: '', enderecoCompleto: '' };
  }

  async function buscarEndereco(consulta) {
    if (!consulta || !consulta.trim()) return [];
    try {
      const q = consulta.toLowerCase().includes('macei') ? consulta : `${consulta}, Maceió, Alagoas`;
      const resp = await fetch(
        `https://nominatim.openstreetmap.org/search?format=jsonv2&q=${encodeURIComponent(q)}&limit=5&countrycodes=br&accept-language=pt-BR`
      );
      if (resp.ok) {
        const lista = await resp.json();
        return lista.map(item => ({
          lat: parseFloat(item.lat),
          lng: parseFloat(item.lon),
          rotulo: item.display_name
        }));
      }
    } catch (e) { /* sem internet: a tela mostra "nenhum endereço encontrado" */ }
    return [];
  }

  /**
   * Seletor do morador. Sem lat/lng, o mapa abre em Maceió SEM marcador: o local só existe depois
   * de uma ação (GPS, busca ou toque). onSelect recebe { origem: 'usuario' | 'programa' }.
   */
  async function criarSeletorLocalizacao({ containerId, lat, lng, onSelect }) {
    const container = document.getElementById(containerId);
    if (!container) return null;
    const L = await leafletOuAviso(container, 'Use o botão do GPS e preencha rua, bairro e ponto de referência.');
    if (!L) return null;

    const temPosicao = lat != null && lng != null && !Number.isNaN(Number(lat)) && !Number.isNaN(Number(lng));
    const curLat = temPosicao ? Number(lat) : -9.66599;
    const curLng = temPosicao ? Number(lng) : -35.73500;

    async function notificarMovimento(novaLat, novaLng, resolver, origem) {
      const latFix = Number(novaLat.toFixed(6));
      const lngFix = Number(novaLng.toFixed(6));
      const info = resolver ? await geocodificarReverso(latFix, lngFix) : { logradouro: '', bairro: '', enderecoCompleto: '' };
      if (typeof onSelect === 'function') {
        onSelect({ lat: latFix, lng: lngFix, origem, ...info });
      }
    }

    const map = novoMapa(L, container, containerId);
    map.setView([curLat, curLng], temPosicao ? 16 : 13);
    let marker = null;

    function posicionarMarcador(latLng) {
      if (!marker) {
        marker = L.marker(latLng, { draggable: true, keyboard: true, title: 'Local do problema' }).addTo(map);
        marker.on('dragend', () => {
          const p = marker.getLatLng();
          notificarMovimento(p.lat, p.lng, true, 'usuario');
        });
      } else {
        marker.setLatLng(latLng);
      }
    }

    if (temPosicao) posicionarMarcador([curLat, curLng]);

    map.on('click', ev => {
      posicionarMarcador(ev.latlng);
      map.panTo(ev.latlng);
      notificarMovimento(ev.latlng.lat, ev.latlng.lng, true, 'usuario');
    });

    setTimeout(() => map.invalidateSize(), 150);

    return {
      setPosition(nLat, nLng, resolver = true) {
        const latNum = Number(nLat);
        const lngNum = Number(nLng);
        posicionarMarcador([latNum, lngNum]);
        map.setView([latNum, lngNum], 17);
        notificarMovimento(latNum, lngNum, resolver, 'programa');
      }
    };
  }

  // RS008: a posição do fiscal só vem do GPS (setFiscalPosition). O clique no mapa serve apenas
  // para marcar a posição corrigida do ponto (RF007^2), quando o fiscal liga essa opção.
  async function criarMapaFiscal({ containerId, pontoLat, pontoLng, fiscalLat, fiscalLng, correcaoLat, correcaoLng, onCliqueMapa }) {
    const container = document.getElementById(containerId);
    if (!container) return null;
    const L = await leafletOuAviso(container, 'A distância continua sendo conferida pelo GPS ao salvar.');
    if (!L) return null;
    const pLat = Number(pontoLat);
    const pLng = Number(pontoLng);

    const map = novoMapa(L, container, containerId);
    map.setView([pLat, pLng], 17);

    L.marker([pLat, pLng], { title: 'Local informado pelo cidadão' }).addTo(map).bindPopup('Local informado pelo cidadão');

    const circulo = L.circle([pLat, pLng], {
      radius: 100,
      color: '#64748b',
      fillOpacity: 0.14
    }).addTo(map);

    let fiscalMarker = null;
    let correcaoMarker = null;

    function posicionarFiscal(latNum, lngNum) {
      if (!fiscalMarker) {
        fiscalMarker = L.circleMarker([latNum, lngNum], {
          radius: 9,
          color: '#ffffff',
          weight: 2,
          fillColor: '#1d4ed8',
          fillOpacity: 1
        }).addTo(map).bindPopup('Sua posição pelo GPS');
      } else {
        fiscalMarker.setLatLng([latNum, lngNum]);
      }
      const d = calcularDistanciaMetros(pLat, pLng, latNum, lngNum);
      circulo.setStyle({ color: d <= 100 ? '#059669' : '#b91c1c' });
    }

    function posicionarCorrecao(latVal, lngVal) {
      if (latVal == null || lngVal == null) {
        if (correcaoMarker) { correcaoMarker.remove(); correcaoMarker = null; }
        return;
      }
      const ll = [Number(latVal), Number(lngVal)];
      if (!correcaoMarker) {
        correcaoMarker = L.circleMarker(ll, {
          radius: 8, color: '#ffffff', weight: 2, fillColor: '#f59e0b', fillOpacity: 1
        }).addTo(map).bindPopup('Nova posição do ponto');
      } else {
        correcaoMarker.setLatLng(ll);
      }
    }

    if (fiscalLat != null && fiscalLng != null) {
      posicionarFiscal(Number(fiscalLat), Number(fiscalLng));
    }
    posicionarCorrecao(correcaoLat, correcaoLng);

    if (typeof onCliqueMapa === 'function') {
      map.on('click', ev => onCliqueMapa(Number(ev.latlng.lat.toFixed(6)), Number(ev.latlng.lng.toFixed(6))));
    }

    setTimeout(() => map.invalidateSize(), 150);

    return {
      setFiscalPosition(latVal, lngVal) {
        const latNum = Number(latVal);
        const lngNum = Number(lngVal);
        posicionarFiscal(latNum, lngNum);
        map.panTo([latNum, lngNum]);
      },
      setCorrecao(latVal, lngVal) {
        posicionarCorrecao(latVal, lngVal);
      }
    };
  }

  const Tipos = () => window.SaneaMapaTipos;

  // RF010/RF006: cor e ícone dizem o tipo; a borda (e o "!") diz a urgência.
  function iconePonto(L, p, selecionado) {
    const tipo = Tipos() ? Tipos().porId(p.tipoOcorrencia) : { cor: '#1f2937' };
    const urg = p.nivelUrgencia || 'NORMAL';
    const tam = urg === 'CRITICA' ? 36 : 30;
    const svg = Tipos() ? Tipos().icone(p.tipoOcorrencia, 16, '#ffffff') : '';
    return L.divIcon({
      className: '',
      html: `<span class="marcador-ponto" data-urgencia="${esc(urg)}" data-selecionado="${selecionado}" style="--cor:${esc(tipo.cor)}">${svg}</span>`,
      iconSize: [tam, tam],
      iconAnchor: [tam / 2, tam / 2]
    });
  }

  function adicionarLegenda(L, map, camadasAtivas) {
    const T = Tipos();
    if (!T) return;
    const legenda = L.control({ position: 'bottomright' });
    legenda.onAdd = () => {
      const div = L.DomUtil.create('div', 'legenda-mapa');
      div.setAttribute('role', 'region');
      div.setAttribute('aria-label', 'Legenda do mapa');
      let html = '';
      if (camadasAtivas.pontos) {
        html += '<b>Tipo (cor e ícone)</b>' + T.TIPOS.map(t =>
          `<span class="item"><span class="bola" style="background:${t.cor}">${T.icone(t.id, 12, '#ffffff')}</span>${esc(t.nome)}</span>`).join('');
        html += '<b>Urgência (borda)</b>' + T.URGENCIAS.map(u =>
          `<span class="item"><span class="anel" style="border:3px solid ${u.id === 'NORMAL' ? '#ffffff' : u.cor}; box-shadow:0 0 0 1px #94a3b8;"></span>${esc(u.nome)}${u.id === 'CRITICA' ? ' (com “!”)' : ''}</span>`).join('');
      }
      if (camadasAtivas.vuln) html += '<span class="item"><span class="anel" style="background:#d97706;opacity:.5"></span>Área vulnerável</span>';
      if (camadasAtivas.censo) html += '<span class="item"><span class="anel" style="background:#0f766e;opacity:.5"></span>Setor do Censo (mais escuro = menos rede)</span>';
      if (camadasAtivas.calor) html += '<span class="item"><span class="anel" style="background:#890028;opacity:.35"></span>Concentração de relatos</span>';
      div.innerHTML = html;
      return div;
    };
    legenda.addTo(map);
  }

  // Centro e zoom do mapa montado no contêiner, para a atualização periódica do painel não mover a vista.
  function vistaAtual(containerId) {
    const map = mapasAtivos[containerId];
    if (!map) return null;
    try {
      const c = map.getCenter();
      return { lat: c.lat, lng: c.lng, zoom: map.getZoom() };
    } catch (e) {
      return null;
    }
  }

  async function criarMapaPrefeitura({ containerId, pontos, camadasVuln, setoresCenso, camadasAtivas, selectedId, onSelectPonto, vista }) {
    const container = document.getElementById(containerId);
    if (!container) return null;
    const L = await leafletOuAviso(container, 'A fila abaixo continua disponível.');
    if (!L) return null;
    const centro = { lat: -9.655, lng: -35.735 };

    const map = novoMapa(L, container, containerId);
    map.setView([centro.lat, centro.lng], 12);

    if (camadasAtivas.vuln) {
      (camadasVuln || []).filter(c => c.ativa).forEach(c => {
        if (c.tipoGeometria === 'PONTO_RAIO' && c.centroLatitude && c.centroLongitude) {
          L.circle([Number(c.centroLatitude), Number(c.centroLongitude)], {
            radius: Number(c.raioMetros || 150),
            color: '#b45309',
            fillColor: '#d97706',
            fillOpacity: 0.16,
            weight: 1.5
          }).addTo(map).bindPopup(`${esc(c.nome)} (peso ${esc(c.pesoUrgencia)})`);
        } else if (c.tipoGeometria === 'POLIGONO' && c.poligonoCoordenadas) {
          try {
            const coords = JSON.parse(c.poligonoCoordenadas);
            L.polygon(coords, {
              color: '#92400e',
              fillColor: '#d97706',
              fillOpacity: 0.14,
              weight: 1.5
            }).addTo(map).bindPopup(`${esc(c.nome)} (peso ${esc(c.pesoUrgencia)})`);
          } catch (e) {
            console.warn('Camada com polígono inválido ignorada no mapa:', c.nome, e);
          }
        }
      });
    }

    if (camadasAtivas.calor) {
      (pontos || []).forEach(p => {
        if (!p.latitudeAproximada || !p.longitudeAproximada) return;
        L.circle([Number(p.latitudeAproximada), Number(p.longitudeAproximada)], {
          radius: 180 + (Number(p.quantidadeRelatos || 1) * 70),
          stroke: false,
          fillColor: '#890028',
          fillOpacity: 0.25
        }).addTo(map);
      });
    }

    // RF020: cada setor é desenhado pelo seu polígono real ([[lat, lon], ...]).
    if (camadasAtivas.censo) {
      (setoresCenso || []).forEach(s => {
        let coords;
        try {
          coords = JSON.parse(s.poligonoCoordenadas);
        } catch (e) {
          console.warn('Setor do Censo com polígono inválido ignorado no mapa:', s.codigoSetor, e);
          return;
        }
        const menorCobertura = Math.min(Number(s.pctRedeAgua ?? 100), Number(s.pctRedeEsgoto ?? 100));
        L.polygon(coords, {
          color: '#0f766e',
          weight: 1.5,
          fillColor: '#0f766e',
          fillOpacity: 0.08 + (100 - menorCobertura) / 100 * 0.45
        }).addTo(map).bindPopup(
          `Setor ${esc(s.codigoSetor)} (${esc(s.bairro)})<br>Água: ${esc(s.pctRedeAgua)}% · Esgoto: ${esc(s.pctRedeEsgoto)}% dos domicílios na rede geral`);
      });
    }

    const coordsPontos = [];
    if (camadasAtivas.pontos) {
      (pontos || []).forEach(p => {
        if (!p.latitudeAproximada || !p.longitudeAproximada) return;
        const lat = Number(p.latitudeAproximada);
        const lng = Number(p.longitudeAproximada);
        coordsPontos.push([lat, lng]);
        const sel = p.id === selectedId;
        const tipoNome = Tipos() ? Tipos().porId(p.tipoOcorrencia).nome : p.tipoRotulo;
        const m = L.marker([lat, lng], {
          icon: iconePonto(L, p, sel),
          keyboard: true,
          title: `${p.codigoExibicao} · ${tipoNome} · urgência ${p.nivelRotulo || ''}`,
          zIndexOffset: p.nivelUrgencia === 'CRITICA' ? 500 : 0
        }).addTo(map);
        m.bindTooltip(`${esc(p.codigoExibicao)} · ${esc(tipoNome)} · ${esc(p.quantidadeRelatos)} relato(s) · urgência ${esc(p.nivelRotulo)}`);
        m.on('click', () => onSelectPonto && onSelectPonto(p.id));
      });
    }

    adicionarLegenda(L, map, camadasAtivas);

    if (vista) {
      map.setView([vista.lat, vista.lng], vista.zoom);
    } else if (coordsPontos.length === 1) {
      map.setView(coordsPontos[0], 15);
    } else if (coordsPontos.length > 1) {
      map.fitBounds(coordsPontos, { padding: [36, 36], maxZoom: 15 });
    }

    setTimeout(() => map.invalidateSize(), 150);
    return { engine: 'openstreetmap' };
  }

  function inicializarLightboxFotos() {
    if (window.__saneaLightboxInit) return;
    window.__saneaLightboxInit = true;

    const modal = document.createElement('div');
    modal.id = 'sanea-lightbox';
    modal.className = 'sanea-lightbox';
    modal.hidden = true;
    modal.innerHTML = `
      <div class="sanea-lightbox-box" role="dialog" aria-modal="true" aria-label="Visualização ampliada da imagem">
        <div class="sanea-lightbox-bar">
          <span id="sanea-lightbox-legenda">Visualização ampliada</span>
          <button type="button" class="sanea-lightbox-fechar" id="sanea-lightbox-fechar">Fechar (Esc)</button>
        </div>
        <img id="sanea-lightbox-img" class="sanea-lightbox-img" src="" alt="Foto ampliada">
      </div>`;
    document.body.appendChild(modal);

    const imgEl = modal.querySelector('#sanea-lightbox-img');
    const legendaEl = modal.querySelector('#sanea-lightbox-legenda');
    const fecharBtn = modal.querySelector('#sanea-lightbox-fechar');

    function fechar() {
      modal.hidden = true;
      imgEl.src = '';
    }

    modal.addEventListener('click', ev => {
      if (ev.target === modal || ev.target === fecharBtn) fechar();
    });

    document.addEventListener('keydown', ev => {
      if (ev.key === 'Escape' && !modal.hidden) fechar();
    });

    document.addEventListener('click', ev => {
      const target = ev.target.closest('img.midia-preview-img');
      if (!target || !target.src) return;
      imgEl.src = target.src;
      legendaEl.textContent = target.alt || 'Foto ampliada';
      modal.hidden = false;
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', inicializarLightboxFotos);
  } else {
    inicializarLightboxFotos();
  }

  return {
    garantirMapa,
    geocodificarReverso,
    buscarEndereco,
    calcularDistanciaMetros,
    criarSeletorLocalizacao,
    criarMapaFiscal,
    criarMapaPrefeitura,
    vistaAtual
  };
})();
