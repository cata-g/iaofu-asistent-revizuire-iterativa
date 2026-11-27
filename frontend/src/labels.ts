// Romanian labels for values that come from the backend.

export const CATEGORY_LABELS: Record<string, string> = {
  hashing: 'Hashing',
  'two-pointers': 'Doi pointeri',
  sorting: 'Sortare și intervale',
  stack: 'Stivă',
  'binary-search': 'Căutare binară',
  graphs: 'Grafuri',
  dp: 'Programare dinamică',
}

export const DIFFICULTY_LABELS: Record<string, string> = {
  easy: 'Ușoară',
  medium: 'Medie',
}

export const GROUP_LABELS: Record<string, string> = {
  basic: 'de bază',
  edge: 'caz limită',
  large: 'input mare',
}

export const OBSERVATION_TYPE_LABELS: Record<string, string> = {
  correctness: 'Corectitudine',
  edge_case: 'Caz limită',
  efficiency: 'Eficiență',
}

const MAX_SHOWN = 300

/** Long values (a generated grid, a large list) would fill the screen; show their start. */
function shorten(text: string): string {
  return text.length <= MAX_SHOWN ? text
    : `${text.slice(0, MAX_SHOWN)} … (${text.length.toLocaleString('ro-RO')} caractere în total)`
}

export function formatJson(value: unknown): string {
  return shorten(typeof value === 'string' ? value : JSON.stringify(value))
}

export function formatMs(ms: number | null | undefined): string {
  if (ms === null || ms === undefined) return '–'
  if (ms < 1) return `${ms.toFixed(3)} ms`
  if (ms < 100) return `${ms.toFixed(1)} ms`
  return `${Math.round(ms)} ms`
}

/** {"nums": [3, 2, 4], "target": 6} -> "nums = [3,2,4], target = 6"; strings stay as they are. */
export function formatInput(input: unknown): string {
  if (input === null || typeof input !== 'object' || Array.isArray(input)) return formatJson(input)
  return shorten(Object.entries(input).map(([k, v]) => `${k} = ${formatJson(v)}`).join(', '))
}
