export const CURRENCY = 'INR'

const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: CURRENCY })
export const formatMoney = (value) => money.format(value)

// "2026-09-28" -> "28 Sep 2026". Built from parts so no timezone shift can happen.
export function formatDate(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('en-GB', {
    day: 'numeric', month: 'short', year: 'numeric',
  })
}

// Today in the user's LOCAL timezone as YYYY-MM-DD
export function todayLocal() {
  const n = new Date()
  const mm = String(n.getMonth() + 1).padStart(2, '0')
  const dd = String(n.getDate()).padStart(2, '0')
  return `${n.getFullYear()}-${mm}-${dd}`
}
// Date -> "YYYY-MM-DD" in the user's LOCAL timezone (toISOString would give UTC)
export function toISODate(d) {
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd}`
}

// "2026-09-05" -> "5 Sep"
export function formatDayMonth(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })
}

// offset 0 = this month, -1 = last month
export function monthRange(offset = 0) {
  const n = new Date()
  const start = new Date(n.getFullYear(), n.getMonth() + offset, 1)
  const end = new Date(n.getFullYear(), n.getMonth() + offset + 1, 0)
  return { from: toISODate(start), to: toISODate(end) }
}

export function lastNDays(n) {
  const end = new Date()
  const start = new Date(end.getFullYear(), end.getMonth(), end.getDate() - (n - 1))
  return { from: toISODate(start), to: toISODate(end) }
}

export function yearToDate() {
  const n = new Date()
  return { from: toISODate(new Date(n.getFullYear(), 0, 1)), to: toISODate(n) }
}

// Every day from..to, with 0 where nothing was spent. Uses Date(y, m, d + i) so DST can't skip a day.
export function fillDays(from, to, dailyTotals) {
  const byDate = new Map(dailyTotals.map((t) => [t.date, Number(t.total)]))
  const [y, m, d] = from.split('-').map(Number)
  const out = []
  for (let i = 0; ; i++) {
    const iso = toISODate(new Date(y, m - 1, d + i))
    out.push({ date: iso, total: byDate.get(iso) ?? 0 })
    if (iso >= to) break
  }
  return out
}