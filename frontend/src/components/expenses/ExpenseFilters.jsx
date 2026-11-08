export default function ExpenseFilters({ filters, categories, onChange, onClear }) {
  return (
    <div className="filters">
      <label>
        From
        <input type="date" value={filters.from}
               onChange={(e) => onChange({ from: e.target.value })} />
      </label>
      <label>
        To
        <input type="date" value={filters.to}
               onChange={(e) => onChange({ to: e.target.value })} />
      </label>
      <label>
        Category
        <select value={filters.categoryId}
                onChange={(e) => onChange({ categoryId: e.target.value })}>
          <option value="">All</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
      </label>
      <button type="button" className="secondary" onClick={onClear}>Clear</button>
    </div>
  )
}