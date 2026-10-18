import type { Key } from 'react';

interface DropdownSelectProps<T> {
  id: string;
  label: string;
  placeholder: string;
  items: readonly T[];
  selected: T | null;
  getKey: (item: T) => Key;
  getLabel: (item: T) => string;
  onSelect: (item: T) => void;
  disabled?: boolean;
}

/**
 * A Bootstrap dropdown styled like a select (see .dropdown > .form-select-sm in the theme).
 * Opening/closing is handled by Bootstrap's JS via data-bs-toggle.
 */
export function DropdownSelect<T>({
  id,
  label,
  placeholder,
  items,
  selected,
  getKey,
  getLabel,
  onSelect,
  disabled = false,
}: DropdownSelectProps<T>) {
  const selectedKey = selected === null ? null : getKey(selected);

  return (
    <>
      <label className="form-label" htmlFor={id}>
        {label}
      </label>
      <div className="dropdown">
        <button
          id={id}
          type="button"
          className="btn btn-outline-secondary form-select-sm w-100 text-start d-flex justify-content-between align-items-center"
          data-bs-toggle="dropdown"
          aria-expanded="false"
          disabled={disabled}
        >
          <span>{selected ? getLabel(selected) : placeholder}</span>
          <i className="fas fa-chevron-down ms-auto" />
        </button>
        <ul className="dropdown-menu w-100" aria-labelledby={id}>
          {items.map((item) => {
            const key = getKey(item);
            return (
              <li key={key}>
                <button
                  type="button"
                  className={`dropdown-item ${key === selectedKey ? 'active' : ''}`}
                  onClick={() => onSelect(item)}
                >
                  {getLabel(item)}
                </button>
              </li>
            );
          })}
        </ul>
      </div>
    </>
  );
}
