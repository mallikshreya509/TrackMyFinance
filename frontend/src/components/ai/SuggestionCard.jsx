import { formatMoney } from '../../utils/format'

export default function SuggestionCard({ title, detail, severity = 'INFO', saving }) {
  return (
    <div className={`ai-card sev-${severity.toLowerCase()}`}>
      <strong>{title}</strong>
      {detail && <p>{detail}</p>}
      {saving != null && (
        <span className="saving">Could save about {formatMoney(saving)} per month</span>
      )}
    </div>
  )
}