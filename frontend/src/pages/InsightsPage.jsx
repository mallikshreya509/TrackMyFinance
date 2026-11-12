import { useEffect, useState } from 'react'
import { getErrorMessage } from '../api/errors'
import InsightsPanel from '../components/ai/InsightsPanel'
import DateRangePicker from '../components/dashboard/DateRangePicker'
import { useGenerateSuggestions, useLatestSuggestions } from '../hooks/useAiSuggestions'
import { monthRange } from '../utils/format'

export default function InsightsPage() {
  const [range, setRange] = useState(() => monthRange(0))
  const [cooldown, setCooldown] = useState(0) // seconds until "Regenerate" is allowed again
  const latest = useLatestSuggestions()
  const generate = useGenerateSuggestions()

  // Count the cooldown down once a second
  useEffect(() => {
    if (cooldown <= 0) return
    const id = setTimeout(() => setCooldown((s) => s - 1), 1000)
    return () => clearTimeout(id)
  }, [cooldown])

  const invalidRange = !range.from || !range.to || range.from > range.to
  // Show the newest answer from this page, otherwise the last one stored on the server
  const result = generate.data ?? latest.data ?? null

  function run(refresh) {
    generate.mutate(
      { ...range, refresh },
      { onSuccess: (data) => setCooldown(data.retryAfterSeconds ?? 0) },
    )
  }

  return (
    <>
      <div className="page-head">
        <h1>AI insights</h1>
      </div>

      <DateRangePicker range={range} onChange={setRange} />

      {invalidRange && (
        <p className="error">Choose a valid date range (From must not be after To).</p>
      )}

      <div className="actions-row">
        <button onClick={() => run(false)} disabled={invalidRange || generate.isPending}>
          Get AI suggestions
        </button>
        <button
          className="secondary"
          onClick={() => run(true)}
          disabled={invalidRange || generate.isPending || cooldown > 0}
        >
          {cooldown > 0 ? `Regenerate (${cooldown}s)` : 'Regenerate'}
        </button>
        {generate.isPending && (
          <span className="muted">Analysing your spending… this can take up to a minute.</span>
        )}
      </div>

      {generate.isError && <p className="error">{getErrorMessage(generate.error)}</p>}

      {result ? (
        <InsightsPanel result={result} />
      ) : (
        !latest.isPending && (
          <p className="muted">
            No suggestions yet. Pick a date range and click “Get AI suggestions”.
          </p>
        )
      )}
    </>
  )
}