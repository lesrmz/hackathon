import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { createRoot } from 'react-dom/client'
import './app.css'

const LANGUAGES = ['en', 'ary', 'ar', 'fr', 'es', 'ur', 'ru', 'ky']
const LANGUAGE_NAMES = {
  en: 'English',
  ary: 'Moroccan Darija',
  ar: 'Arabic',
  fr: 'French',
  es: 'Spanish',
  ur: 'Urdu',
  ru: 'Russian',
  ky: 'Kyrgyz',
}
const RTL_LANGUAGES = new Set(['ary', 'ar', 'ur'])
const VILLAIN_TAGLINES = {
  'pink-manipulator': 'Pretty but Petty',
  'corporate-heartbreaker': 'CEO of Bad Decisions',
  'romantic-menace': 'Love Is a Battlefield',
  'lavender-chaos-queen': 'Delulu but Designer',
  'innocent-menace': 'Sweet Face, Secret Agenda',
}
const VILLAIN_OUTFITS = {
  'pink-manipulator': '/assets/outfits/pink.svg',
  'corporate-heartbreaker': '/assets/outfits/corporate.svg',
  'romantic-menace': '/assets/outfits/drama.svg',
  'lavender-chaos-queen': '/assets/outfits/velvet.svg',
  'innocent-menace': '/assets/outfits/pink.svg',
}

async function api(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const headers = { ...options.headers }
  const request = async (url, init) => {
    try {
      return await fetch(url, init)
    } catch (error) {
      if (error instanceof TypeError) {
        throw new Error('Cannot reach the Pretty Petty API. Start Spring Boot and MongoDB, then refresh.')
      }
      throw error
    }
  }
  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
    let csrfToken = document.cookie.split('; ').find((cookie) => cookie.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('=')
    if (!csrfToken) {
      const csrfResponse = await request('/api/auth/csrf', { credentials: 'same-origin' })
      if (!csrfResponse.ok) throw new Error('Could not initialize a secure request token. Refresh and retry.')
      csrfToken = document.cookie.split('; ').find((cookie) => cookie.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('=')
    }
    if (!csrfToken) throw new Error('The secure request token is missing. Refresh and retry.')
    headers['X-XSRF-TOKEN'] = decodeURIComponent(csrfToken)
  }
  const response = await request(path, {
    credentials: 'same-origin',
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || body.error || `Request failed (${response.status})`)
  }
  if (response.status === 204) return null
  return response.json()
}

function localDateValue(isoDate) {
  if (!isoDate) return ''
  const date = new Date(isoDate)
  if (Number.isNaN(date.getTime())) return ''
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 16)
}

function dateToIso(value) {
  return value ? new Date(value).toISOString() : null
}

function itemIcon(item) {
  const name = item.toLowerCase()
  if (/bow|ribbon|barrette|clip/.test(name)) return '🎀'
  if (/bag|purse|clutch|tote/.test(name)) return '👜'
  if (/heel|shoe|flat|mary jane|platform/.test(name)) return '👠'
  if (/lip|gloss|lipstick|compact/.test(name)) return '💄'
  if (/pearl|jewel|hoop|ring|watch|locket|charm/.test(name)) return '💎'
  if (/coffee|café/.test(name)) return '☕'
  if (/rose|flower/.test(name)) return '🌹'
  if (/sunglass/.test(name)) return '🕶️'
  if (/trouser|pants/.test(name)) return '👖'
  if (/skirt|mini/.test(name)) return '👗'
  if (/cardigan|sweater|knit|blazer|shirt|dress|skirt|glove/.test(name)) return '👗'
  return '✨'
}

