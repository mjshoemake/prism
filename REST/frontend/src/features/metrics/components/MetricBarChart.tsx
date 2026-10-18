import { BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, Tooltip, type ChartOptions } from 'chart.js';
import { Bar } from 'react-chartjs-2';
import type { MetricRow } from '../../../api/types';
import { palette } from '../../../theme/palette';
import type { MetricDefinition } from '../metricDefinitions';

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend);

interface MetricBarChartProps {
  rows: MetricRow[];
  /** Row key holding each bar's label (the focus level column). */
  labelKey: string;
  /** Focus level display name, used in the tooltip. */
  levelName: string;
  metric: MetricDefinition;
  onBarClick: (label: string) => void;
}

export function MetricBarChart({ rows, labelKey, levelName, metric, onBarClick }: MetricBarChartProps) {
  const labels = rows.map((row) => String(row[labelKey] || 'Unknown'));
  const values = rows.map((row) => Number(row[metric.key]) || 0);

  const data = {
    labels,
    datasets: [
      {
        label: metric.legendLabel,
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
      const label = elements.length > 0 ? labels[elements[0].index] : undefined;
      if (label) onBarClick(label);
    },
    plugins: {
      legend: { labels: { color: palette.white, font: { size: 12 } } },
      tooltip: {
        callbacks: {
          afterBody: (items) => (items.length ? `Filter on: ${levelName} = ${items[0].label}` : ''),
        },
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        max: metric.max,
        ticks: { color: palette.textMuted, callback: (value) => metric.formatTick(value) },
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
