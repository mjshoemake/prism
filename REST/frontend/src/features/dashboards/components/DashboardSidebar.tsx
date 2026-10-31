import { useState } from 'react';
import type { Dashboard, DashboardLayout, FocusArea, FocusLevel } from '../../../api/types';
import { DropdownSelect } from '../../../components/DropdownSelect';
import { ErrorAlert } from '../../../components/ErrorAlert';
import { ActiveFilters } from '../../metrics/components/ActiveFilters';
import type { Filter } from '../../metrics/filters';
import { LAYOUTS } from '../layouts';

interface DashboardSidebarProps {
  dashboards: Dashboard[];
  selected: Dashboard | null;
  onSelect: (dashboardPk: number) => void;
  onCreate: (name: string) => Promise<boolean>;
  onRename: (name: string) => Promise<boolean>;
  onDelete: () => void;
  onSetLayout: (layout: DashboardLayout) => void;
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

/** Dashboard picker, new / rename / delete, the layout switch, and the selections every chart shares. */
export function DashboardSidebar(props: DashboardSidebarProps) {
  const { dashboards, selected, loading } = props;
  const [newName, setNewName] = useState('');
  const [renaming, setRenaming] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const create = async () => {
    if (newName.trim() && (await props.onCreate(newName.trim()))) setNewName('');
  };
  const rename = async () => {
    if (renaming !== null && renaming.trim() && (await props.onRename(renaming))) setRenaming(null);
  };

  return (
    <aside className="metrics-sidebar">
      <h2 className="section-title">Dashboard</h2>

      {dashboards.length > 0 && (
        <div className="mb-2">
          <DropdownSelect
            id="dashboardDropdown"
            label="Open"
            placeholder="Select Dashboard"
            items={dashboards}
            selected={selected}
            getKey={(d) => d.dashboard_pk}
            getLabel={(d) => d.name}
            onSelect={(d) => {
              setRenaming(null);
              setConfirmDelete(false);
              props.onSelect(d.dashboard_pk);
            }}
            disabled={loading}
          />
        </div>
      )}

      {selected && (
        <div className="mb-3">
          {renaming === null ? (
            <div className="d-flex gap-2">
              <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => setRenaming(selected.name)}>
                Rename
              </button>
              {confirmDelete ? (
                <>
                  <button type="button" className="btn btn-sm btn-danger" onClick={() => { setConfirmDelete(false); props.onDelete(); }}>
                    Delete "{selected.name}"
                  </button>
                  <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => setConfirmDelete(false)}>
                    Keep
                  </button>
                </>
              ) : (
                <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => setConfirmDelete(true)}>
                  Delete
                </button>
              )}
            </div>
          ) : (
            <div className="input-group input-group-sm">
              <input
                className="form-control"
                aria-label="Dashboard name"
                value={renaming}
                maxLength={60}
                onChange={(e) => setRenaming(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') void rename(); }}
              />
              <button type="button" className="btn btn-outline-secondary" onClick={() => void rename()} disabled={!renaming.trim()}>
                Save
              </button>
              <button type="button" className="btn btn-outline-secondary" onClick={() => setRenaming(null)}>
                Cancel
              </button>
            </div>
          )}
        </div>
      )}

      <label className="form-label" htmlFor="newDashboardName">New dashboard</label>
      <div className="input-group input-group-sm mb-3">
        <input
          id="newDashboardName"
          className="form-control"
          placeholder="Name"
          value={newName}
          maxLength={60}
          onChange={(e) => setNewName(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') void create(); }}
        />
        <button type="button" className="btn btn-outline-secondary" onClick={() => void create()} disabled={!newName.trim()}>
          Create
        </button>
      </div>

      {selected && (
        <>
          <label className="form-label">Layout</label>
          <div className="btn-group btn-group-sm w-100 mb-3" role="group" aria-label="Layout">
            {LAYOUTS.map(({ layout, label, icon }) => (
              <button
                key={layout}
                type="button"
                className={`btn ${selected.layout === layout ? 'btn-primary' : 'btn-outline-secondary'}`}
                aria-pressed={selected.layout === layout}
                onClick={() => props.onSetLayout(layout)}
              >
                <i className={`fas ${icon} me-1`} />
                {label}
              </button>
            ))}
          </div>

          <h2 className="section-title">Selections (all charts)</h2>
          <div className="mb-3">
            <DropdownSelect
              id="dashboardFocusArea"
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
              id="dashboardFocusLevel"
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
        </>
      )}

      {props.error && <ErrorAlert message={props.error} className="mt-3" />}
    </aside>
  );
}
