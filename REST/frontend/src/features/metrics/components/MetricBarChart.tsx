import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip,
  type ChartOptions,
  type ChartType,
  type Plugin,
} from 'chart.js';
import { Bar } from 'react-chartjs-2';
import type { ChartRow, MetricInfo } from '../../../api/types';
import { palette } from '../../../theme/palette';
import { formatLabel, type ChartBar } from '../filters';
import { clipToScale, formatTick, formatValue, legendLabel, type BarFit, type ChartScale } from '../metricFormat';

/** Options of the barber-pole plugin: how each bar fits the axis (index = bar). */
interface BarberPoleOptions {
  fits?: readonly BarFit[];
}

declare module 'chart.js' {
  interface PluginOptionsByType<TType extends ChartType> {
    /** Striped ends on bars cut at a hard axis bound (see barberPole below). */
    barberPole?: BarberPoleOptions;
  }
}

/** Height of the striped end of a bar cut at the axis maximum, in pixels. */
const STRIPE_BAND = 14;
const STRIPE_GAP = 6;

/**
 * Draws diagonal stripes on bars that don't show their true height (docs/OVERVIEW.md §4.3): the top end of a bar
 * above the axis maximum, and the whole stub of a bar below a raised minimum, so neither can be mistaken for a bar
 * of true height. The real value is in the tooltip.
 */
const barberPole: Plugin<'bar', BarberPoleOptions> = {
  id: 'barberPole',
  afterDatasetsDraw(chart, _args, options) {
    const fits = options.fits;
    if (!fits || fits.every((f) => f === 'within')) return;
    const { ctx, chartArea } = chart;
    chart.getDatasetMeta(0).data.forEach((element, i) => {
      const fit = fits[i];
      if (!fit || fit === 'within') return;
      const { x, y, base, width } = element.getProps(['x', 'y', 'base', 'width'], true) as {
        x: number;
        y: number;
        base: number;
        width: number;
      };
      const left = x - width / 2;
      // Stay inside the plot area, whatever the bar's geometry.
      const top = Math.max(Math.min(y, base), chartArea.top);
      const bottom = Math.min(Math.max(y, base), chartArea.bottom);
      const height = bottom - top;
      const band = fit === 'above' ? Math.min(STRIPE_BAND, height) : height;
      if (band <= 0) return;

      ctx.save();
      ctx.beginPath();
      ctx.rect(left, top, width, band);
      ctx.clip();
      ctx.fillStyle = palette.primaryDarker;
      ctx.fillRect(left, top, width, band);
      ctx.strokeStyle = palette.white;
      ctx.lineWidth = 2;
      ctx.beginPath();
      for (let offset = -band; offset < width + band; offset += STRIPE_GAP) {
        ctx.moveTo(left + offset, top + band);
        ctx.lineTo(left + offset + band, top);
      }
      ctx.stroke();
      ctx.restore();
    });
  },
};

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend, barberPole);

interface MetricBarChartProps {
  rows: ChartRow[];
  /** Focus level display name, used in the tooltip. */
  levelName: string;
  metric: MetricInfo;
  /** The y-axis (metricFormat.effectiveScale). */
  scale: ChartScale;
  /** Called with the clicked bar's key (the filter value) and raw label (null for an empty value). */
  onBarClick: (bar: ChartBar) => void;
}

/**
 * Bar chart of one metric. Everything metric-specific (title, unit, decimals) comes from `metric`, the axis from
 * `scale`. On a hard scale, bars beyond the bounds are drawn to the edge with a striped end; scaling never changes a
 * value, and tooltips always show the real one.
 */
export function MetricBarChart({ rows, levelName, metric, scale, onBarClick }: MetricBarChartProps) {
  const bars: ChartBar[] = rows.map((row) => ({ key: row.key, label: row.label === null ? null : String(row.label) }));
  const labels = rows.map((row) => formatLabel(row.label));
  // null = no value (e.g. 0 ÷ 0): no bar, rather than a misleading 0.
  const values = rows.map((row) => (row.value === null ? null : Number(row.value)));
  const top = Math.max(...values.filter((v): v is number => v !== null), Number.NEGATIVE_INFINITY);
  const clipped = values.map((v) => clipToScale(v, scale, Number.isFinite(top) ? top : undefined));
  const fits = clipped.map((c) => c.fit);
  const raisedMin = scale.hard && scale.min !== undefined && scale.min > 0 ? scale.min : null;

  const data = {
    labels,
    datasets: [
      {
        label: legendLabel(metric),
        data: clipped.map((c) => c.drawn),
        backgroundColor: palette.primaryLighter,
        borderColor: palette.primaryDark,
        borderWidth: 1,
        hoverBackgroundColor: palette.primaryLightest,
        hoverBorderColor: palette.primaryLightest,
        // On a raised minimum, bars grow from the axis bottom (not from 0, which is off the chart).
        base: raisedMin ?? undefined,
      },
    ],
  };

  const options: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    // Hover and click by column, so bars cut short (or with no height at all) are as easy to reach as any other.
    interaction: { mode: 'index', intersect: false },
    onClick: (_event, elements) => {
      if (elements.length > 0) onBarClick(bars[elements[0].index]);
    },
    plugins: {
      legend: { labels: { color: palette.white, font: { size: 12 } } },
      barberPole: { fits },
      tooltip: {
        callbacks: {
          label: (item) => `${metric.title}: ${formatValue(metric, values[item.dataIndex])}`,
          afterLabel: (item) => {
            const fit = fits[item.dataIndex];
            if (fit === 'above' && scale.max !== undefined) return `Above the axis maximum (${formatTick(metric, scale.max)})`;
            if (fit === 'below' && scale.min !== undefined) return `Below the axis minimum (${formatTick(metric, scale.min)})`;
            return '';
          },
          afterBody: (items) => (items.length ? `Filter on: ${levelName} = ${items[0].label}` : ''),
        },
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        min: scale.min,
        max: scale.max,
        suggestedMin: scale.suggestedMin,
        suggestedMax: scale.suggestedMax,
        ticks: { color: palette.textMuted, callback: (value) => formatTick(metric, value) },
        grid: { color: palette.secondary },
      },
      x: {
        ticks: { color: palette.textMuted },
        grid: { color: palette.secondary },
      },
    },
  };

  return (
    <div className="metric-chart">
      {raisedMin !== null && (
        <div className="metric-chart__axis-note">
          <span className="badge text-bg-warning">Axis starts at {formatTick(metric, raisedMin)}</span>
        </div>
      )}
      <div className="metric-chart__canvas">
        <Bar data={data} options={options} />
      </div>
    </div>
  );
}
