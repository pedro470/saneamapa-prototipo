/**
 * SaneaMapaA11y — apoio de acessibilidade (RNF04) para telas que se redesenham com innerHTML.
 *
 * - anunciar(texto): escreve numa região viva permanente (aria-live), para o leitor de tela
 *   ouvir protocolo, sucesso e erros mesmo quando a tela inteira é recriada.
 * - redesenhar(raiz, html): troca o conteúdo e devolve o foco ao mesmo controle de antes
 *   (identificado por id, name, aria-label ou texto), em vez de perdê-lo para o <body>.
 * - focarTitulo(raiz): leva o foco ao primeiro título da tela, ao mudar de passo ou de aba.
 */
window.SaneaMapaA11y = (function () {
  function regiao(politeness) {
    const id = 'sanea-anuncio-' + politeness;
    let el = document.getElementById(id);
    if (!el) {
      el = document.createElement('div');
      el.id = id;
      el.className = 'somente-leitor';
      el.setAttribute('aria-live', politeness);
      el.setAttribute('role', politeness === 'assertive' ? 'alert' : 'status');
      document.body.appendChild(el);
    }
    return el;
  }

  function anunciar(texto, urgente = false) {
    if (!texto) return;
    const el = regiao(urgente ? 'assertive' : 'polite');
    el.textContent = '';
    // Pequeno atraso para o leitor de tela perceber a troca mesmo com o mesmo texto.
    setTimeout(() => { el.textContent = texto; }, 60);
  }

  function chaveDe(el) {
    if (!el || el === document.body) return null;
    if (el.id) return { id: el.id };
    const tag = el.tagName;
    const assinatura = el.getAttribute('name') || el.getAttribute('aria-label') || (el.textContent || '').trim().slice(0, 60);
    return { tag, assinatura };
  }

  function candidatos(raiz, chave) {
    return Array.from(raiz.querySelectorAll(chave.tag)).filter(el =>
      (el.getAttribute('name') || el.getAttribute('aria-label') || (el.textContent || '').trim().slice(0, 60)) === chave.assinatura);
  }

  function redesenhar(raiz, html) {
    const ativo = document.activeElement;
    const dentro = ativo && raiz.contains(ativo);
    const chave = dentro ? chaveDe(ativo) : null;
    let indice = 0;
    let selecao = null;
    if (chave && !chave.id) indice = candidatos(raiz, chave).indexOf(ativo);
    if (dentro && typeof ativo.selectionStart === 'number') {
      try { selecao = [ativo.selectionStart, ativo.selectionEnd]; } catch (e) { selecao = null; }
    }

    raiz.innerHTML = html;

    if (!chave) return;
    const alvo = chave.id ? document.getElementById(chave.id) : candidatos(raiz, chave)[Math.max(0, indice)];
    if (alvo && typeof alvo.focus === 'function') {
      alvo.focus({ preventScroll: true });
      if (selecao && typeof alvo.setSelectionRange === 'function') {
        try { alvo.setSelectionRange(selecao[0], selecao[1]); } catch (e) { /* campo sem seleção de texto */ }
      }
    }
  }

  function focarTitulo(raiz) {
    const h = (raiz || document).querySelector('h1, h2');
    if (!h) return;
    if (!h.hasAttribute('tabindex')) h.setAttribute('tabindex', '-1');
    h.focus({ preventScroll: false });
  }

  return { anunciar, redesenhar, focarTitulo };
})();
