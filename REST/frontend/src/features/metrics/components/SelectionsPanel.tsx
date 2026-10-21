import type { FocusArea, FocusLevel, MetricInfo } from '../../../api/types';
import { DropdownSelect } from '../../../components/DropdownSelect';
import { ErrorAlert } from '../../../components/ErrorAlert';
import type { Filter } from '../filters';
import { ActiveFilters } from './ActiveFilters';

interface SelectionsPanelProps {
  metrics: MetricInfo[];
  selectedMetric: MetricInfo | null;
  onSelectMetric: (metric: MetricInfo) => void;
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
  metrics,
  selectedMetric,
  onSelectMetric,
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
          id="metricDropdown"
          label="Metric"
          placeholder="Select Metric"
          items={metrics}
          selected={selectedMetric}
          getKey={(m) => m.metric_pk}
          getLabel={(m) => m.title}
          onSelect={onSelectMetric}
          disabled={loading || metrics.length === 0}
        />
      </div>

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
          getKey={(l) => l.focus_level_pk}
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
