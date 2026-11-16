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
      s.onerror = reject;
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
        return window.L;
      })();
    }
    return leafletPromise;
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
    } catch (e) { /* silencioso */ }
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
    } catch (e) { /* silencioso */ }
    return [];
  }

  async function criarSeletorLocalizacao({ containerId, lat, lng, onSelect }) {
    const container = document.getElementById(containerId);
    if (!container) return null;
    const L = await garantirMapa();

    let curLat = Number(lat) || -9.66599;
    let curLng = Number(lng) || -35.73500;

    async function notificarMovimento(novaLat, novaLng, resolver = true) {
      curLat = Number(novaLat.toFixed(6));
      curLng = Number(novaLng.toFixed(6));
      const info = resolver ? await geocodificarReverso(curLat, curLng) : { logradouro: '', bairro: '', enderecoCompleto: '' };
      if (typeof onSelect === 'function') {
        onSelect({ lat: curLat, lng: curLng, ...info });
      }
    }

    container.innerHTML = '';
    const map = L.map(container).setView([curLat, curLng], 16);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap'
    }).addTo(map);
    const marker = L.marker([curLat, curLng], { draggable: true }).addTo(map);

    map.on('click', ev => {
      marker.setLatLng(ev.latlng);
      map.panTo(ev.latlng);
      notificarMovimento(ev.latlng.lat, ev.latlng.lng, true);
    });
    marker.on('dragend', () => {
      const p = marker.getLatLng();
      notificarMovimento(p.lat, p.lng, true);
    });

    setTimeout(() => map.invalidateSize(), 150);

    return {
      setPosition(nLat, nLng, resolver = true) {
        const latNum = Number(nLat);
        const lngNum = Number(nLng);
        marker.setLatLng([latNum, lngNum]);
        map.setView([latNum, lngNum], 17);
        notificarMovimento(latNum, lngNum, resolver);
      }
    };
  }

  // RS008: a posição do fiscal só vem do GPS (setFiscalPosition). O mapa não aceita clique para movê-la.
  async function criarMapaFiscal({ containerId, pontoLat, pontoLng, fiscalLat, fiscalLng }) {
    const container = document.getElementById(containerId);
    if (!container) return null;
    const L = await garantirMapa();
    const pLat = Number(pontoLat);
    const pLng = Number(pontoLng);

    container.innerHTML = '';
    const map = L.map(container).setView([pLat, pLng], 17);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap'
    }).addTo(map);

    L.marker([pLat, pLng]).addTo(map).bindPopup('Local informado pelo cidadão');

    const circulo = L.circle([pLat, pLng], {
      radius: 100,
      color: '#64748b',
      fillOpacity: 0.14
    }).addTo(map);

    let fiscalMarker = null;

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

    if (fiscalLat != null && fiscalLng != null) {
      posicionarFiscal(Number(fiscalLat), Number(fiscalLng));
    }

    setTimeout(() => map.invalidateSize(), 150);

    return {
      setFiscalPosition(latVal, lngVal) {
        const latNum = Number(latVal);
        const lngNum = Number(lngVal);
        posicionarFiscal(latNum, lngNum);
        map.panTo([latNum, lngNum]);
      }
    };
  }

  const COR_TIPO = {
    CASA_SEM_AGUA: '#2e51b9',
    AGUA_FORA_PADRAO: '#97570d',
    RUA_SEM_ESGOTO: '#5c33a8',
    ESGOTO_CEU_ABERTO: '#890028'
  };

  async function criarMapaPrefeitura({ containerId, pontos, camadasVuln, setoresCenso, camadasAtivas, selectedId, onSelectPonto }) {
    const container = document.getElementById(containerId);
    if (!container) return null;
    const L = await garantirMapa();
    const centro = { lat: -9.655, lng: -35.735 };

    container.innerHTML = '';
    const map = L.map(container).setView([centro.lat, centro.lng], 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap'
    }).addTo(map);

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
          } catch (e) {}
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

    if (camadasAtivas.censo) {
      (setoresCenso || []).forEach((s, idx) => {
        const pt = (pontos || []).find(p => (p.bairro || '').toLowerCase() === (s.bairro || '').toLowerCase());
        const lat = pt ? Number(pt.latitudeAproximada) : -9.63 - idx * 0.015;
        const lng = pt ? Number(pt.longitudeAproximada) : -35.73 + idx * 0.01;
        L.circleMarker([lat, lng], {
          radius: 9,
          color: '#0f766e',
          fillColor: '#14b8a6',
          fillOpacity: 0.6
        }).addTo(map).bindPopup(`Censo ${esc(s.bairro)}: Água ${esc(s.pctRedeAgua)}% · Esgoto ${esc(s.pctRedeEsgoto)}%`);
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
        const m = L.circleMarker([lat, lng], {
          radius: sel ? 13 : 10,
          color: sel ? '#f59e0b' : '#ffffff',
          weight: sel ? 3 : 2,
          fillColor: COR_TIPO[p.tipoOcorrencia] || '#1f2937',
          fillOpacity: 0.95
        }).addTo(map);
        m.bindTooltip(`${esc(p.codigoExibicao)} · ${esc(p.quantidadeRelatos)} relato(s)`);
        m.on('click', () => onSelectPonto && onSelectPonto(p.id));
      });
    }

    if (coordsPontos.length === 1) {
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
    criarMapaPrefeitura
  };
})();
