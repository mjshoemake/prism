import { formatLabel, type Filter } from '../filters';

interface ActiveFiltersProps {
  filters: Filter[];
  onRemove: (index: number) => void;
  onClear: () => void;
}

export function ActiveFilters({ filters, onRemove, onClear }: ActiveFiltersProps) {
  return (
    <>
      <h2 className="section-title">Active Filters</h2>

      {filters.length === 0 ? (
        <p className="text-muted">No filters applied. Click a bar to add a filter.</p>
      ) : (
        <>
          <ul className="list-group mb-2">
            {filters.map((f, i) => (
              <li
                key={`${f.focus_level_pk}=${f.value ?? ''}`}
                className="list-group-item d-flex justify-content-between align-items-center"
              >
                <span>
                  <strong>{f.name}:</strong> {formatLabel(f.value)}
                </span>
                <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => onRemove(i)}>
                  Remove
                </button>
              </li>
            ))}
          </ul>
          <button type="button" className="btn btn-sm btn-outline-secondary" onClick={onClear}>
            Clear all
          </button>
        </>
      )}
    </>
  );
}
