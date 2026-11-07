import { Box, Chip, Paper, Stack, Typography } from '@mui/material'
import type { Classification, ClassifiedTest } from '../api'
import { GROUP_LABELS } from '../labels'

const EFFICIENCY_TEXT: Record<string, string> = {
  'too_slow->ok': 'Acum termină în limita de timp pe inputurile mari.',
  'ok->too_slow': 'A devenit prea lentă pe inputurile mari.',
  'too_slow->too_slow': 'E în continuare prea lentă pe inputurile mari.',
}

function TestChips({ tests, color }: { tests: ClassifiedTest[]; color: 'success' | 'error' | 'warning' }) {
  return (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
      {tests.map((t) => (
        <Chip key={t.testId} size="small" color={color} variant="outlined"
          label={`${t.testId} · ${GROUP_LABELS[t.group] ?? t.group}`} />
      ))}
    </Box>
  )
}

function Row({ title, tests, color, empty }: {
  title: string; tests: ClassifiedTest[]; color: 'success' | 'error' | 'warning'; empty: string
}) {
  return (
    <Box>
      <Typography variant="body2" sx={{ fontWeight: 600, mb: 0.5 }}>{title} ({tests.length})</Typography>
      {tests.length > 0 ? <TestChips tests={tests} color={color} />
        : <Typography variant="body2" color="text.secondary">{empty}</Typography>}
    </Box>
  )
}

export default function ClassificationPanel({ classification }: { classification: Classification }) {
  const efficiency = EFFICIENCY_TEXT[`${classification.efficiency.before}->${classification.efficiency.after}`]
  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Typography variant="subtitle2" gutterBottom>
        Față de versiunea {classification.previousVersion}
      </Typography>
      <Stack spacing={1.5}>
        <Row title="Rezolvat" tests={classification.resolved} color="success" empty="Niciun test nou trecut." />
        <Row title="Regresie nouă" tests={classification.regression} color="error" empty="Nicio regresie." />
        <Row title="Rămas" tests={classification.remaining} color="warning" empty="Nimic rămas din versiunea anterioară." />
        {efficiency && <Typography variant="body2">{efficiency}</Typography>}
      </Stack>
    </Paper>
  )
}
