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