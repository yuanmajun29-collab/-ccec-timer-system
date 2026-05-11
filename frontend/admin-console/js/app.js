function esc(s) {
  return String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

let prPage = 0;
let auPage = 0;
const prSize = 20;

async function guardMe() {
  const r = await fetch('/api/v1/auth/me', { credentials: 'include' });
  if (!r.ok) {
    location.href = './login.html';
    return false;
  }
  const j = await r.json();
  document.getElementById('who').textContent = j.username || '—';
  return true;
}

async function guardMgmt(res) {
  if (res.status === 401 || res.status === 403) {
    location.href = './login.html';
    return false;
  }
  return true;
}

function showTab(name) {
  document.querySelectorAll('aside nav button').forEach((b) => {
    b.classList.toggle('active', b.dataset.tab === name);
  });
  ['stations', 'ct', 'alarms', 'production', 'audit'].forEach((t) => {
    document.getElementById('tab-' + t).classList.toggle('hidden', t !== name);
  });
}

async function loadStations() {
  const r = await fetch('/api/v1/management/stations', { credentials: 'include' });
  if (!(await guardMgmt(r))) return;
  const body = await r.json();
  const rows = body.data.items || [];
  const tb = document.querySelector('#st-table tbody');
  tb.innerHTML = rows.map((row) => `
    <tr>
      <td>${esc(row.stationCode)}</td>
      <td>${esc(row.stationName)}</td>
      <td>${esc(row.lineCode)}</td>
      <td>${esc(row.plcCode)}</td>
      <td>${row.enabled ? '<span class="tag">是</span>' : '<span class="tag">否</span>'}</td>
      <td><button type="button" class="js-edit-st" data-id="${row.id}">编辑</button></td>
    </tr>`).join('');
  tb.querySelectorAll('.js-edit-st').forEach((btn) => {
    btn.addEventListener('click', () => fillStation(rows.find((x) => String(x.id) === btn.dataset.id)));
  });
}

function fillStation(row) {
  document.getElementById('st-id').value = row.id;
  document.getElementById('st-code').value = row.stationCode || '';
  document.getElementById('st-name').value = row.stationName || '';
  document.getElementById('st-line').value = row.lineCode || '';
  document.getElementById('st-screen-code').value = row.screenCode || '';
  document.getElementById('st-screen-ip').value = row.screenIp || '';
  document.getElementById('st-plc').value = row.plcCode || '';
  document.getElementById('st-enabled').checked = !!row.enabled;
}

function clearStation() {
  document.getElementById('st-id').value = '';
  document.getElementById('st-code').value = '';
  document.getElementById('st-name').value = '';
  document.getElementById('st-line').value = '';
  document.getElementById('st-screen-code').value = '';
  document.getElementById('st-screen-ip').value = '';
  document.getElementById('st-plc').value = '';
  document.getElementById('st-enabled').checked = true;
}

async function saveStation() {
  const id = document.getElementById('st-id').value;
  const payload = {
    stationCode: document.getElementById('st-code').value.trim(),
    stationName: document.getElementById('st-name').value.trim(),
    lineCode: document.getElementById('st-line').value.trim(),
    screenCode: document.getElementById('st-screen-code').value.trim() || null,
    screenIp: document.getElementById('st-screen-ip').value.trim() || null,
    plcCode: document.getElementById('st-plc').value.trim(),
    enabled: document.getElementById('st-enabled').checked
  };
  const url = id ? `/api/v1/management/stations/${id}` : '/api/v1/management/stations';
  const method = id ? 'PUT' : 'POST';
  const r = await CcecApi.authFetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!(await guardMgmt(r))) return;
  if (!r.ok) {
    alert('保存失败');
    return;
  }
  clearStation();
  await loadStations();
}

