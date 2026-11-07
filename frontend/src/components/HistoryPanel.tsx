import { DiffEditor } from '@monaco-editor/react'
import { Alert, Box, CircularProgress, MenuItem, Stack, TextField, Typography, useColorScheme } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../api'

/** Lists the versions and compares any two of them (F10). */
export default function HistoryPanel({ sessionId }: { sessionId: string }) {
  const history = useQuery({ queryKey: ['history', sessionId], queryFn: () => api.history(sessionId) })
  const [left, setLeft] = useState<number | null>(null)
  const [right, setRight] = useState<number | null>(null)
  const { mode, systemMode } = useColorScheme()
  const dark = (mode === 'system' ? systemMode : mode) === 'dark'

  if (history.isPending) return <CircularProgress />
  if (history.isError) return <Alert severity="error">{history.error.message}</Alert>

  const versions = history.data.versions
  if (versions.length === 0) {
    return <Typography color="text.secondary">Încă nu ai trimis nicio versiune.</Typography>
  }
  const last = versions[versions.length - 1].version
  const leftVersion = left ?? Math.max(1, last - 1)
  const rightVersion = right ?? last
  const codeOf = (v: number) => versions.find((x) => x.version === v)?.code ?? ''

  const picker = (label: string, value: number, onChange: (v: number) => void) => (
    <TextField select size="small" label={label} value={value} onChange={(e) => onChange(Number(e.target.value))}
      sx={{ minWidth: 200 }}>
      {versions.map((v) => (
        <MenuItem key={v.version} value={v.version}>
          v{v.version} · {v.summary.passed}/{v.summary.total} teste
        </MenuItem>
      ))}
    </TextField>
  )

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={2}>
        {picker('Din', leftVersion, setLeft)}
        {picker('În', rightVersion, setRight)}
      </Stack>
      <Box sx={{ height: 420, border: 1, borderColor: 'divider' }}>
        <DiffEditor
          language="python"
          theme={dark ? 'vs-dark' : 'light'}
          original={codeOf(leftVersion)}
          modified={codeOf(rightVersion)}
          options={{ readOnly: true, renderSideBySide: false, minimap: { enabled: false }, scrollBeyondLastLine: false }}
        />
      </Box>
      <Box component="ol" sx={{ m: 0, pl: 3 }}>
        {versions.map((v) => (
          <li key={v.version}>
            <Typography variant="body2">
              v{v.version}: {v.summary.passed} din {v.summary.total} teste
              {v.efficiency === 'too_slow' ? ', prea lentă' : ''}
              {' · '}{new Date(v.createdAt).toLocaleTimeString('ro-RO')}
            </Typography>
          </li>
        ))}
      </Box>
    </Stack>
  )
}
