import type { FocusArea, FocusLevel } from '../../../api/types';
import { DropdownSelect } from '../../../components/DropdownSelect';
import { ErrorAlert } from '../../../components/ErrorAlert';
import type { Filter } from '../filters';
import { ActiveFilters } from './ActiveFilters';

interface SelectionsPanelProps {
  focusAreas: FocusArea[];
  focusLevels: FocusLevel[];
  selectedArea: FocusArea | null;
  selectedLevel: FocusLevel | null;
  filters: Filter[];
  loading: boolean;
  error: string | null;
  onSelectArea: (area: FocusArea) => void;
  onSelectLevel: (level: FocusLevel) => void;
  onRemoveFilter: (index: number) => void;
  onClearFilters: () => void;
}

export function SelectionsPanel({
  focusAreas,
  focusLevels,
  selectedArea,
  selectedLevel,
  filters,
  loading,
  error,
  onSelectArea,
  onSelectLevel,
  onRemoveFilter,
  onClearFilters,
}: SelectionsPanelProps) {
  return (
    <aside className="metrics-sidebar">
      <h2 className="section-title">Selections</h2>

      <div className="mb-3">
        <DropdownSelect
          id="focusAreaDropdown"
          label="Focus Area"
          placeholder="Select Focus Area"
          items={focusAreas}
          selected={selectedArea}
          getKey={(a) => a.focus_area_pk}
          getLabel={(a) => a.name}
          onSelect={onSelectArea}
          disabled={loading}
        />
      </div>

      <div className="mb-2">
        <DropdownSelect
          id="focusLevelDropdown"
          label="Focus Level"
          placeholder="Select Focus Level"
          items={focusLevels}
          selected={selectedLevel}
          getKey={(l) => l.column_name}
          getLabel={(l) => l.name}
          onSelect={onSelectLevel}
          disabled={loading || !selectedLevel}
        />
      </div>

      <div className="spacer" />

      <ActiveFilters filters={filters} onRemove={onRemoveFilter} onClear={onClearFilters} />

      {error && <ErrorAlert message={error} className="mt-3" />}
    </aside>
  );
}