async function loadCt() {
  const q = document.getElementById('ct-filter-station').value.trim();
  const url = q
    ? `/api/v1/management/ct-configs?stationCode=${encodeURIComponent(q)}`
    : '/api/v1/management/ct-configs';
  const r = await fetch(url, { credentials: 'include' });
  if (!(await guardMgmt(r))) return;
  const body = await r.json();
  const rows = body.data.items || [];
  const tb = document.querySelector('#ct-table tbody');
  tb.innerHTML = rows.map((row) => `
    <tr>
      <td>${esc(row.stationCode)}</td>
      <td>${esc(row.engineType || '—')}</td>
      <td>${esc(row.standardCt)}</td>
      <td>${esc(row.warnThreshold)}</td>
      <td>${esc(row.alarmThreshold)}</td>
      <td>${esc(row.effectiveTime)}</td>
      <td><button type="button" class="js-edit-ct" data-id="${row.id}">编辑</button></td>
    </tr>`).join('');
  tb.querySelectorAll('.js-edit-ct').forEach((btn) => {
    btn.addEventListener('click', () => fillCt(rows.find((x) => String(x.id) === btn.dataset.id)));
  });
}

function fillCt(row) {
  document.getElementById('ct-id').value = row.id;
  document.getElementById('ct-ver').value = row.configVersion || 'v1';
  document.getElementById('ct-engine').value = row.engineType || '';
  document.getElementById('ct-station').value = row.stationCode || '';
  document.getElementById('ct-sec').value = row.standardCt;
  document.getElementById('ct-warn').value = row.warnThreshold;
  document.getElementById('ct-alarm').value = row.alarmThreshold;
  document.getElementById('ct-sound').value = row.soundPolicy || '';
  document.getElementById('ct-eff').value = row.effectiveTime || '';
  document.getElementById('ct-enabled').checked = !!row.enabled;
}

function clearCt() {
  document.getElementById('ct-id').value = '';
  document.getElementById('ct-ver').value = 'v1';
  document.getElementById('ct-engine').value = '';
  document.getElementById('ct-station').value = '';
  document.getElementById('ct-sec').value = '300';
  document.getElementById('ct-warn').value = '0.7';
  document.getElementById('ct-alarm').value = '0.9';
  document.getElementById('ct-sound').value = '';
  document.getElementById('ct-eff').value = new Date().toISOString();
  document.getElementById('ct-enabled').checked = true;
}

async function saveCt() {
  const id = document.getElementById('ct-id').value;
  const payload = {
    configVersion: document.getElementById('ct-ver').value.trim(),
    engineType: document.getElementById('ct-engine').value.trim() || null,
    stationCode: document.getElementById('ct-station').value.trim(),
    standardCt: Number(document.getElementById('ct-sec').value),
    warnThreshold: Number(document.getElementById('ct-warn').value),
    alarmThreshold: Number(document.getElementById('ct-alarm').value),
    soundPolicy: document.getElementById('ct-sound').value.trim() || null,
    effectiveTime: document.getElementById('ct-eff').value.trim(),
    enabled: document.getElementById('ct-enabled').checked
  };
  const url = id ? `/api/v1/management/ct-configs/${id}` : '/api/v1/management/ct-configs';
  const method = id ? 'PUT' : 'POST';
  const r = await CcecApi.authFetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!(await guardMgmt(r))) return;
  if (!r.ok) {
    alert('保存失败');
    return;
  }
  clearCt();
  await loadCt();
}

async function loadAlarms() {
  const st = document.getElementById('al-status').value;
  const url = st
    ? `/api/v1/management/alarms?handleStatus=${encodeURIComponent(st)}`
    : '/api/v1/management/alarms';
  const r = await fetch(url, { credentials: 'include' });
  if (!(await guardMgmt(r))) return;
  const body = await r.json();
  const rows = body.data.items || [];
  const tb = document.querySelector('#al-table tbody');
  tb.innerHTML = rows.map((row) => `
    <tr>
      <td>${esc(row.startTime)}</td>
      <td>${esc(row.stationCode)}</td>
      <td>${esc(row.alarmLevel)}</td>
      <td>${esc(row.alarmDesc)}</td>
      <td>${esc(row.handleStatus)}</td>
      <td>${row.handleStatus === 'OPEN' ? `<button type="button" class="js-close-al" data-id="${row.id}">关闭</button>` : '—'}</td>
    </tr>`).join('');
  tb.querySelectorAll('.js-close-al').forEach((btn) => {
    btn.addEventListener('click', () => closeAlarm(btn.dataset.id));
  });
}

