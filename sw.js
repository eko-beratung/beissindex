// Beißindex Service Worker: App offline verfügbar, Updates kommen über das Netz zuerst.
const CACHE = 'beissindex-v2';
const CORE = ['./', './index.html', './manifest.webmanifest', './icon-180.png', './icon-192.png', './icon-512.png', './leaflet.js', './leaflet.css', './datenschutz.html'];
const EXTERNAL = [];

self.addEventListener('install', e => {
  e.waitUntil((async () => {
    const c = await caches.open(CACHE);
    for (const url of [...CORE, ...EXTERNAL]) { try { await c.add(url); } catch (_) {} }
    self.skipWaiting();
  })());
});

self.addEventListener('activate', e => {
  e.waitUntil((async () => {
    for (const k of await caches.keys()) if (k !== CACHE) await caches.delete(k);
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', e => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  // Wetter, Ortssuche und Kartenkacheln immer live, nie aus dem Cache
  if (url.origin !== location.origin) return;

  // Seite selbst: zuerst Netz (damit Updates sofort ankommen), offline aus dem Cache
  if (req.mode === 'navigate') {
    e.respondWith((async () => {
      try {
        const res = await fetch(req);
        const c = await caches.open(CACHE); c.put('./index.html', res.clone());
        return res;
      } catch (_) {
        return (await caches.match('./index.html')) || (await caches.match('./')) || Response.error();
      }
    })());
    return;
  }

  // Alles andere (Icons, Leaflet, Schriften): Cache sofort, im Hintergrund aktualisieren
  if (url.origin === location.origin) {
    e.respondWith((async () => {
      const c = await caches.open(CACHE);
      const hit = await c.match(req);
      const net = fetch(req).then(res => { if (res && (res.ok || res.type === 'opaque')) c.put(req, res.clone()); return res; }).catch(() => null);
      return hit || (await net) || Response.error();
    })());
  }
});
