import type { Dashboard, FocusArea, FocusLevel } from '../../../api/types';
import { DropdownSelect } from '../../../components/DropdownSelect';
import { ErrorAlert } from '../../../components/ErrorAlert';
import { ActiveFilters } from '../../metrics/components/ActiveFilters';
import type { Filter } from '../../metrics/filters';

interface AnalysisSidebarProps {
  dashboards: Dashboard[];
  selected: Dashboard | null;
  onSelect: (dashboard: Dashboard) => void;
  onEdit: () => void;
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

/** The dashboard picker with its Edit button, then the focus area, level and filters every chart shares. */
export function AnalysisSidebar(props: AnalysisSidebarProps) {
  const { dashboards, selected, loading } = props;

  return (
    <aside className="metrics-sidebar">
      <h2 className="section-title">Dashboard</h2>
      <div className="d-flex gap-2 align-items-start mb-3">
        <div className="flex-grow-1 min-w-0">
          <DropdownSelect
            id="analysisDashboard"
            ariaLabel="Dashboard"
            placeholder="Select Dashboard"
            items={dashboards}
            selected={selected}
            getKey={(d) => d.dashboard_pk}
            getLabel={(d) => d.name}
            onSelect={props.onSelect}
            disabled={dashboards.length === 0}
          />
        </div>
        <button
          type="button"
          className="btn btn-sm btn-outline-secondary"
          onClick={props.onEdit}
          disabled={!selected}
          title="Change this dashboard's name, layout, metrics and scales"
        >
          <i className="fas fa-pen me-1" aria-hidden="true" />
          Edit
        </button>
      </div>

      <h2 className="section-title">Selections (all charts)</h2>
      <div className="mb-3">
        <DropdownSelect
          id="analysisFocusArea"
          label="Focus Area"
          placeholder="Select Focus Area"
          items={props.focusAreas}
          selected={props.selectedArea}
          getKey={(a) => a.focus_area_pk}
          getLabel={(a) => a.name}
          onSelect={props.onSelectArea}
          disabled={loading}
        />
      </div>
      <div className="mb-2">
        <DropdownSelect
          id="analysisFocusLevel"
          label="Focus Level"
          placeholder="Select Focus Level"
          items={props.focusLevels}
          selected={props.selectedLevel}
          getKey={(l) => l.focus_level_pk}
          getLabel={(l) => l.name}
          onSelect={props.onSelectLevel}
          disabled={loading || !props.selectedLevel}
        />
      </div>
      <div className="spacer" />
      <ActiveFilters filters={props.filters} onRemove={props.onRemoveFilter} onClear={props.onClearFilters} />

      {props.error && <ErrorAlert message={props.error} className="mt-3" />}
    </aside>
  );
}
