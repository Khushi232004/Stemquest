const CACHE = 'stemquest-v10';
const CORE = [
  '/', '/welcome.html', '/login.html', '/register.html', '/index.html', '/learn.html',
  '/leaderboard.html', '/profile.html', '/quiz.html', '/match.html', '/teacher.html',
  '/students.html', '/student.html',
  '/css/style.css', '/js/app.js', '/js/quiz.js', '/js/match.js',
  '/locales/en.json', '/locales/hi.json', '/manifest.json',
  '/vendor/bootstrap.min.css', '/vendor/chart.umd.min.js'
];

self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE).then(c =>
      Promise.all(CORE.map(u => c.add(new Request(u, { mode: 'cors' })).catch(() => {})))
    )
  );
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', e => {
  const req = e.request;
  const url = new URL(req.url);
  if (req.method !== 'GET') return;
  if (url.origin === location.origin &&
      (url.pathname.startsWith('/api') || url.pathname === '/logout' || url.pathname === '/login')) return;

  e.respondWith(
    fetch(req).then(res => {
      if (res.ok && !res.redirected) {
        const copy = res.clone();
        caches.open(CACHE).then(c => c.put(req, copy));
      }
      return res;
    }).catch(async () => {
      const hit = await caches.match(req, { ignoreSearch: true });
      if (hit) return hit;
      if (req.mode === 'navigate') return caches.match('/index.html');
      return Response.error();
    })
  );
});
