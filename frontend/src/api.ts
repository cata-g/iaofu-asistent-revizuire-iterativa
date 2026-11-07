// Types and calls for the backend API (docs/contracte.md section 6).

export type Json = unknown

export interface ProblemSummary {
  id: string
  title: string
  category: string
  difficulty: string
}

export interface ProblemDetail extends ProblemSummary {
  statement: string
  examples: { input: Record<string, Json>; output: Json; explanation?: string }[]
  constraints: string[]
  timeLimitMs: number
  signature: { function: string; params: { name: string; type: string }[]; returns: string }
  starterCode: string
}

export interface TestResult {
  id: string
  group: 'basic' | 'edge' | 'large'
  passed: boolean
  input?: Json
  expected?: Json
  actual?: Json
  error?: string | null
  timeMs?: number | null
}

export interface Timing {
  n: number
  status: 'ok' | 'timeout' | 'error' | 'wrong' | 'skipped'
  timeMs: number | null
}

export interface TestReport {
  status: 'ok' | 'compile_error' | 'error'
  error: string | null
  summary: { passed: number; total: number }
  tests: TestResult[]
  timings: Timing[]
  slope: number | null
  efficiency: 'ok' | 'too_slow' | 'unknown'
}

export interface ClassifiedTest {
  testId: string
  group: string
}

export interface Classification {
  previousVersion: number
  resolved: ClassifiedTest[]
  regression: ClassifiedTest[]
  remaining: ClassifiedTest[]
  efficiency: { before: string; after: string }
}

export interface HintView {
  level: number
  text: string
}

export interface ObservationView {
  id: string
  type: 'correctness' | 'edge_case' | 'efficiency'
  line: number | null
  evidence: { input?: Json; actual?: Json; expected?: Json; description?: string } | null
  risk: string
  hints: HintView[]
  levelsAvailable: number
  verdict: 'useful' | 'false' | null
}

export type LlmStatus = 'ok' | 'not_used' | 'failed' | 'budget_exceeded'

export interface SubmissionResponse {
  submissionId: string
  version: number
  report: TestReport
  classification: Classification | null
  observations: ObservationView[]
  assistantMessage: string | null
  llmStatus: LlmStatus
}

export interface VersionView {
  version: number
  submissionId: string
  code: string
  createdAt: string
  summary: { passed: number; total: number }
  efficiency: string
  report: TestReport
  classification: Classification | null
  observations: ObservationView[]
  assistantMessage: string | null
}

export interface History {
  sessionId: string
  problemId: string
  finished: boolean
  versions: VersionView[]
}

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const res = await fetch(`/api${path}`, {
    method,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!res.ok) {
    const text = await res.text()
    let message = text
    try {
      message = (JSON.parse(text) as { error?: string }).error ?? text
    } catch {
      // not JSON
    }
    throw new ApiError(res.status, message || res.statusText)
  }
  return (res.status === 204 ? undefined : await res.json()) as T
}

export const api = {
  problems: (filters: { category?: string; difficulty?: string } = {}) => {
    const params = new URLSearchParams()
    if (filters.category) params.set('category', filters.category)
    if (filters.difficulty) params.set('difficulty', filters.difficulty)
    const query = params.toString()
    return request<ProblemSummary[]>('GET', `/problems${query ? `?${query}` : ''}`)
  },
  problem: (id: string) => request<ProblemDetail>('GET', `/problems/${id}`),
  createSession: (problemId: string, mode: string | null, participantId: string | null) =>
    request<{ sessionId: string; problemId: string }>('POST', '/sessions', { problemId, mode, participantId }),
  submit: (sessionId: string, code: string) =>
    request<SubmissionResponse>('POST', `/sessions/${sessionId}/submissions`, { code }),
  finish: (sessionId: string) => request<void>('POST', `/sessions/${sessionId}/finish`),
  history: (sessionId: string) => request<History>('GET', `/sessions/${sessionId}/history`),
  nextHint: (observationId: string) =>
    request<HintView & { levelsAvailable: number }>('POST', `/observations/${observationId}/next-hint`),
  feedback: (observationId: string, verdict: 'useful' | 'false') =>
    request<void>('POST', `/observations/${observationId}/feedback`, { verdict }),
}