async function closeAlarm(id) {
  const r = await CcecApi.authFetch(`/api/v1/management/alarms/${id}/handle`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ handleStatus: 'CLOSED' })
  });
  if (!(await guardMgmt(r))) return;
  await loadAlarms();
}

async function loadProduction() {
  const r = await fetch(`/api/v1/management/production-records?page=${prPage}&size=${prSize}`, { credentials: 'include' });
  if (!(await guardMgmt(r))) return;
  const body = await r.json();
  const d = body.data;
  const rows = d.items || [];
  document.getElementById('pr-meta').textContent = `共 ${d.total} 条 · 第 ${d.page + 1} 页`;
  const tb = document.querySelector('#pr-table tbody');
  tb.innerHTML = rows.map((row) => `
    <tr>
      <td>${esc(row.stationCode)}</td>
      <td>${esc(row.soNo)}</td>
      <td>${esc(row.esnNo)}</td>
      <td>${esc(row.standardCt)}</td>
      <td>${esc(row.actualCt ?? '—')}</td>
      <td>${esc(row.startTime)}</td>
      <td>${esc(row.endTime ?? '—')}</td>
    </tr>`).join('');
}

async function loadAudit() {
  const r = await fetch(`/api/v1/management/audit-logs?page=${auPage}&size=${prSize}`, { credentials: 'include' });
  if (!(await guardMgmt(r))) return;
  const body = await r.json();
  const d = body.data;
  const rows = d.items || [];
  document.getElementById('au-meta').textContent = `共 ${d.total} 条 · 第 ${auPage + 1} 页`;
  const tb = document.querySelector('#au-table tbody');
  tb.innerHTML = rows.map((row) => `
    <tr>
      <td>${esc(row.createdAt)}</td>
      <td>${esc(row.userId)}</td>
      <td>${esc(row.operationType)}</td>
      <td>${esc(row.objectType)}</td>
      <td>${esc(row.objectId)}</td>
    </tr>`).join('');
}

document.querySelectorAll('aside nav button').forEach((b) => {
  b.addEventListener('click', () => {
    showTab(b.dataset.tab);
    if (b.dataset.tab === 'stations') loadStations();
    if (b.dataset.tab === 'ct') loadCt();
    if (b.dataset.tab === 'alarms') loadAlarms();
    if (b.dataset.tab === 'production') loadProduction();
    if (b.dataset.tab === 'audit') loadAudit();
  });
});

document.getElementById('st-reload').addEventListener('click', loadStations);
document.getElementById('st-save').addEventListener('click', saveStation);
document.getElementById('st-clear').addEventListener('click', clearStation);

document.getElementById('ct-reload').addEventListener('click', loadCt);
document.getElementById('ct-save').addEventListener('click', saveCt);
document.getElementById('ct-clear').addEventListener('click', clearCt);

document.getElementById('al-reload').addEventListener('click', loadAlarms);
document.getElementById('al-status').addEventListener('change', loadAlarms);

document.getElementById('pr-prev').addEventListener('click', () => {
  prPage = Math.max(0, prPage - 1);
  loadProduction();
});
document.getElementById('pr-next').addEventListener('click', () => {
  prPage += 1;
  loadProduction();
});

document.getElementById('au-prev').addEventListener('click', () => {
  auPage = Math.max(0, auPage - 1);
  loadAudit();
});
document.getElementById('au-next').addEventListener('click', () => {
  auPage += 1;
  loadAudit();
});

document.getElementById('logout').addEventListener('click', async () => {
  await CcecApi.ensureCsrf();
  await fetch('/api/v1/auth/logout', {
    method: 'POST',
    credentials: 'include',
    headers: { ...CcecApi.csrfHeaders() }
  });
  location.href = './login.html';
});

(async function init() {
  if (!(await guardMe())) return;
  document.getElementById('ct-eff').placeholder = new Date().toISOString();
  await loadStations();
})();
