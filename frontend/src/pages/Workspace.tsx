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
  Toolbar,
  Typography,
} from '@mui/material'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, type SubmissionResponse } from '../api'
import CodeEditor from '../components/CodeEditor'
import ProblemStatement from '../components/ProblemStatement'
import ResultsPanel from '../components/ResultsPanel'
import { studyMode, studyParticipant } from '../study'

export default function Workspace() {
  const { id = '' } = useParams()
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
  const [finished, setFinished] = useState(false)
  const [confirmFinish, setConfirmFinish] = useState(false)

  const submit = useMutation({
    mutationFn: () => api.submit(session.data!.sessionId, code ?? problem.data!.starterCode),
    onSuccess: setLatest,
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

      <Box sx={{ flex: 1, minHeight: 0, display: 'grid', gridTemplateColumns: { xs: '1fr', md: '380px 1fr 460px' } }}>
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

        <Box sx={{ p: 2, overflow: 'auto', borderLeft: 1, borderColor: 'divider' }}>
          {finished && <Alert severity="info" sx={{ mb: 2 }}>Ai marcat problema ca terminată. Mulțumim!</Alert>}
          {submit.isError && <Alert severity="error" sx={{ mb: 2 }}>{submit.error.message}</Alert>}
          {!latest && !submit.isPending && (
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography color="text.secondary">
                Scrie o soluție și trimite-o. Rezultatele testelor apar aici.
              </Typography>
            </Paper>
          )}
          {latest && <ResultsPanel report={latest.report} timeLimitMs={p.timeLimitMs} />}
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
