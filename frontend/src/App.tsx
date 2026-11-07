import Editor from '@monaco-editor/react'
import { useEffect, useState } from 'react'

const STARTER_CODE = `def two_sum(nums: list[int], target: int) -> list[int]:
    pass
`

export default function App() {
  const [backendStatus, setBackendStatus] = useState('checking...')
  const [code, setCode] = useState(STARTER_CODE)

  useEffect(() => {
    fetch('/api/health')
      .then((res) => res.json())
      .then((body: { status: string }) => setBackendStatus(body.status))
      .catch(() => setBackendStatus('unreachable'))
  }, [])

  return (
    <main style={{ padding: 16 }}>
      <h1>Asistent de revizuire</h1>
      <p>Backend: {backendStatus}</p>
      <Editor
        height="60vh"
        defaultLanguage="python"
        value={code}
        onChange={(value) => setCode(value ?? '')}
        options={{ minimap: { enabled: false }, fontSize: 14 }}
      />
    </main>
  )
}
