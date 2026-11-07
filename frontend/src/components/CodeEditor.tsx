import Editor, { type OnMount } from '@monaco-editor/react'
import { useColorScheme } from '@mui/material'
import type * as Monaco from 'monaco-editor'
import { useEffect, useRef } from 'react'

interface Props {
  value: string
  onChange: (value: string) => void
  highlightLines?: number[]
  readOnly?: boolean
}

export default function CodeEditor({ value, onChange, highlightLines = [], readOnly = false }: Props) {
  const editorRef = useRef<Monaco.editor.IStandaloneCodeEditor | null>(null)
  const decorationsRef = useRef<Monaco.editor.IEditorDecorationsCollection | null>(null)
  const { mode, systemMode } = useColorScheme()
  const dark = (mode === 'system' ? systemMode : mode) === 'dark'

  const onMount: OnMount = (editor) => {
    editorRef.current = editor
    decorationsRef.current = editor.createDecorationsCollection()
  }

  useEffect(() => {
    decorationsRef.current?.set(highlightLines.map((line) => ({
      range: { startLineNumber: line, startColumn: 1, endLineNumber: line, endColumn: 1 },
      options: { isWholeLine: true, className: 'observation-line', glyphMarginClassName: 'observation-glyph' },
    })))
  }, [highlightLines])

  return (
    <Editor
      height="100%"
      defaultLanguage="python"
      theme={dark ? 'vs-dark' : 'light'}
      value={value}
      onChange={(v) => onChange(v ?? '')}
      onMount={onMount}
      options={{
        minimap: { enabled: false },
        fontSize: 14,
        readOnly,
        scrollBeyondLastLine: false,
        glyphMargin: true,
        tabSize: 4,
        insertSpaces: true,
      }}
    />
  )
}
