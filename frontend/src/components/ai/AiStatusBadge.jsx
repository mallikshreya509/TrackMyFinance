const LABELS = {
  GEMINI: { text: 'AI · Gemini', cls: 'badge-ai' },
  CACHED: { text: 'AI · saved result', cls: 'badge-cached' },
  FALLBACK: { text: 'Basic tips', cls: 'badge-fallback' },
}

export default function AiStatusBadge({ source, stale }) {
  const { text, cls } = LABELS[source] ?? LABELS.FALLBACK
  return (
    <span className={`badge ${cls}`}>
      {text}
      {stale ? ' · out of date' : ''}
    </span>
  )
}