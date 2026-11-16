/**
 * SaneaMapaAvisos — mostra ao usuário os erros que nenhuma tela tratou (RNF03).
 * Ações dos botões que falham sem try/catch viram promessas rejeitadas; aqui elas
 * aparecem como um aviso no topo da tela em vez de sumirem em silêncio.
 * Erros marcados com `tratado = true` (ex.: sessão expirada, que já leva ao login) são ignorados.
 */
window.SaneaMapaAvisos = (function () {
  let timer = null;

  function mostrarErro(mensagem) {
    let el = document.getElementById('aviso-global');
    if (!el) {
      el = document.createElement('div');
      el.id = 'aviso-global';
      el.className = 'chamada aviso-global';
      el.dataset.tom = 'critico';
      el.setAttribute('role', 'alert');
      document.body.appendChild(el);
    }
    el.innerHTML = '';
    const rotulo = document.createElement('span');
    rotulo.className = 'rotulo';
    rotulo.textContent = 'Não foi possível concluir a ação';
    const texto = document.createElement('span');
    texto.className = 't-pequeno';
    texto.textContent = mensagem || 'Falha na operação. Tente de novo.';
    const fechar = document.createElement('button');
    fechar.type = 'button';
    fechar.className = 'botao';
    fechar.textContent = 'Fechar';
    fechar.onclick = () => { el.hidden = true; };
    el.append(rotulo, texto, fechar);
    el.hidden = false;
    clearTimeout(timer);
    timer = setTimeout(() => { el.hidden = true; }, 10000);
  }

  window.addEventListener('unhandledrejection', ev => {
    const err = ev.reason;
    if (err && err.tratado) return;
    const msg = err instanceof TypeError
      ? 'Sem conexão com o servidor. Verifique a internet e tente de novo.'
      : (err && err.message);
    mostrarErro(msg);
  });

  return { mostrarErro };
})();
