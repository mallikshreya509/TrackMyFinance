import { formatDate } from '../../utils/format'
import AiStatusBadge from './AiStatusBadge'
import SuggestionCard from './SuggestionCard'

export default function InsightsPanel({ result }) {
  return (
    <div className="panel">
      <div className="ai-head">
        <AiStatusBadge source={result.source} stale={result.stale} />
        <span className="muted">
          {formatDate(result.from)} – {formatDate(result.to)} · generated{' '}
          {new Date(result.generatedAt).toLocaleString()}
        </span>
      </div>

      {result.notice && <p className="notice">{result.notice}</p>}

      <p className="ai-summary">{result.summary}</p>

      {result.insights.length > 0 && (
        <>
          <h2>Insights</h2>
          <div className="ai-grid">
            {result.insights.map((i) => (
              <SuggestionCard key={i.title} title={i.title} detail={i.detail} severity={i.severity} />
            ))}
          </div>
        </>
      )}

      {result.suggestions.length > 0 && (
        <>
          <h2>Suggestions</h2>
          <div className="ai-grid">
            {result.suggestions.map((s) => (
              <SuggestionCard
                key={s.title}
                title={s.title}
                detail={s.detail}
                saving={s.estimatedMonthlySaving}
              />
            ))}
          </div>
        </>
      )}

      <p className="muted small">{result.disclaimer}</p>
    </div>
  )
}