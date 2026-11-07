import { Alert, Box, Button, Chip, Paper, Stack, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { api, type ObservationView } from '../api'
import { OBSERVATION_TYPE_LABELS, formatInput, formatJson } from '../labels'

const LEVEL_NAMES = ['Orientare', 'Direcție', 'Concret']

interface Props {
  observation: ObservationView
  readOnly?: boolean
}

export default function ObservationCard({ observation, readOnly = false }: Props) {
  const [hints, setHints] = useState(observation.hints)
  const [verdict, setVerdict] = useState(observation.verdict)
  const nextHint = useMutation({
    mutationFn: () => api.nextHint(observation.id),
    onSuccess: (hint) => setHints((current) => [...current, { level: hint.level, text: hint.text }]),
  })
  const feedback = useMutation({
    mutationFn: (value: 'useful' | 'false') => api.feedback(observation.id, value),
    onSuccess: (_, value) => setVerdict(value),
  })
  const evidence = observation.evidence

  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack direction="row" spacing={1} sx={{ mb: 1.5, alignItems: 'center' }}>
        <Chip size="small" color="warning" label={OBSERVATION_TYPE_LABELS[observation.type] ?? observation.type} />
        {observation.line !== null && <Typography variant="body2" color="text.secondary">linia {observation.line}</Typography>}
      </Stack>

      {evidence && (
        <Box component="dl" sx={{ m: 0, mb: 1.5, p: 1.5, bgcolor: 'action.hover', borderRadius: 1, display: 'grid',
          gridTemplateColumns: 'max-content 1fr', columnGap: 2, rowGap: 0.5, fontSize: 13,
          '& dt': { color: 'text.secondary' }, '& dd': { m: 0, fontFamily: 'monospace', wordBreak: 'break-all' } }}>
          {evidence.description
            ? <><dt>dovadă</dt><Box component="dd" sx={{ fontFamily: 'inherit !important' }}>{evidence.description}</Box></>
            : <>
              <dt>pică pe</dt><dd>{formatInput(evidence.input)}</dd>
              <dt>obținut</dt><dd>{evidence.actual === undefined ? 'eroare' : formatJson(evidence.actual)}</dd>
              {evidence.expected !== undefined && <><dt>corect</dt><dd>{formatJson(evidence.expected)}</dd></>}
            </>}
        </Box>
      )}

      <Typography variant="body2" sx={{ mb: 1.5 }}><strong>Risc:</strong> {observation.risk}</Typography>

      <Stack spacing={1}>
        {hints.map((h) => (
          <Box key={h.level} sx={{ pl: 1.5, borderLeft: 3, borderColor: 'primary.main' }}>
            <Typography variant="caption" color="text.secondary">
              Indiciu {h.level} · {LEVEL_NAMES[h.level - 1]}
            </Typography>
            <Typography variant="body2">{h.text}</Typography>
          </Box>
        ))}
      </Stack>

      {nextHint.isError && <Alert severity="error" sx={{ mt: 1 }}>{nextHint.error.message}</Alert>}
      {!readOnly && (
        <Stack direction="row" sx={{ mt: 1.5, alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 1 }}>
          {hints.length < observation.levelsAvailable
            ? <Button size="small" onClick={() => nextHint.mutate()} disabled={nextHint.isPending}>Mai mult ajutor</Button>
            : <span />}
          <ToggleButtonGroup size="small" exclusive value={verdict}
            onChange={(_, value: 'useful' | 'false' | null) => value && feedback.mutate(value)}>
            <ToggleButton value="useful">Utilă</ToggleButton>
            <ToggleButton value="false">Falsă</ToggleButton>
          </ToggleButtonGroup>
        </Stack>
      )}
    </Paper>
  )
}
