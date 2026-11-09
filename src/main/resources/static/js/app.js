if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/service-worker.js').catch(() => {});
}

/* ---------- Small helpers ---------- */
function mk(tag, cls, text) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (text !== undefined) e.textContent = text;
  return e;
}
function shuffle(a) {
  a = a.slice();
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

/* ---------- Language ---------- */
let dict = {};
function getLang() { return localStorage.getItem('lang') || 'en'; }
function setLang(l) { if (l === getLang()) return; localStorage.setItem('lang', l); location.reload(); }
function t(k) { return dict[k] || k; }
function tr(k, f) { return dict[k] || f; }

async function loadLang() {
  try {
    const r = await fetch('/locales/' + getLang() + '.json');
    dict = await r.json();
  } catch (e) {}
  document.documentElement.lang = getLang();
  document.querySelectorAll('[data-i18n]').forEach(el => el.textContent = t(el.dataset.i18n));
  document.querySelectorAll('[data-i18n-ph]').forEach(el => el.placeholder = t(el.dataset.i18nPh));
}

/* ---------- Avatar, rank, daily goal ---------- */
const AVATARS = ['🌙', '⭐', '🌊', '🍃', '🔭', '🪐'];
function getAvatar() {
  try {
    const avatar = JSON.parse(localStorage.getItem('me') || '{}').avatar;
    return AVATARS.includes(avatar) ? avatar : '🌙';
  } catch (e) { return '🌙'; }
}
function rankFor(level) { return t('rank_' + Math.min(Math.max(level, 1), 6)); }

const DAILY_GOAL = 3;
function todayKey() { return new Date().toLocaleDateString('en-CA'); }
function dailyKey() { return 'daily:' + (myId() || 0); }
function getDaily() {
  try {
    const d = JSON.parse(localStorage.getItem(dailyKey()) || '{}');
    return d.uid === myId() && d.date === todayKey() ? d.n : 0;
  } catch (e) { return 0; }
}
function addDaily() {
  const uid = myId();
  if (uid) localStorage.setItem(dailyKey(), JSON.stringify({ uid, date: todayKey(), n: getDaily() + 1 }));
}

/* ---------- Calm game feedback ---------- */
function burst() {}

/* ---------- Navigation ---------- */
let navShown = null;
function logout() {
  ['me', 'lb', 'role', 'hist', 'topics'].forEach(k => localStorage.removeItem(k));
  location.href = '/logout';
}

function renderNav(showLogout) {
  navShown = showLogout;
  if (localStorage.getItem('role') === 'TEACHER' &&
      ['/index.html', '/learn.html', '/leaderboard.html', '/quiz.html', '/match.html'].includes(location.pathname)) {
    location.replace('/teacher.html'); return;
  }
  const nav = document.getElementById('nav');
  if (!nav) return;
  const path = location.pathname;
  let items;
  if (localStorage.getItem('role') === 'TEACHER') {
    items = [['/teacher.html', '🎓', 'navDash'], ['/students.html', '👥', 'navStudents'],
             ['/profile.html', '👤', 'navProfile']];
  } else {
    items = [['/index.html', '🏠', 'navHome'], ['/learn.html', '📚', 'navLearn'],
             ['/leaderboard.html', '🏆', 'navRanks'], ['/profile.html', '👤', 'navProfile']];
  }

  let h = '<header class="topbar"><a class="brand" href="' + (showLogout ? '/index.html' : '/welcome.html') + '">StemQuest</a>' +
    '<div class="d-flex align-items-center gap-2">' +
    '<div class="lang"><button data-l="en">EN</button><button data-l="hi">हिं</button></div>' +
    (showLogout ? '<button id="logoutBtn" class="iconbtn" title="Logout">⎋</button>' : '') + '</div></header>';
  if (showLogout) {
    h += '<nav class="bnav">' + items.map(i =>
      '<a href="' + i[0] + '" class="' + (path === i[0] ? 'on' : '') + '"><span>' + i[1] + '</span><small>' + t(i[2]) + '</small></a>'
    ).join('') + '</nav>';
  }
  nav.innerHTML = h;

  nav.querySelectorAll('.lang button').forEach(b => {
    if (b.dataset.l === getLang()) b.classList.add('on');
    b.onclick = () => setLang(b.dataset.l);
  });
  const lb = document.getElementById('logoutBtn');
  if (lb) lb.onclick = logout;

}

/* ---------- Fetch with offline cache ---------- */
async function getJSON(url, key) {
  try {
    const r = await fetch(url);
    if (r.status === 401) { location.href = '/login.html'; return null; }
    if (r.ok) {
      const d = await r.json();
      localStorage.setItem(key, JSON.stringify(d));
      let role = null;
      if (url === '/api/users/me' && d.role) role = d.role;
      else if (url.startsWith('/api/teacher/')) role = 'TEACHER';
      if (role && localStorage.getItem('role') !== role) {
        localStorage.setItem('role', role);
        if (navShown !== null) renderNav(navShown);
      }
      return d;
    }
  } catch (e) {}
  const c = localStorage.getItem(key);
  return c ? JSON.parse(c) : null;
}

/* ---------- Offline queue + sync (results are tagged with the user's id) ---------- */
function myId() {
  try { return Number(JSON.parse(localStorage.getItem('me') || '{}').id) || 0; } catch (e) { return 0; }
}
function getQueue() { return JSON.parse(localStorage.getItem('queue') || '[]'); }
function isMine(x) { const uid = myId(); return uid > 0 && Number(x && x.uid) === uid; }
function pendingCount() { return getQueue().filter(isMine).length; }

function saveResult(r) {
  r.clientId = Date.now() + '-' + Math.random().toString(36).slice(2);
  r.uid = myId();
  const q = getQueue();
  q.push(r);
  localStorage.setItem('queue', JSON.stringify(q));
  addDaily();
  return syncNow();
}

async function syncNow() {
  const all = getQueue();
  const mine = all.filter(isMine);
  if (!mine.length || !navigator.onLine) return null;
  try {
    const res = await fetch('/api/progress/sync', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(mine)
    });
    if (res.ok) {
      localStorage.setItem('queue', JSON.stringify(all.filter(x => !isMine(x))));
      return await res.json();
    }
  } catch (e) {}
  return null;
}
window.addEventListener('online', syncNow);

