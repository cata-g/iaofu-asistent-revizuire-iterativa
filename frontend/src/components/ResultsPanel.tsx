import {
  Alert,
  Box,
  Button,
  Chip,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import type { TestReport, TestResult } from '../api'
import { GROUP_LABELS, formatInput, formatJson, formatMs } from '../labels'

const COLLAPSED_FAILURES = 3

function FailedTest({ test }: { test: TestResult }) {
  return (
    <Box sx={{ p: 1.5, border: 1, borderColor: 'divider', borderRadius: 1 }}>
      <Stack direction="row" spacing={1} sx={{ mb: 1, alignItems: 'center' }}>
        <Chip size="small" color="error" label="picat" />
        <Chip size="small" variant="outlined" label={GROUP_LABELS[test.group] ?? test.group} />
        <Typography variant="body2" color="text.secondary">{test.id}</Typography>
      </Stack>
      <Box component="dl" sx={{ m: 0, display: 'grid', gridTemplateColumns: 'max-content 1fr', columnGap: 2, rowGap: 0.5,
        fontFamily: 'monospace', fontSize: 13, '& dt': { color: 'text.secondary' }, '& dd': { m: 0, wordBreak: 'break-all' } }}>
        <dt>input</dt><dd>{formatInput(test.input)}</dd>
        {test.expected !== undefined && <><dt>așteptat</dt><dd>{formatJson(test.expected)}</dd></>}
        {test.error
          ? <><dt>eroare</dt><dd>{test.error}</dd></>
          : <><dt>obținut</dt><dd>{test.actual === undefined || test.actual === null ? 'None' : formatJson(test.actual)}</dd></>}
      </Box>
    </Box>
  )
}

function efficiencyMessage(report: TestReport, timeLimitMs: number): string {
  const firstTimeout = report.timings.find((t) => t.status === 'timeout')
  if (firstTimeout) {
    return `Prea lent: la n = ${firstTimeout.n.toLocaleString('ro-RO')} depășește limita de ${timeLimitMs} ms.`
  }
  return 'Termină în limita de timp pe toate inputurile mari.'
}

export default function ResultsPanel({ report, timeLimitMs }: { report: TestReport; timeLimitMs: number }) {
  const [showAll, setShowAll] = useState(false)
  if (report.status !== 'ok') {
    return (
      <Alert severity="error">
        {report.status === 'compile_error' ? 'Codul nu se poate rula: ' : 'Execuția a fost oprită: '}
        <Box component="span" sx={{ fontFamily: 'monospace' }}>{report.error}</Box>
      </Alert>
    )
  }
  const failed = report.tests.filter((t) => !t.passed)
  const allPassed = failed.length === 0
  const shown = showAll ? failed : failed.slice(0, COLLAPSED_FAILURES)

  return (
    <Stack spacing={2}>
      <Alert severity={allPassed ? 'success' : 'warning'}>
        {report.summary.passed} din {report.summary.total} teste trec. {efficiencyMessage(report, timeLimitMs)}
      </Alert>

      {failed.length > 0 && (
        <Stack spacing={1}>
          <Typography variant="subtitle2">Teste picate ({failed.length})</Typography>
          {shown.map((t) => <FailedTest key={t.id} test={t} />)}
          {failed.length > COLLAPSED_FAILURES && (
            <Button size="small" onClick={() => setShowAll(!showAll)} sx={{ alignSelf: 'flex-start' }}>
              {showAll ? 'Arată mai puține' : `Arată toate cele ${failed.length}`}
            </Button>
          )}
        </Stack>
      )}

      <Box>
        <Typography variant="subtitle2" gutterBottom>
          Timpi pe inputuri mari
          {report.slope !== null && (
            <Typography component="span" variant="body2" color="text.secondary">
              {' '}· timpul crește cu aproximativ n<sup>{report.slope.toFixed(1)}</sup>
            </Typography>
          )}
        </Typography>
        <Table size="small" sx={{ maxWidth: 420 }}>
          <TableHead>
            <TableRow><TableCell>n</TableCell><TableCell>timp</TableCell><TableCell>stare</TableCell></TableRow>
          </TableHead>
          <TableBody>
            {report.timings.map((t) => (
              <TableRow key={t.n}>
                <TableCell>{t.n.toLocaleString('ro-RO')}</TableCell>
                <TableCell>{formatMs(t.timeMs)}</TableCell>
                <TableCell>{TIMING_LABELS[t.status] ?? t.status}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>
    </Stack>
  )
}

const TIMING_LABELS: Record<string, string> = {
  ok: 'corect',
  timeout: 'depășește timpul',
  error: 'eroare',
  wrong: 'răspuns greșit',
  skipped: 'nerulat',
}
