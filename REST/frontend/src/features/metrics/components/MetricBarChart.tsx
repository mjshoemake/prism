import { BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, Tooltip, type ChartOptions } from 'chart.js';
import { Bar } from 'react-chartjs-2';
import type { ChartRow, MetricInfo } from '../../../api/types';
import { palette } from '../../../theme/palette';
import { formatLabel, type ChartBar } from '../filters';
import { axisBounds, formatTick, formatValue, legendLabel } from '../metricFormat';

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend);

interface MetricBarChartProps {
  rows: ChartRow[];
  /** Focus level display name, used in the tooltip. */
  levelName: string;
  metric: MetricInfo;
  /** Called with the clicked bar's key (the filter value) and raw label (null for an empty value). */
  onBarClick: (bar: ChartBar) => void;
}

/** Bar chart of one metric. Everything metric-specific (title, unit, decimals, scale) comes from `metric`. */
export function MetricBarChart({ rows, levelName, metric, onBarClick }: MetricBarChartProps) {
  const bars: ChartBar[] = rows.map((row) => ({ key: row.key, label: row.label === null ? null : String(row.label) }));
  const labels = rows.map((row) => formatLabel(row.label));
  // null = no value (e.g. 0 ÷ 0): no bar, rather than a misleading 0.
  const values = rows.map((row) => (row.value === null ? null : Number(row.value)));

  const data = {
    labels,
    datasets: [
      {
        label: legendLabel(metric),
        data: values,
        backgroundColor: palette.primaryLighter,
        borderColor: palette.primaryDark,
        borderWidth: 1,
        hoverBackgroundColor: palette.primaryLightest,
        hoverBorderColor: palette.primaryLightest,
      },
    ],
  };

  const options: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    onClick: (_event, elements) => {
      if (elements.length > 0) onBarClick(bars[elements[0].index]);
    },
    plugins: {
      legend: { labels: { color: palette.white, font: { size: 12 } } },
      tooltip: {
        callbacks: {
          label: (item) => `${metric.title}: ${formatValue(metric, values[item.dataIndex])}`,
          afterBody: (items) => (items.length ? `Filter on: ${levelName} = ${items[0].label}` : ''),
        },
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        ...axisBounds(metric),
        ticks: { color: palette.textMuted, callback: (value) => formatTick(metric, value) },
        grid: { color: palette.secondary },
      },
      x: {
        ticks: { color: palette.textMuted },
        grid: { color: palette.secondary },
      },
    },
  };

  return <Bar data={data} options={options} />;
}
