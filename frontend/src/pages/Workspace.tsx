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
import { useState } from 'react'
import Markdown from 'react-markdown'
import { Link, useParams } from 'react-router'
import { api, type SubmissionResponse } from '../api'
import ClassificationPanel from '../components/ClassificationPanel'
import CodeEditor from '../components/CodeEditor'
import HistoryPanel from '../components/HistoryPanel'
import ObservationCard from '../components/ObservationCard'
import ProblemStatement from '../components/ProblemStatement'
import ResultsPanel from '../components/ResultsPanel'
import { studyMode, studyParticipant } from '../study'

const LLM_STATUS_TEXT: Record<string, string> = {
  failed: 'Asistentul nu a putut genera observații pentru această versiune. Rezultatele testelor sunt complete.',
  budget_exceeded: 'Asistentul nu mai este disponibil. Rezultatele testelor sunt complete.',
}

function Feedback({ latest, timeLimitMs }: { latest: SubmissionResponse; timeLimitMs: number }) {
  return (
    <Stack spacing={2}>
      {latest.classification && <ClassificationPanel classification={latest.classification} />}
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
  const queryClient = useQueryClient()
  const problem = useQuery({ queryKey: ['problem', id], queryFn: () => api.problem(id) })
  // One session per problem and browser tab; cached so React StrictMode does not create two.
  const session = useQuery({
    queryKey: ['session', id],
    queryFn: () => api.createSession(id, studyMode(), studyParticipant()),
    staleTime: Infinity,
    gcTime: Infinity,
    enabled: problem.isSuccess,
  })

  const [code, setCode] = useState<string | null>(null)
  const [latest, setLatest] = useState<SubmissionResponse | null>(null)
  const [tab, setTab] = useState<'feedback' | 'history'>('feedback')
  const [finished, setFinished] = useState(false)
  const [confirmFinish, setConfirmFinish] = useState(false)

  const submit = useMutation({
    mutationFn: () => api.submit(session.data!.sessionId, code ?? problem.data!.starterCode),
    onSuccess: (response) => {
      setLatest(response)
      setTab('feedback')
      void queryClient.invalidateQueries({ queryKey: ['history', session.data!.sessionId] })
    },
  })
  const finish = useMutation({
    mutationFn: () => api.finish(session.data!.sessionId),
    onSuccess: () => setFinished(true),
  })

  if (problem.isPending) return <CircularProgress sx={{ m: 4 }} />
  if (problem.isError) return <Alert severity="error" sx={{ m: 4 }}>{problem.error.message}</Alert>

  const p = problem.data
  const highlight = latest?.observations.map((o) => o.line).filter((l): l is number => l !== null) ?? []

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="static" color="default" elevation={0} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Toolbar variant="dense" sx={{ gap: 2 }}>
          <Button component={Link} to="/" size="small">← Probleme</Button>
          <Typography variant="h6" component="h1" sx={{ flexGrow: 1 }} noWrap>{p.title}</Typography>
          {latest && <Typography variant="body2" color="text.secondary">Versiunea {latest.version}</Typography>}
          <Button variant="outlined" size="small" disabled={finished || !session.data}
            onClick={() => setConfirmFinish(true)}>
            Am terminat
          </Button>
        </Toolbar>
      </AppBar>

      <Box sx={{ flex: 1, minHeight: 0, display: 'grid', gridTemplateColumns: { xs: '1fr', md: '360px 1fr 480px' } }}>
        <Box sx={{ p: 2, overflow: 'auto', borderRight: 1, borderColor: 'divider' }}>
          <ProblemStatement problem={p} />
        </Box>

        <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: 400 }}>
          <Box sx={{ flex: 1, minHeight: 0 }}>
            <CodeEditor value={code ?? p.starterCode} onChange={setCode} highlightLines={highlight} readOnly={finished} />
          </Box>
          <Stack direction="row" spacing={2} sx={{ p: 1.5, borderTop: 1, borderColor: 'divider', alignItems: 'center' }}>
            <Button variant="contained" onClick={() => submit.mutate()}
              disabled={!session.data || submit.isPending || finished}>
              {submit.isPending ? 'Se rulează…' : 'Trimite versiunea'}
            </Button>
            {submit.isPending && <CircularProgress size={20} />}
            {session.isError && <Alert severity="error" sx={{ py: 0 }}>Sesiunea nu a pornit: {session.error.message}</Alert>}
          </Stack>
        </Box>

        <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: 0, borderLeft: 1, borderColor: 'divider' }}>
          <Tabs value={tab} onChange={(_, v: 'feedback' | 'history') => setTab(v)}
            sx={{ borderBottom: 1, borderColor: 'divider', minHeight: 40 }}>
            <Tab value="feedback" label="Feedback" sx={{ minHeight: 40 }} />
            <Tab value="history" label="Versiuni" sx={{ minHeight: 40 }} disabled={!session.data} />
          </Tabs>
          <Box sx={{ p: 2, overflow: 'auto', flex: 1 }}>
            {finished && <Alert severity="info" sx={{ mb: 2 }}>Ai marcat problema ca terminată. Mulțumim!</Alert>}
            {submit.isError && <Alert severity="error" sx={{ mb: 2 }}>{submit.error.message}</Alert>}
            {tab === 'feedback' && !latest && !submit.isPending && (
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography color="text.secondary">
                  Scrie o soluție și trimite-o. Rezultatele apar aici.
                </Typography>
              </Paper>
            )}
            {tab === 'feedback' && latest && <Feedback key={latest.submissionId} latest={latest} timeLimitMs={p.timeLimitMs} />}
            {tab === 'history' && session.data && <HistoryPanel sessionId={session.data.sessionId} />}
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
