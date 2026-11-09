(async () => {
  await loadLang(); renderNav(true);
  const me = await getJSON('/api/users/me', 'me');
  const p = new URLSearchParams(location.search);
  const topic = p.get('topic');
  const teacherPreview = me && me.role === 'TEACHER';
  const g = teacherPreview ? (Number(p.get('grade')) === 7 ? 7 : 6) : (Number(me && me.grade) || 6);
  const box = document.getElementById('box');
  const url = '/api/questions?topic=' + encodeURIComponent(topic) + (teacherPreview ? '&grade=' + g : '');
  const all = await getJSON(url, 'q_' + g + '_' + topic);
  if (!all || !all.length) { box.textContent = t('noQuestions'); return; }

  // pick up to 4 questions that have different answers
  const hi = getLang() === 'hi';
  const seen = new Set(), pairs = [];
  shuffle(all).forEach(q => {
    const ans = [q.optionA, q.optionB, q.optionC, q.optionD][q.answer];
    if (pairs.length < 4 && !seen.has(ans)) {
      seen.add(ans);
      pairs.push({ q: (hi && q.textHi) ? q.textHi : q.textEn, a: ans });
    }
  });

  document.getElementById('title').textContent = tr('topic_' + topic, topic) + ' · ' + t('matchHint');
  const startTime = Date.now();
  let selected = null, mistakes = 0, done = 0;

  const left = shuffle(pairs.map((x, id) => ({ id, text: x.q })));
  const right = shuffle(pairs.map((x, id) => ({ id, text: x.a })));

  left.forEach(item => {
    const b = mk('button', 'btn btn-outline-primary match-btn', item.text);
    b.onclick = () => {
      if (b.classList.contains('done')) return;
      document.querySelectorAll('#left .selected').forEach(x => x.classList.remove('selected'));
      b.classList.add('selected');
      selected = { id: item.id, el: b };
    };
    document.getElementById('left').append(b);
  });

  right.forEach(item => {
    const b = mk('button', 'btn btn-outline-secondary match-btn', item.text);
    b.onclick = () => {
      if (!selected || b.classList.contains('done')) return;
      if (selected.id === item.id) {
        selected.el.classList.remove('selected');
        selected.el.classList.add('done');
        b.classList.add('done');
        done++;
        selected = null;
        if (done === pairs.length) finish();
      } else {
        mistakes++;
        b.classList.add('wrong');
        setTimeout(() => b.classList.remove('wrong'), 500);
      }
    };
    document.getElementById('right').append(b);
  });

  function finish() {
    const score = Math.max(0, pairs.length - mistakes);
    const secs = Math.round((Date.now() - startTime) / 1000);
    saveResult({ topic: topic, game: 'MATCH', score: score, total: pairs.length, timeTaken: secs })
      .then(() => showResult(box, score, pairs.length));
    showResult(box, score, pairs.length);
  }
})();