function App() {
  const [screen, setScreen] = useState(() => window.location.pathname.startsWith('/board/') ? 'planner' : 'home')
  const [questions, setQuestions] = useState([])
  const [themes, setThemes] = useState([])
  const [user, setUser] = useState(null)
  const [result, setResult] = useState(null)
  const [board, setBoard] = useState(null)
  const [notes, setNotes] = useState([])
  const [answers, setAnswers] = useState({})
  const [quizIndex, setQuizIndex] = useState(0)
  const [selectedTheme, setSelectedTheme] = useState('pink')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [reaction, setReaction] = useState(null)
  const reactionTimer = useRef(null)
  const [editor, setEditor] = useState(null)
  const [chaosOpen, setChaosOpen] = useState(false)
  const [chaosLanguageIndex, setChaosLanguageIndex] = useState(0)
  const [copyLabel, setCopyLabel] = useState('Copy Board Link')
  const [analysisText, setAnalysisText] = useState('Analyzing your questionable decisions...')
  const [mobileOrder, setMobileOrder] = useState([])
  const [booted, setBooted] = useState(false)
  const [drag, setDrag] = useState(null)
  const boardRef = useRef(null)
  const chaosButtonRef = useRef(null)
  const chaosCloseRef = useRef(null)
  const boardId = board?.id

  const theme = useMemo(
    () => themes.find((item) => item.id === (board?.themeId || selectedTheme)) || themes[0],
    [themes, board?.themeId, selectedTheme],
  )

  useEffect(() => {
    if (!theme) return
    const root = document.documentElement
    root.style.setProperty('--page', theme.backgroundColor)
    root.style.setProperty('--note', theme.noteColor)
    root.style.setProperty('--ink', theme.textColor)
    root.style.setProperty('--accent', theme.accentColor)
  }, [theme])

  useEffect(() => {
    Promise.all([api('/api/quiz/questions'), api('/api/themes'), api('/api/auth/me').catch(() => null)])
      .then(([questionData, themeData, profile]) => {
        setQuestions(questionData)
        setThemes(themeData)
        setUser(profile?.id ? profile : null)
        if (profile?.aestheticId) setSelectedTheme(profile.aestheticId)
      })
      .catch((requestError) => setError(`${requestError.message}. Start the Spring Boot API and local MongoDB, then refresh.`))
      .finally(() => setBooted(true))
  }, [])

  useEffect(() => {
    function handlePopState() {
      if (window.location.pathname.startsWith('/board/')) {
        setScreen('planner')
        loadBoard(window.location.pathname.split('/').filter(Boolean).at(-1))
      } else {
        setScreen('home')
      }
    }
    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [user])

  useEffect(() => {
    if (!booted || !window.location.pathname.startsWith('/board/')) return
    if (user) {
      loadBoard(window.location.pathname.split('/').filter(Boolean).at(-1))
    } else {
      api('/api/auth/guest', { method: 'POST' })
        .then(setUser)
        .catch((requestError) => setError(requestError.message))
    }
  }, [booted, user])

  useEffect(() => {
    if (!chaosOpen) return undefined
    chaosCloseRef.current?.focus()
    function onKeyDown(event) {
      if (event.key === 'Escape') closeChaos()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [chaosOpen])

  useEffect(() => {
    if (!chaosOpen) return undefined
    const timer = window.setInterval(() => setChaosLanguageIndex((index) => (index + 1) % LANGUAGES.length), 1900)
    return () => window.clearInterval(timer)
  }, [chaosOpen])

  const showReaction = useCallback(async (trigger) => {
    try {
      const next = await api(`/api/reactions?trigger=${encodeURIComponent(trigger)}`)
      setReaction(next)
      window.clearTimeout(reactionTimer.current)
      reactionTimer.current = window.setTimeout(() => setReaction(null), 4600)
    } catch (requestError) {
      setError(requestError.message)
    }
  }, [])

  async function loadBoard(id) {
    if (!id) return
    if (!user) {
      try {
        setUser(await api('/api/auth/guest', { method: 'POST' }))
      } catch (requestError) {
        setError(requestError.message)
      }
      return
    }
    setLoading(true)
    setError('')
    try {
      const saved = await api(`/api/boards/${encodeURIComponent(id)}`)
      setBoard(saved.board)
      setNotes(saved.notes || [])
      setSelectedTheme(saved.board.themeId)
      setScreen('planner')
    } catch (requestError) {
      setError(requestError.message)
      setScreen('home')
    } finally {
      setLoading(false)
    }
  }

  function startQuiz() {
    setError('')
    setAnswers({})
    setQuizIndex(0)
    setScreen('quiz')
    showReaction('QUIZ_START')
  }

  async function revealVillain(event) {
    event.preventDefault()
    if (loading || Object.keys(answers).length !== questions.length) return
    setLoading(true)
    setError('')
    setScreen('analyzing')
    setAnalysisText('Analyzing your questionable decisions...')
    await new Promise((resolve) => window.setTimeout(resolve, 950))
    setAnalysisText('Your innocence could not be verified.')
    try {
      const found = await api('/api/quiz/result', {
        method: 'POST',
        body: JSON.stringify({ answerIds: questions.map((question) => answers[question.id]) }),
      })
      setResult(found)
      setSelectedTheme(found.suggestedAestheticId)
      setScreen('result')
      showReaction('RESULT')
    } catch (requestError) {
      setError(requestError.message)
      setScreen('quiz')
    } finally {
      setLoading(false)
    }
  }

  async function enterPlanner() {
    if (!result || loading) return
    setLoading(true)
    setError('')
    try {
      if (!user) setUser(await api('/api/auth/guest', { method: 'POST' }))
      const profile = await api('/api/auth/profile', {
        method: 'PATCH',
        body: JSON.stringify({ villainId: result.villainId, aestheticId: selectedTheme }),
      })
      setUser(profile)
      if (board?.id) {
        await api(`/api/boards/${board.id}/villain`, {
          method: 'PATCH',
          body: JSON.stringify({ villainId: result.villainId }),
        })
        const updatedBoard = await api(`/api/boards/${board.id}/theme`, {
          method: 'PATCH',
          body: JSON.stringify({ themeId: selectedTheme }),
        })
        setBoard((current) => ({ ...current, ...updatedBoard }))
      } else {
        const created = await api('/api/boards', {
          method: 'POST',
          body: JSON.stringify({ villainId: result.villainId, themeId: selectedTheme }),
        })
        setBoard(created.board)
        setNotes(created.notes || [])
        window.history.pushState({}, '', `/board/${created.board.id}`)
      }
      setScreen('planner')
      showReaction('PLANNER_ENTRY')
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  async function saveTheme(themeId) {
    setSelectedTheme(themeId)
    if (!boardId) return
    try {
      const updated = await api(`/api/boards/${boardId}/theme`, {
        method: 'PATCH',
        body: JSON.stringify({ themeId }),
      })
      setBoard((current) => ({ ...current, ...updated }))
      if (user) {
        const profile = await api('/api/auth/profile', {
          method: 'PATCH',
          body: JSON.stringify({ villainId: board.villainId, aestheticId: themeId }),
        })
        setUser(profile)
      }
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  async function saveNote(noteId, patch) {
    const saved = await api(`/api/boards/${boardId}/notes/${noteId}`, {
      method: 'PATCH',
      body: JSON.stringify(patch),
    })
    setNotes((current) => current.map((note) => note.id === noteId ? saved : note))
    return saved
  }

  async function submitNote(event) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const text = String(form.get('text') || '').trim()
    if (!text) return
    setLoading(true)
    setError('')
    const payload = {
      text,
      type: form.get('type'),
      deadline: dateToIso(String(form.get('deadline') || '')),
    }
    try {
      if (editor?.id) {
        const updated = await api(`/api/boards/${boardId}/notes/${editor.id}`, {
          method: 'PATCH',
          body: JSON.stringify(payload),
        })
        setNotes((current) => current.map((note) => note.id === updated.id ? updated : note))
      } else {
        const created = await api(`/api/boards/${boardId}/notes`, {
          method: 'POST',
          body: JSON.stringify({ ...payload, x: 22 + Math.random() * 180, y: 24 + Math.random() * 130, rotation: Math.random() * 6 - 3 }),
        })
        setNotes((current) => [...current, created])
        showReaction('ADD_NOTE')
      }
      setEditor(null)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  async function deleteNote(noteId) {
    try {
      await api(`/api/boards/${boardId}/notes/${noteId}`, { method: 'DELETE' })
      setNotes((current) => current.filter((note) => note.id !== noteId))
      setEditor(null)
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  async function toggleNote(note) {
    try {
      await saveNote(note.id, { completed: !note.completed })
      if (!note.completed) showReaction('COMPLETE')
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  async function bringToFront(note, persist = true) {
    const top = Math.max(0, ...notes.map((item) => item.zIndex || 0)) + 1
    if (note.zIndex >= top - 1) return
    setNotes((current) => current.map((item) => item.id === note.id ? { ...item, zIndex: top } : item))
    if (!persist) return
    try {
      await saveNote(note.id, { zIndex: top })
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  function onDragStart(event, note) {
    if (window.matchMedia('(max-width: 720px)').matches) return
    const boardRect = boardRef.current?.getBoundingClientRect()
    const noteRect = event.currentTarget.closest('.sticky-note').getBoundingClientRect()
    if (!boardRect) return
    event.currentTarget.setPointerCapture?.(event.pointerId)
    const top = Math.max(0, ...notes.map((item) => item.zIndex || 0)) + 1
    setDrag({ id: note.id, dx: event.clientX - noteRect.left, dy: event.clientY - noteRect.top })
    setNotes((current) => current.map((item) => item.id === note.id ? { ...item, zIndex: top } : item))
  }

  function onBoardPointerMove(event) {
    if (!drag || !boardRef.current) return
    const rect = boardRef.current.getBoundingClientRect()
    const noteWidth = 200
    const noteHeight = 194
    const x = Math.max(0, Math.min(rect.width - noteWidth, event.clientX - rect.left - drag.dx))
    const y = Math.max(0, Math.min(rect.height - noteHeight, event.clientY - rect.top - drag.dy))
    setNotes((current) => current.map((note) => note.id === drag.id ? { ...note, x, y } : note))
  }

  async function onBoardPointerUp() {
    if (!drag) return
    const note = notes.find((item) => item.id === drag.id)
    setDrag(null)
    if (!note) return
    try {
      await saveNote(note.id, { x: note.x, y: note.y, zIndex: note.zIndex })
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  async function makeWorse() {
    if (!boardId || loading) return
    setLoading(true)
    setError('')
    const bounds = boardRef.current?.getBoundingClientRect()
    const mobile = window.matchMedia('(max-width: 720px)').matches
    try {
      const updated = await api(`/api/boards/${boardId}/chaos`, {
        method: 'POST',
        body: JSON.stringify({
          width: Math.max(190, Math.floor(bounds?.width || 900)),
          height: Math.max(158, Math.floor(bounds?.height || 640)),
          mobile,
        }),
      })
      setNotes(updated)
      if (mobile) setMobileOrder(updated.map((note) => note.id).sort(() => Math.random() - 0.5))
      const order = [...LANGUAGES].sort(() => Math.random() - 0.5)
      setChaosLanguages(order)
      setChaosLanguageIndex(0)
      setChaosOpen(true)
      showReaction('CHAOS')
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  const [chaosLanguages, setChaosLanguages] = useState(LANGUAGES)

  async function copyBoardLink() {
    try {
      await navigator.clipboard.writeText(window.location.href)
      setCopyLabel('Copied!')
      window.setTimeout(() => setCopyLabel('Copy Board Link'), 1800)
    } catch {
      setError('Clipboard access was blocked. Copy the board URL from your address bar.')
    }
  }

  function closeChaos() {
    setChaosOpen(false)
    window.setTimeout(() => chaosButtonRef.current?.focus(), 0)
  }

  const header = (
    <header className={`topbar ${screen === 'result' || screen === 'analyzing' ? 'dark-topbar' : ''}`}>
      <a className="wordmark" href="/" onClick={(event) => { event.preventDefault(); setScreen('home'); window.history.pushState({}, '', '/') }}>
        PRETTY PETTY<span>✳</span>
      </a>
      <span className="topbar-note">YOUR STYLE. YOUR COLOR. YOUR CHAOS.</span>
      <nav className="top-actions" aria-label="Main navigation">
        {user && <button className="text-button" onClick={() => setScreen('profile')}>Your Profile</button>}
      </nav>
    </header>
  )

  if (screen === 'analyzing') return <main className="app-shell dark-shell">{header}<div className="analysis-screen"><span className="analysis-spark">✦</span><p>{analysisText}</p><div className="analysis-line" /></div></main>

  return (
    <main className={`app-shell ${screen === 'result' ? 'editorial-shell' : ''} ${screen === 'planner' ? 'planner-shell' : ''}`}>
      {header}
      {error && <div className="error-banner" role="alert"><span>{error}</span><button aria-label="Dismiss error" onClick={() => setError('')}>×</button></div>}
      {loading && <div className="loading-strip" aria-live="polite">Saving your excellent decisions…</div>}
      {screen === 'home' && <HomePage onStart={startQuiz} user={user} onProfile={() => setScreen('profile')} />}
      {screen === 'quiz' && <QuizPage questions={questions} answers={answers} setAnswers={setAnswers} quizIndex={quizIndex} setQuizIndex={setQuizIndex} onReveal={revealVillain} loading={loading} />}
      {screen === 'result' && result && <VillainResult result={result} selectedTheme={selectedTheme} themes={themes} onTheme={setSelectedTheme} onEnter={() => enterPlanner()} onRetake={startQuiz} loading={loading} />}
      {screen === 'profile' && <ProfilePage user={user} themes={themes} selectedTheme={user?.aestheticId || selectedTheme} onSeeResult={async () => {
        if (!user?.villainId) return
        try {
          setResult(await api(`/api/quiz/villains/${user.villainId}`))
          setSelectedTheme(user.aestheticId || selectedTheme)
          setScreen('result')
        } catch (requestError) { setError(requestError.message) }
      }} onTheme={async (themeId) => {
        setSelectedTheme(themeId)
        if (user?.villainId) {
          try {
            setUser(await api('/api/auth/profile', { method: 'PATCH', body: JSON.stringify({ villainId: user.villainId, aestheticId: themeId }) }))
            if (board?.id) {
              const updated = await api(`/api/boards/${board.id}/theme`, {
                method: 'PATCH',
                body: JSON.stringify({ themeId }),
              })
              setBoard((current) => ({ ...current, ...updated }))
            }
          } catch (requestError) { setError(requestError.message) }
        }
      }} onRetake={startQuiz} onBack={() => setScreen('home')} />}
      {screen === 'planner' && board && <PlannerPage
        board={board}
        notes={notes}
        theme={theme}
        themes={themes}
        loading={loading}
        copyLabel={copyLabel}
        boardRef={boardRef}
        chaosButtonRef={chaosButtonRef}
        mobileOrder={mobileOrder}
        onTheme={saveTheme}
        onAdd={() => setEditor({ id: null, text: '', type: 'TASK', deadline: '' })}
        onEdit={(note) => setEditor({ ...note, deadline: localDateValue(note.deadline) })}
        onToggle={toggleNote}
        onDelete={deleteNote}
        onSelect={(note) => bringToFront(note)}
        onWorse={makeWorse}
        onCopy={copyBoardLink}
        onDragStart={onDragStart}
        onPointerMove={onBoardPointerMove}
        onPointerUp={onBoardPointerUp}
      />}
      {screen === 'planner' && !board && <div className="loading-screen">{loading ? 'Loading your saved board…' : 'Preparing your saved board…'}</div>}
      {editor && <NoteEditor note={editor} loading={loading} onClose={() => setEditor(null)} onSubmit={submitNote} onDelete={editor.id ? () => deleteNote(editor.id) : null} />}
      {chaosOpen && <ChaosPopup language={chaosLanguages[chaosLanguageIndex]} closeRef={chaosCloseRef} onClose={closeChaos} />}
      {reaction && ['quiz', 'result', 'planner'].includes(screen) && <ReactionBubble reaction={reaction} onClose={() => setReaction(null)} />}
      <footer className="site-footer"><span>YOUR LIFE IS A MESS. AT LEAST MAKE IT DESIGNER. 🎀</span><span>Original chaos, locally saved to MongoDB.</span></footer>
    </main>
  )
}

function HomePage({ onStart, user, onProfile }) {
  return <section className="home-page">
    <div className="home-copy">
      <p className="eyebrow">A VERY SCIENTIFIC STYLE STUDY · EST. QUESTIONABLE</p>
      <h1>Your Villain Era,<br /><em>But Make It Fashion</em></h1>
      <p className="home-description">You have excellent taste and questionable intentions. Answer a few questions, meet your fashion villain alter ego, and find out what color your chaos should be.</p>
      <div className="home-buttons"><button className="button button-dark" onClick={onStart}>Meet Your Villain <span>↗</span></button>
        {user && <button className="button button-light" onClick={onProfile}>Your Profile</button>}
      </div>
      <p className="home-tagline">“Your life is a mess. At least make it designer. 🎀”</p>
    </div>
    <div className="cover-art" aria-label="Fashion scrapbook collage">
      <div className="cover-paper"><img src="/assets/outfits/pink.svg" alt="Original illustrated pink fashion look" /><span className="cover-label">THE<br />PLOT<br />THICKENS</span></div>
      <span className="cover-bow" aria-hidden="true">🎀</span><span className="cover-cherry" aria-hidden="true">🍒</span>
      <div className="cover-stamp">STYLE<br />WITH<br />INTENT</div>
      <div className="cover-caption">innocent-ish<br />since whenever</div>
    </div>
    <div className="home-bottom"><span>FASHION · PERSONALITY · PLANNER</span><span>Scroll less. Plot more.</span></div>
  </section>
}

function QuizPage({ questions, answers, setAnswers, quizIndex, setQuizIndex, onReveal, loading }) {
  const question = questions[quizIndex]
  if (!question) return <section className="quiz-page"><p>Loading the quiz…</p></section>
  const complete = Object.keys(answers).length === questions.length
  return <section className="quiz-page">
    <div className="quiz-heading"><p className="eyebrow">THE VILLAIN STUDIO</p><h1>Let’s examine<br /><em>your choices.</em></h1><p>One answer per question. No appeals, darling.</p></div>
    <div className="progress-track" role="progressbar" aria-label="Quiz progress" aria-valuemin="0" aria-valuemax={questions.length} aria-valuenow={Object.keys(answers).length}><span style={{ width: `${Object.keys(answers).length / questions.length * 100}%` }} /></div>
    <form className="quiz-card" onSubmit={onReveal}>
      <div className="quiz-card-top"><span>CASE FILE 0{quizIndex + 1}</span><span>{quizIndex + 1} / {questions.length}</span></div>
      <h2>{question.text}</h2>
      <div className="answer-list">{question.answers.map((answer, index) => <button
        key={answer.id}
        type="button"
        className={`answer-option ${answers[question.id] === answer.id ? 'chosen' : ''}`}
        aria-pressed={answers[question.id] === answer.id}
        onClick={() => setAnswers((current) => ({ ...current, [question.id]: answer.id }))}
      ><span className="answer-number">0{index + 1}</span><span>{answer.text}</span><span className="answer-check">✳</span></button>)}</div>
      <div className="quiz-controls">
        <button className="button button-light" type="button" disabled={quizIndex === 0} onClick={() => setQuizIndex((index) => index - 1)}>← Previous</button>
        {quizIndex < questions.length - 1
          ? <button className="button button-dark" type="button" disabled={!answers[question.id]} onClick={() => setQuizIndex((index) => index + 1)}>Next Question →</button>
          : <button className="button button-dark" type="submit" disabled={!complete || loading}>Reveal My Villain ✦</button>}
      </div>
    </form>
  </section>
}

function VillainResult({ result, selectedTheme, themes, onTheme, onEnter, onRetake, loading }) {
  const collage = result.collages?.[0] || { title: 'The Signature Look', items: [result.outfitDescription] }
  const sticker = result.catStickerPath || '/assets/stickers/cat.svg'
  return <section className="result-page">
    <p className="magazine-kicker">THE VILLAIN STUDIO <span>✦</span> A PERSONAL STYLE REPORT</p>
    <div className="result-grid">
      <div className="result-copy">
        <p className="eyebrow">THE FASHION COUNCIL IS CONCERNED.</p>
        <p className="villain-subtitle">{VILLAIN_TAGLINES[result.villainId] || 'Your Style Report'}</p>
        <h1>{result.name}</h1>
        <p className="result-description">{result.description}</p>
        <blockquote>“{result.signatureQuote}”</blockquote>
        <div className="result-outfit-text"><span className="tiny-label">THE LOOK</span>{result.outfitDescription}</div>
        <div className="cat-sticker"><img src={sticker} onError={(event) => { event.currentTarget.src = '/assets/stickers/cat.svg' }} alt="Silly original cat sticker" /><span>COUNCIL<br />OBJECTION</span></div>
        <button className="button button-gold" onClick={onEnter} disabled={loading}>Enter My Chaos <span>↗</span></button>
        <button className="retake-link" onClick={onRetake}>Retake the quiz</button>
      </div>
      <div className="result-visual">
        <div className="outfit-card">
          <div className="outfit-image-wrap"><img src={result.outfitImage || VILLAIN_OUTFITS[result.villainId]} alt={`${result.name} original illustrated outfit`} /><span className="outfit-stamp">LOOK 01<br />UNREASONABLE</span></div>
          <div className="outfit-card-foot"><span>{collage.title}</span><span>PP—0{Object.keys(VILLAIN_TAGLINES).indexOf(result.villainId) + 1}</span></div>
        </div>
        <FashionCollage collage={collage} villainId={result.villainId} />
        <span className="result-star" aria-hidden="true">✦</span>
      </div>
    </div>
    <section className="palette-section">
      <div><p className="tiny-label">PALETTE IS YOURS, NOT YOUR VILLAIN’S</p><h2>Choose your own chaos color.</h2></div>
      <ThemePicker themes={themes} selectedTheme={selectedTheme} onSelect={onTheme} />
    </section>
  </section>
}

function FashionCollage({ collage, villainId }) {
  return <div className={`fashion-collage collage-${villainId}`} aria-label={`${collage.title} fashion moodboard`}>
    <span className="collage-title">{collage.title}</span>
    {collage.items?.slice(0, 6).map((item, index) => <div className={`collage-item item-${index + 1}`} key={`${item}-${index}`}><span>{itemIcon(item)}</span><small>{item}</small></div>)}
    <span className="collage-scribble">obviously<br />a good idea</span>
  </div>
}

function ThemePicker({ themes, selectedTheme, onSelect }) {
  return <div className="theme-picker" role="group" aria-label="Choose your color palette">{themes.map((item) => <button className={`theme-choice ${selectedTheme === item.id ? 'active' : ''}`} key={item.id} onClick={() => onSelect(item.id)} aria-pressed={selectedTheme === item.id}>
    <span className="theme-swatch" style={{ '--swatch': item.accentColor, '--paper': item.backgroundColor }} />
    <span>{item.name}</span><span className="theme-check">✓</span>
  </button>)}</div>
}

function ProfilePage({ user, themes, selectedTheme, onTheme, onRetake, onBack, onSeeResult }) {
  return <section className="profile-page">
    <p className="eyebrow">YOUR PERSONAL STYLE FILE</p><h1>Your Style.<br /><em>Your Color. Your Chaos.</em></h1>
    <div className="profile-card"><span className="profile-monogram">{user?.displayName?.slice(0, 1)?.toUpperCase() || 'P'}</span><div><h2>{user?.displayName || 'Guest'}</h2><small>Villain: {user?.villainId || 'Take the quiz to find out'}</small></div></div>
    <h2>Pick a new palette</h2><ThemePicker themes={themes} selectedTheme={selectedTheme} onSelect={onTheme} />
    <div className="profile-buttons">{user?.villainId && <button className="button button-light" onClick={onSeeResult}>Revisit your result</button>}<button className="button button-dark" onClick={onRetake}>Retake the quiz</button><button className="button button-light" onClick={onBack}>Back to home</button></div>
  </section>
}

function PlannerPage({ board, notes, theme, themes, loading, copyLabel, boardRef, chaosButtonRef, mobileOrder, onTheme, onAdd, onEdit, onToggle, onDelete, onSelect, onWorse, onCopy, onDragStart, onPointerMove, onPointerUp }) {
  const reorderedNotes = mobileOrder.length
    ? [...notes].sort((a, b) => mobileOrder.indexOf(a.id) - mobileOrder.indexOf(b.id))
    : [...notes].sort((a, b) => (b.zIndex || 0) - (a.zIndex || 0))
  return <section className="planner-page">
    <div className="planner-titlebar"><span className="window-dots"><i /><i /><i /></span><span>pretty-petty.exe — private chaos workspace</span><span>✕</span></div>
    <div className="planner-heading"><div><p className="eyebrow">BOARD NO. {board?.id?.slice(0, 8).toUpperCase()}</p><h1>Organized?<br /><em>Never Heard of Her.</em></h1><p>Put your deadlines, bad ideas, and things you’ll definitely do tomorrow here. Drag them around until it feels productive. Your planner is a disaster, but at least it matches your outfit.</p></div>
      <div className="planner-cat" aria-hidden="true"><img src="/assets/stickers/cat.svg" alt="" /><span>WORKING<br />ON IT*</span></div></div>
    <div className="planner-toolbar">
      <button className="button button-dark" onClick={onAdd}>＋ Add Note</button>
      <button className="button button-chaos" onClick={onWorse} disabled={loading} ref={chaosButtonRef}>MAKE IT WORSE 💀</button>
      <div className="toolbar-spacer" />
      <label className="toolbar-style">Change Style <select value={board?.themeId || theme?.id} onChange={(event) => onTheme(event.target.value)}>{themes.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
      <button className="button button-light copy-button" onClick={onCopy}>{copyLabel}</button>
    </div>
    <p className="board-sharing-note">Private to this browser session. A localhost link only works on this computer unless you configure hosting or network access.</p>
    <div className="desk-window">
      <div className="desk-window-bar"><span>✿ YOUR CURRENT SITUATION</span><span>— □ ×</span></div>
      <div className="planner-board" ref={boardRef} onPointerMove={onPointerMove} onPointerUp={onPointerUp} onPointerCancel={onPointerUp} aria-label="Messy planner board">
        <span className="desk-doodle doodle-one" aria-hidden="true">✳</span><span className="desk-doodle doodle-two" aria-hidden="true">♡</span><span className="desk-label" aria-hidden="true">do it<br />tomorrow</span>
        {notes.length === 0 && <div className="empty-board"><span>🗒</span><h2>Nothing is on fire. Yet.</h2><p>Add your first note and let the visual chaos begin.</p><button className="button button-dark" onClick={onAdd}>Add your first note</button></div>}
        {reorderedNotes.map((note, index) => <StickyNote
          key={note.id}
          note={note}
          index={index}
          onEdit={onEdit}
          onToggle={onToggle}
          onDelete={onDelete}
          onDragStart={onDragStart}
          onSelect={() => onSelect(note)}
        />)}
      </div>
    </div>
    <div className="planner-bottomline"><span>{notes.length} little {notes.length === 1 ? 'problem' : 'problems'}</span><span>Theme: {theme?.name}</span><span>Desktop positions save automatically</span></div>
  </section>
}

function StickyNote({ note, index, onEdit, onToggle, onDelete, onDragStart, onSelect }) {
  const [showMeaning, setShowMeaning] = useState(false)
  const rotation = note.rotation || 0
  const position = { left: `${note.x || 0}px`, top: `${note.y || 0}px`, zIndex: note.zIndex || index + 1, '--rotation': `${rotation}deg` }
  return <article className={`sticky-note note-${(index % 5) + 1} ${note.completed ? 'completed' : ''}`} style={position} onClick={(event) => { if (!event.target.closest('button')) onSelect() }}>
    <div className="note-tape" aria-hidden="true" />
    <div className="note-top"><button className="drag-handle" aria-label="Drag note" title="Drag note" onPointerDown={(event) => onDragStart(event, note)}>⠿</button><span>{note.type}</span><button className="note-more" aria-label="Edit note" onClick={() => onEdit(note)}>•••</button></div>
    <p className="note-text">{note.text}</p>
    {note.deadline && <time className="note-deadline" dateTime={note.deadline}>◷ {new Date(note.deadline).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}</time>}
    {note.reactionText && <button className="note-reaction" lang={note.reactionLanguage} dir={note.reactionDirection} onClick={() => setShowMeaning((current) => !current)}>{showMeaning ? note.reactionMeaning : note.reactionText}</button>}
    <div className="note-actions"><button onClick={() => onToggle(note)}>{note.completed ? '↶ Undo' : '✓ Done'}</button><button onClick={() => onEdit(note)}>Edit</button><button aria-label="Delete note" onClick={() => onDelete(note.id)}>×</button></div>
    <span className="note-index">0{index + 1}</span>
  </article>
}

function NoteEditor({ note, loading, onClose, onSubmit, onDelete }) {
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <section className="note-editor" role="dialog" aria-modal="true" aria-labelledby="note-editor-title">
      <button className="modal-close" aria-label="Close editor" onClick={onClose}>×</button>
      <p className="eyebrow">{note.id ? 'REVISING THE PLAN' : 'A NEW LITTLE PROBLEM'}</p><h2 id="note-editor-title">{note.id ? 'Edit your note' : 'Add a note'}</h2>
      <form onSubmit={onSubmit}>
        <label>Note text<textarea name="text" rows="4" maxLength="1000" required autoFocus defaultValue={note.text} placeholder="Write it down before you forget (again)..." /></label>
        <div className="editor-fields"><label>Type<select name="type" defaultValue={note.type}><option value="TASK">Task</option><option value="REMINDER">Reminder</option><option value="THOUGHT">Thought</option></select></label><label>Deadline (optional)<input type="datetime-local" name="deadline" defaultValue={note.deadline || ''} /></label></div>
        <div className="editor-buttons"><button className="button button-dark" disabled={loading}>{note.id ? 'Save Changes' : 'Add Note'}</button>{onDelete && <button className="button delete-button" type="button" onClick={onDelete}>Delete</button>}</div>
      </form>
    </section>
  </div>
}

function ChaosPopup({ language, closeRef, onClose }) {
  const [caption, setCaption] = useState(null)
  const [captionError, setCaptionError] = useState('')
  const [showMeaning, setShowMeaning] = useState(false)
  useEffect(() => {
    setShowMeaning(false)
    setCaptionError('')
    api(`/api/reactions?trigger=CHAOS&language=${language}`)
      .then(setCaption)
      .catch(() => {
        setCaption(null)
        setCaptionError('Multilingual commentary could not load. Your chaos remains intact.')
      })
  }, [language])
  function keepFocusInside(event) {
    if (event.key !== 'Tab') return
    const controls = [...event.currentTarget.querySelectorAll('button:not([disabled])')]
    const first = controls[0]
    const last = controls[controls.length - 1]
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first.focus()
    }
  }
  return <div className="chaos-overlay">
    <section className="chaos-popup" role="dialog" aria-modal="true" aria-labelledby="chaos-title" aria-describedby="chaos-description" onKeyDown={keepFocusInside}>
      <button ref={closeRef} className="chaos-close" aria-label="Close chaos popup" onClick={onClose}>×</button>
      <div className="chaos-cat"><img src="/assets/stickers/cat.svg" alt="A tiny cat overwhelmed by planner notes" /></div>
      <p className="eyebrow">A TOTALLY PREDICTABLE OUTCOME</p>
      <h2 id="chaos-title">CONGRATULATIONS.<br /><em>IT’S WORSE.</em></h2>
      <p id="chaos-description">Your deadlines are still here. They’re just harder to emotionally process now.</p>
      {caption && <button className="chaos-caption" type="button" lang={caption.language} dir={caption.direction} onClick={() => setShowMeaning((visible) => !visible)}>{showMeaning ? caption.englishMeaning : caption.text}<small>{showMeaning ? 'English meaning' : LANGUAGE_NAMES[caption.language]} · tap</small></button>}
      {captionError && <p className="chaos-caption-error" role="status">{captionError}</p>}
      <div className="language-dots" aria-label={`Commentary in ${LANGUAGE_NAMES[language]}`}><span>{LANGUAGE_NAMES[language]}</span><i>✦</i></div>
      <button className="button button-dark" onClick={onClose}>Let Me Back Into My Mess</button>
    </section>
  </div>
}

function ReactionBubble({ reaction }) {
  const [showMeaning, setShowMeaning] = useState(false)
  return <button className="reaction-bubble" onClick={() => setShowMeaning((visible) => !visible)} lang={reaction.language} dir={reaction.direction} aria-label={`Reaction in ${LANGUAGE_NAMES[reaction.language]}`}>
    <span>{showMeaning ? reaction.englishMeaning : reaction.text}</span><small>{showMeaning ? 'English meaning' : `${LANGUAGE_NAMES[reaction.language]} · tap`}</small>
  </button>
}

const container = document.getElementById('root')
const root = import.meta.hot?.data.root || createRoot(container)
if (import.meta.hot) import.meta.hot.dispose((data) => { data.root = root })
root.render(<App />)
