import { Box, Chip, Stack, Typography } from '@mui/material'
import Markdown from 'react-markdown'
import type { ProblemDetail } from '../api'
import { CATEGORY_LABELS, DIFFICULTY_LABELS, formatInput, formatJson } from '../labels'

export default function ProblemStatement({ problem }: { problem: ProblemDetail }) {
  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1}>
        <Chip size="small" label={CATEGORY_LABELS[problem.category] ?? problem.category} />
        <Chip size="small" variant="outlined" label={DIFFICULTY_LABELS[problem.difficulty] ?? problem.difficulty} />
      </Stack>
      <Box sx={{ '& p': { mt: 0 }, '& code': { fontFamily: 'monospace', bgcolor: 'action.hover', px: 0.5, borderRadius: 0.5 } }}>
        <Markdown>{problem.statement}</Markdown>
      </Box>

      <Box>
        <Typography variant="subtitle2" gutterBottom>Exemple</Typography>
        <Stack spacing={1}>
          {problem.examples.map((ex, i) => (
            <Box key={i} sx={{ p: 1.5, bgcolor: 'action.hover', borderRadius: 1, fontFamily: 'monospace', fontSize: 13 }}>
              <div>Input: {formatInput(ex.input)}</div>
              <div>Output: {formatJson(ex.output)}</div>
              {ex.explanation && <Box sx={{ fontFamily: 'body1.fontFamily', color: 'text.secondary', mt: 0.5 }}>{ex.explanation}</Box>}
            </Box>
          ))}
        </Stack>
      </Box>

      <Box>
        <Typography variant="subtitle2" gutterBottom>Constrângeri</Typography>
        <Box component="ul" sx={{ m: 0, pl: 3 }}>
          {problem.constraints.map((c) => <li key={c}><Typography variant="body2">{c}</Typography></li>)}
          <li><Typography variant="body2">Limita de timp: {problem.timeLimitMs} ms pe inputul mare.</Typography></li>
        </Box>
      </Box>
    </Stack>
  )
}
