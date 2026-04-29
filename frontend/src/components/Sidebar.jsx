const SORT_OPTIONS = [
  { value: 'relevant', label: 'Most relevant' },
  { value: 'newest', label: 'Newest' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_dec', label: 'Price: high to low' },
];

export default function Sidebar({ filters, setFilters, categories }) {
  const update = (patch) => setFilters({ ...filters, ...patch });

  const toggleCategory = (cat) => {
    update({ category: filters.category === cat ? '' : cat });
  };

  return (
    <aside className="w-60 shrink-0 border-r border-gray-200 bg-white p-4 text-sm">
      <h2 className="mb-3 text-xs font-bold uppercase tracking-wide text-gray-500">Filters</h2>

      <section className="mb-5">
        <h3 className="mb-2 font-semibold">Category</h3>
        <ul className="space-y-1">
          <li>
            <label className="flex items-center gap-2">
              <input
                type="radio"
                checked={!filters.category}
                onChange={() => update({ category: '' })}
                className="accent-usc-cardinal"
              />
              <span>All</span>
            </label>
          </li>
          {categories.map((cat) => (
            <li key={cat}>
              <label className="flex items-center gap-2">
                <input
                  type="radio"
                  checked={filters.category === cat}
                  onChange={() => toggleCategory(cat)}
                  className="accent-usc-cardinal"
                />
                <span>{cat.replaceAll('_', ' ').toLowerCase()}</span>
              </label>
            </li>
          ))}
        </ul>
      </section>

      <section className="mb-5">
        <h3 className="mb-2 font-semibold">Price Range</h3>
        <div className="flex items-center gap-2">
          <input
            type="number"
            min="0"
            placeholder="Min"
            value={filters.minPrice ?? ''}
            onChange={(e) => update({ minPrice: e.target.value })}
            className="w-full rounded border border-gray-300 px-2 py-1"
          />
          <span className="text-gray-400">–</span>
          <input
            type="number"
            min="0"
            placeholder="Max"
            value={filters.maxPrice ?? ''}
            onChange={(e) => update({ maxPrice: e.target.value })}
            className="w-full rounded border border-gray-300 px-2 py-1"
          />
        </div>
      </section>

      <section>
        <h3 className="mb-2 font-semibold">Sort By</h3>
        <select
          value={filters.sortBy || 'relevant'}
          onChange={(e) => update({ sortBy: e.target.value })}
          className="w-full rounded border border-gray-300 px-2 py-1"
        >
          {SORT_OPTIONS.map((s) => (
            <option key={s.value} value={s.value}>{s.label}</option>
          ))}
        </select>
      </section>
    </aside>
  );
}