/* ---------- Result screen (quiz + match) ---------- */
function showResult(el, score, total) {
  const xp = score * 10 + (total > 0 && score === total ? 20 : 0);
  const pct = total ? score / total : 0;
  el.innerHTML = '';
  const w = mk('div', 'text-center pop');
  w.append(
    mk('div', 'face', pct === 1 ? '🏆' : pct >= 0.6 ? '😄' : '💪'),
    mk('h3', 'mb-2', t('score') + ': ' + score + ' / ' + total),
    mk('div', 'xpchip mb-3', '+' + xp + ' XP'),
    mk('p', '', pct === 1 ? t('msgPerfect') : pct >= 0.6 ? t('msgGood') : t('msgTry')),
    mk('p', 'text-muted small mb-1', Math.min(getDaily(), DAILY_GOAL) + ' / ' + DAILY_GOAL + ' ' + t('gamesToday')),
    mk('p', 'text-muted small', pendingCount() ? t('pending') : t('saved'))
  );
  const row = mk('div', 'd-flex gap-2 justify-content-center flex-wrap');
  const a = mk('a', 'btn btn-success', t('back')); a.href = '/index.html';
  const b = mk('a', 'btn btn-primary', t('playAgain')); b.href = location.href;
  row.append(a, b);
  w.append(row);
  el.append(w);
  if (!el.dataset.shown) { el.dataset.shown = '1'; if (pct >= 0.6) burst(); }
}
