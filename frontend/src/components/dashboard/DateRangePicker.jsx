import { lastNDays, monthRange, yearToDate } from '../../utils/format'

export default function DateRangePicker({ range, onChange }) {
  const presets = [
    ['This month', () => monthRange(0)],
    ['Last month', () => monthRange(-1)],
    ['Last 30 days', () => lastNDays(30)],
    ['This year', yearToDate],
  ]

  return (
    <div className="filters">
      <div className="presets">
        {presets.map(([label, make]) => (
          <button key={label} type="button" className="secondary" onClick={() => onChange(make())}>
            {label}
          </button>
        ))}
      </div>
      <label>
        From
        <input type="date" value={range.from}
               onChange={(e) => onChange({ ...range, from: e.target.value })} />
      </label>
      <label>
        To
        <input type="date" value={range.to}
               onChange={(e) => onChange({ ...range, to: e.target.value })} />
      </label>
    </div>
  )
}