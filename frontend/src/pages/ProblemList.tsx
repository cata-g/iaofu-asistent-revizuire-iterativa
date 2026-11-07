import {
  Alert,
  Box,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api'
import { CATEGORY_LABELS, DIFFICULTY_LABELS } from '../labels'

export default function ProblemList() {
  const [category, setCategory] = useState('')
  const [difficulty, setDifficulty] = useState('')
  const problems = useQuery({
    queryKey: ['problems', category, difficulty],
    queryFn: () => api.problems({ category, difficulty }),
  })

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Probleme
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Alege o problemă și trimite soluții succesive. După fiecare versiune vezi ce teste trec și ce s-a schimbat.
      </Typography>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 3 }}>
        <TextField select label="Categorie" value={category} onChange={(e) => setCategory(e.target.value)}
          sx={{ minWidth: 220 }} size="small">
          <MenuItem value="">Toate</MenuItem>
          {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
            <MenuItem key={value} value={value}>{label}</MenuItem>
          ))}
        </TextField>
        <TextField select label="Dificultate" value={difficulty} onChange={(e) => setDifficulty(e.target.value)}
          sx={{ minWidth: 160 }} size="small">
          <MenuItem value="">Toate</MenuItem>
          {Object.entries(DIFFICULTY_LABELS).map(([value, label]) => (
            <MenuItem key={value} value={value}>{label}</MenuItem>
          ))}
        </TextField>
      </Stack>

      {problems.isPending && <CircularProgress />}
      {problems.isError && <Alert severity="error">Lista de probleme nu s-a putut încărca: {problems.error.message}</Alert>}
      {problems.data?.length === 0 && <Typography color="text.secondary">Nicio problemă pentru filtrele alese.</Typography>}

      <Stack spacing={1.5}>
        {problems.data?.map((p) => (
          <Card key={p.id} variant="outlined">
            <CardActionArea component={Link} to={`/problems/${p.id}`}>
              <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Typography variant="h6" component="h2" sx={{ flexGrow: 1 }}>{p.title}</Typography>
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <Chip size="small" label={CATEGORY_LABELS[p.category] ?? p.category} />
                  <Chip size="small" variant="outlined" label={DIFFICULTY_LABELS[p.difficulty] ?? p.difficulty} />
                </Box>
              </CardContent>
            </CardActionArea>
          </Card>
        ))}
      </Stack>
    </Container>
  )
}
