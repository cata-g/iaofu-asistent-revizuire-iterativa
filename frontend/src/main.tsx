import { CssBaseline, ThemeProvider, createTheme } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router'
import './index.css'
import './monaco'
import ProblemList from './pages/ProblemList'
import Workspace from './pages/Workspace'
import { initStudyParams } from './study'

initStudyParams()

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
})

const theme = createTheme({
  colorSchemes: { light: true, dark: true },
  shape: { borderRadius: 8 },
  typography: { fontFamily: 'system-ui, "Segoe UI", Roboto, sans-serif' },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider theme={theme} defaultMode="system">
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<ProblemList />} />
            <Route path="/problems/:id" element={<Workspace />} />
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
)
