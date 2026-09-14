// Cliente asincrono de la mini aplicacion web.
// Toda comunicacion con el servidor pasa por fetch(); la pagina nunca
// se recarga y permanece interactiva mientras espera la respuesta.

const resultEl = document.getElementById('result');
const errorEl = document.getElementById('error');

function showLoading(message) {
  resultEl.textContent = '';
  errorEl.textContent = '';
  resultEl.innerHTML = `<span class="loading">${message}</span>`;
}

function showResult(message) {
  errorEl.textContent = '';
  resultEl.textContent = message;
}

function showError(message) {
  resultEl.textContent = '';
  errorEl.textContent = message;
}

function setButtonsDisabled(disabled, ...buttons) {
  buttons.forEach((b) => { if (b) b.disabled = disabled; });
}

/**
 * Calls a service URL, distinguishing three outcomes:
 *  1) a successful HTTP response (status < 400) with usable JSON,
 *  2) a valid HTTP response that reports a client/server error,
 *  3) a network failure (server unreachable, DNS error, CORS, etc).
 */
async function callService(url) {
  let response;
  try {
    response = await fetch(url, { method: 'GET' });
  } catch (networkError) {
    // The request never reached the server (or never came back).
    throw new Error('No se pudo contactar al servidor. Revisa tu conexion.');
  }

  let body = null;
  try {
    body = await response.json();
  } catch (parseError) {
    // Body wasn't JSON (e.g. a raw "404 Not Found" text response).
    body = null;
  }

  if (!response.ok) {
    const message = body && body.error ? body.error : `Error del servidor (HTTP ${response.status}).`;
    throw new Error(message);
  }

  return body;
}

// --- Greeting service ---------------------------------------------------

document.getElementById('greet-form').addEventListener('submit', async (event) => {
  event.preventDefault(); // never let the browser reload the page
  const button = event.submitter;
  const name = document.getElementById('name').value.trim();

  if (!name) {
    showError('Escribe un nombre antes de saludar.');
    return;
  }

  setButtonsDisabled(true, button);
  showLoading('Enviando saludo...');
  try {
    const data = await callService(`/api/greet?name=${encodeURIComponent(name)}`);
    showResult(data.greeting);
  } catch (err) {
    showError(err.message);
  } finally {
    setButtonsDisabled(false, button);
  }
});

// --- Square service -------------------------------------------------------

document.getElementById('square-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = event.submitter;
  const raw = document.getElementById('value').value;

  if (raw === '' || Number.isNaN(Number(raw))) {
    showError('Escribe un numero valido.');
    return;
  }

  setButtonsDisabled(true, button);
  showLoading('Calculando...');
  try {
    const data = await callService(`/api/square?value=${encodeURIComponent(raw)}`);
    showResult(`${data.input}^2 = ${data.square}`);
  } catch (err) {
    showError(err.message);
  } finally {
    setButtonsDisabled(false, button);
  }
});

// --- Server time service --------------------------------------------------

document.getElementById('time-btn').addEventListener('click', async (event) => {
  const button = event.currentTarget;
  setButtonsDisabled(true, button);
  showLoading('Consultando hora del servidor...');
  try {
    const data = await callService('/api/time');
    showResult(`Hora del servidor: ${data.serverTime}`);
  } catch (err) {
    showError(err.message);
  } finally {
    setButtonsDisabled(false, button);
  }
});

// --- Health service ---------------------------------------------------

document.getElementById('health-btn').addEventListener('click', async (event) => {
  const button = event.currentTarget;
  setButtonsDisabled(true, button);
  showLoading('Verificando salud del servidor...');
  try {
    const data = await callService('/api/health');
    showResult(`Estado del servidor: ${data.status}`);
  } catch (err) {
    showError(err.message);
  } finally {
    setButtonsDisabled(false, button);
  }
});
