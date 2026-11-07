// The study conductor opens the app with ?mode=...&participant=...; both are kept
// for the browser tab and never shown to the participant (F18).

const MODE_KEY = 'study.mode'
const PARTICIPANT_KEY = 'study.participant'
const MODES = ['proposed', 'baseline_a', 'baseline_b']

function read(key: string): string | null {
  try {
    return sessionStorage.getItem(key)
  } catch {
    return null
  }
}

function write(key: string, value: string) {
  try {
    sessionStorage.setItem(key, value)
  } catch {
    // storage unavailable; the values only last for this page load
  }
}

let mode: string | null = null
let participant: string | null = null

export function initStudyParams() {
  const params = new URLSearchParams(window.location.search)
  const urlMode = params.get('mode')
  const urlParticipant = params.get('participant')
  if (urlMode && MODES.includes(urlMode)) write(MODE_KEY, urlMode)
  if (urlParticipant) write(PARTICIPANT_KEY, urlParticipant)
  mode = (urlMode && MODES.includes(urlMode) ? urlMode : null) ?? read(MODE_KEY)
  participant = urlParticipant ?? read(PARTICIPANT_KEY)
  if (urlMode || urlParticipant) {
    // Drop the parameters from the address bar so the participant does not see them.
    window.history.replaceState(null, '', window.location.pathname)
  }
}

export function studyMode(): string | null {
  return mode
}

export function studyParticipant(): string | null {
  return participant
}
