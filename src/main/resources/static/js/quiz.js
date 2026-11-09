(async () => {
  await loadLang(); renderNav(true);
  const me = await getJSON('/api/users/me', 'me');
  const p = new URLSearchParams(location.search);
  const topic = p.get('topic');
  const teacherPreview = me && me.role === 'TEACHER';
  const g = teacherPreview ? (Number(p.get('grade')) === 7 ? 7 : 6) : (Number(me && me.grade) || 6);
  const box = document.getElementById('box');
  const url = '/api/questions?topic=' + encodeURIComponent(topic) + (teacherPreview ? '&grade=' + g : '');

  const questions = await getJSON(url, 'q_' + g + '_' + topic);
  if (!questions || !questions.length) { box.textContent = t('noQuestions'); return; }

  const qs = shuffle(questions).slice(0, 5);
  let i = 0, score = 0, left = 15, timer = null;
  const startTime = Date.now();
  const $ = id => document.getElementById(id);

  function show() {
    if (i >= qs.length) return finish();
    const q = qs[i];
    if ($('qbar')) $('qbar').style.width = (i / qs.length * 100) + '%';
    $('count').textContent = (i + 1) + ' / ' + qs.length;
    $('qtext').textContent = (getLang() === 'hi' && q.textHi) ? q.textHi : q.textEn;
    $('opts').innerHTML = '';
    [q.optionA, q.optionB, q.optionC, q.optionD].forEach((text, idx) => {
      const b = document.createElement('button');
      b.className = 'btn btn-outline-primary opt-btn';
      b.textContent = text;
      b.onclick = () => answer(idx, b);
      $('opts').append(b);
    });
    left = 15;
    $('timer').textContent = '⏱ ' + left;
    clearInterval(timer);
    timer = setInterval(() => {
      left--;
      $('timer').textContent = '⏱ ' + left;
      if (left <= 0) answer(-1, null);
    }, 1000);
  }

  function answer(idx, btn) {
    clearInterval(timer);
    const q = qs[i];
    const buttons = $('opts').querySelectorAll('button');
    buttons.forEach(b => b.disabled = true);
    buttons[q.answer].className = 'btn btn-success opt-btn';
    if (idx === q.answer) score++;
    else if (btn) btn.className = 'btn btn-danger opt-btn';
    setTimeout(() => { i++; show(); }, 900);
  }

  function finish() {
    clearInterval(timer);
    const secs = Math.round((Date.now() - startTime) / 1000);
    saveResult({ topic: topic, game: 'QUIZ', score: score, total: qs.length, timeTaken: secs })
      .then(() => showResult(box, score, qs.length));
    showResult(box, score, qs.length);
  }

  show();
})();
