import {
  Alert,
  AppBar,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Paper,
  Stack,
  Tab,
  Tabs,
  Toolbar,
  Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import Markdown from 'react-markdown'
import { Link, useParams } from 'react-router'
import { api, ApiError, type History, type ProblemDetail, type SubmissionResponse, type VersionView } from '../api'
import ClassificationPanel from '../components/ClassificationPanel'
import CodeEditor from '../components/CodeEditor'
import HistoryPanel from '../components/HistoryPanel'
import ObservationCard from '../components/ObservationCard'
import ProblemStatement from '../components/ProblemStatement'
import ResultsPanel from '../components/ResultsPanel'
import {
  forgetSession,
  savedDraft,
  savedSession,
  saveDraft,
  saveSession,
  studyMode,
  studyParticipant,
} from '../study'

const LLM_STATUS_TEXT: Record<string, string> = {
  failed: 'Asistentul nu a putut genera observații pentru această versiune. Rezultatele testelor sunt complete.',
  budget_exceeded: 'Asistentul nu mai este disponibil. Rezultatele testelor sunt complete.',
}

interface SessionState {
  sessionId: string
  /** Null for a new session; otherwise what the session already holds. */
  history: History | null
}

/** Continues the session kept for this tab, or starts a new one. */
async function openSession(problemId: string): Promise<SessionState> {
  const saved = savedSession(problemId)
  if (saved) {
    try {
      return { sessionId: saved, history: await api.history(saved) }
    } catch (e) {
      // the database was reset: start over
      if (!(e instanceof ApiError && e.status === 404)) throw e
      forgetSession(problemId)
    }
  }
  const created = await api.createSession(problemId, studyMode(), studyParticipant())
  saveSession(problemId, created.sessionId)
  return { sessionId: created.sessionId, history: null }
}

function asResponse(v: VersionView): SubmissionResponse {
  return {
    submissionId: v.submissionId,
    version: v.version,
    report: v.report,
    classification: v.classification,
    observations: v.observations,
    assistantMessage: v.assistantMessage,
    llmStatus: v.llmStatus,
  }
}

function Feedback({ latest, timeLimitMs }: { latest: SubmissionResponse; timeLimitMs: number }) {
  return (
    <Stack spacing={2}>
      {/* a version that did not run has nothing to compare */}
      {latest.classification && latest.report.status === 'ok' && <ClassificationPanel classification={latest.classification} />}
      {latest.observations.length > 0 && (
        <Stack spacing={1.5}>
          <Typography variant="subtitle2">Observații</Typography>
          {latest.observations.map((o) => <ObservationCard key={o.id} observation={o} />)}
        </Stack>
      )}
      {latest.assistantMessage && (
        <Paper variant="outlined" sx={{ p: 2, '& pre': { overflow: 'auto', bgcolor: 'action.hover', p: 1, borderRadius: 1 },
          '& p:first-of-type': { mt: 0 } }}>
          <Typography variant="subtitle2" gutterBottom>Asistent</Typography>
          <Markdown>{latest.assistantMessage}</Markdown>
        </Paper>
      )}
      {LLM_STATUS_TEXT[latest.llmStatus] && <Alert severity="info">{LLM_STATUS_TEXT[latest.llmStatus]}</Alert>}
      <ResultsPanel report={latest.report} timeLimitMs={timeLimitMs} />
    </Stack>
  )
}

export default function Workspace() {
  const { id = '' } = useParams()
  const problem = useQuery({ queryKey: ['problem', id], queryFn: () => api.problem(id) })
  // Cached so React StrictMode does not open two sessions; refetched on a new page load.
  const session = useQuery({
    queryKey: ['session', id],
    queryFn: () => openSession(id),
    staleTime: Infinity,
    gcTime: 0,
    retry: false,
    enabled: problem.isSuccess,
  })

  if (problem.isError) return <Alert severity="error" sx={{ m: 4 }}>{problem.error.message}</Alert>
  if (session.isError) {
    return <Alert severity="error" sx={{ m: 4 }}>Sesiunea nu a pornit: {session.error.message}</Alert>
  }
  if (!problem.data || !session.data) return <CircularProgress sx={{ m: 4 }} />
  return <WorkspaceBody key={session.data.sessionId} problem={problem.data} session={session.data} />
}

function WorkspaceBody({ problem: p, session }: { problem: ProblemDetail; session: SessionState }) {
  const queryClient = useQueryClient()
  const { sessionId, history } = session
  const lastVersion = history?.versions.at(-1)

  const [code, setCode] = useState(() => savedDraft(sessionId) ?? lastVersion?.code ?? p.starterCode)
  const [latest, setLatest] = useState<SubmissionResponse | null>(lastVersion ? asResponse(lastVersion) : null)
  const [tab, setTab] = useState<'feedback' | 'history'>('feedback')
  const [finished, setFinished] = useState(history?.finished ?? false)
  const [confirmFinish, setConfirmFinish] = useState(false)
  // A double click fires twice before the button is disabled.
  const submitting = useRef(false)

  const submit = useMutation({
    mutationFn: () => api.submit(sessionId, code),
    onSuccess: (response) => {
      setLatest(response)
      setTab('feedback')
      void queryClient.invalidateQueries({ queryKey: ['history', sessionId] })
    },
    onSettled: () => {
      submitting.current = false
    },
  })
  const finish = useMutation({
    mutationFn: () => api.finish(sessionId),
    onSuccess: () => setFinished(true),
  })

  const onSubmit = () => {
    if (submitting.current) return
    submitting.current = true
    submit.mutate()
  }
  const onCodeChange = (value: string) => {
    setCode(value)
    saveDraft(sessionId, value)
  }

  const empty = code.trim() === ''
  const highlight = latest?.observations.map((o) => o.line).filter((l): l is number => l !== null) ?? []

  return (
    <Box sx={{ height: { xs: 'auto', lg: '100vh' }, minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="static" color="default" elevation={0} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Toolbar variant="dense" sx={{ gap: 2 }}>
          <Button component={Link} to="/" size="small">← Probleme</Button>
          <Typography variant="h6" component="h1" sx={{ flexGrow: 1 }} noWrap>{p.title}</Typography>
          {latest && <Typography variant="body2" color="text.secondary">Versiunea {latest.version}</Typography>}
          <Button variant="outlined" size="small" disabled={finished} onClick={() => setConfirmFinish(true)}>
            Am terminat
          </Button>
        </Toolbar>
      </AppBar>

      <Box sx={{ flex: 1, minHeight: 0, display: 'grid',
        gridTemplateColumns: { xs: '1fr', lg: '320px 1fr 420px', xl: '360px 1fr 480px' } }}>
        <Box sx={{ p: 2, overflow: 'auto', borderRight: { lg: 1 }, borderBottom: { xs: 1, lg: 0 }, borderColor: 'divider' }}>
          <ProblemStatement problem={p} />
        </Box>

        <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: { xs: 480, lg: 0 } }}>
          <Box sx={{ flex: 1, minHeight: 0 }}>
            <CodeEditor value={code} onChange={onCodeChange} highlightLines={highlight} readOnly={finished} />
          </Box>
          <Stack direction="row" spacing={2} sx={{ p: 1.5, borderTop: 1, borderColor: 'divider', alignItems: 'center' }}>
            <Button variant="contained" onClick={onSubmit} disabled={submit.isPending || finished || empty}>
              {submit.isPending ? 'Se rulează…' : 'Trimite versiunea'}
            </Button>
            {submit.isPending && <CircularProgress size={20} />}
            {empty && !finished && <Typography variant="body2" color="text.secondary">Scrie cod ca să poți trimite.</Typography>}
          </Stack>
        </Box>

        <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: 0, borderLeft: { lg: 1 }, borderTop: { xs: 1, lg: 0 },
          borderColor: 'divider' }}>
          <Tabs value={tab} onChange={(_, v: 'feedback' | 'history') => setTab(v)}
            sx={{ borderBottom: 1, borderColor: 'divider', minHeight: 40 }}>
            <Tab value="feedback" label="Feedback" sx={{ minHeight: 40 }} />
            <Tab value="history" label="Versiuni" sx={{ minHeight: 40 }} />
          </Tabs>
          <Box sx={{ p: 2, overflow: 'auto', flex: 1 }}>
            {finished && <Alert severity="info" sx={{ mb: 2 }}>Ai marcat problema ca terminată. Mulțumim!</Alert>}
            {submit.isError && <Alert severity="error" sx={{ mb: 2 }}>{submit.error.message}</Alert>}
            {finish.isError && <Alert severity="error" sx={{ mb: 2 }}>{finish.error.message}</Alert>}
            {tab === 'feedback' && !latest && !submit.isPending && (
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography color="text.secondary">
                  Scrie o soluție și trimite-o. Rezultatele apar aici.
                </Typography>
              </Paper>
            )}
            {tab === 'feedback' && latest && <Feedback key={latest.submissionId} latest={latest} timeLimitMs={p.timeLimitMs} />}
            {tab === 'history' && <HistoryPanel sessionId={sessionId} />}
          </Box>
        </Box>
      </Box>

      <Dialog open={confirmFinish} onClose={() => setConfirmFinish(false)}>
        <DialogTitle>Marchezi problema ca terminată?</DialogTitle>
        <DialogContent>
          <DialogContentText>După asta nu mai poți trimite versiuni noi pentru această problemă.</DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmFinish(false)}>Nu încă</Button>
          <Button variant="contained" onClick={() => { finish.mutate(); setConfirmFinish(false) }}>Da, am terminat</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
