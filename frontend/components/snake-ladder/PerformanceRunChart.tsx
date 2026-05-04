"use client";

type RunPoint = {
  round: number;
  [key: string]: string | number;
};

type Props = {
  title: string;
  data: RunPoint[];
  series: {
    key: string;
    label: string;
    colorClass: string;
    stroke: string;
  }[];
  yLabel?: string;
};

function getMaxValue(data: RunPoint[], keys: string[]) {
  let max = 0;

  for (const row of data) {
    for (const key of keys) {
      const value = Number(row[key] ?? 0);
      if (value > max) max = value;
    }
  }

  return max === 0 ? 1 : max;
}

export default function PerformanceRunChart({
  title,
  data,
  series,
  yLabel = "Time (ms)",
}: Props) {
  const width = 900;
  const height = 320;
  const padLeft = 52;
  const padRight = 18;
  const padTop = 24;
  const padBottom = 42;

  const innerWidth = width - padLeft - padRight;
  const innerHeight = height - padTop - padBottom;

  const keys = series.map((s) => s.key);
  const maxValue = getMaxValue(data, keys);

  const yTicks = 5;
  const xCount = Math.max(data.length, 1);

  const getX = (index: number) => {
    if (xCount === 1) return padLeft + innerWidth / 2;
    return padLeft + (index * innerWidth) / (xCount - 1);
  };

  const getY = (value: number) => {
    return padTop + innerHeight - (value / maxValue) * innerHeight;
  };

  const getPath = (key: string) => {
    return data
      .map((row, index) => {
        const x = getX(index);
        const y = getY(Number(row[key] ?? 0));
        return `${index === 0 ? "M" : "L"} ${x} ${y}`;
      })
      .join(" ");
  };

  return (
    <div className="rounded-2xl border border-white/10 bg-[#050505] p-5 shadow-2xl">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-[0.3em] text-cyan-400">
            {title}
          </p>
          <p className="mt-1 text-[11px] text-white/45">
            {yLabel} across {data.length} individual rounds
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {series.map((item) => (
            <div key={item.key} className="flex items-center gap-2 text-[10px] text-white/70">
              <span className={`inline-block h-2.5 w-2.5 rounded-full ${item.colorClass}`} />
              <span className="font-mono uppercase tracking-wider">{item.label}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="overflow-x-auto">
        <svg viewBox={`0 0 ${width} ${height}`} className="min-w-[820px] w-full">
          <rect x="0" y="0" width={width} height={height} fill="transparent" />

          {Array.from({ length: yTicks + 1 }).map((_, i) => {
            const value = (maxValue / yTicks) * i;
            const y = getY(value);

            return (
              <g key={i}>
                <line
                  x1={padLeft}
                  y1={y}
                  x2={width - padRight}
                  y2={y}
                  stroke="rgba(255,255,255,0.08)"
                  strokeDasharray="4 4"
                />
                <text
                  x={padLeft - 8}
                  y={y + 4}
                  textAnchor="end"
                  fontSize="10"
                  fill="rgba(255,255,255,0.45)"
                >
                  {value.toFixed(1)}
                </text>
              </g>
            );
          })}

          {data.map((row, index) => {
            const x = getX(index);

            return (
              <g key={row.round}>
                <line
                  x1={x}
                  y1={padTop}
                  x2={x}
                  y2={height - padBottom}
                  stroke="rgba(255,255,255,0.04)"
                />
                <text
                  x={x}
                  y={height - 14}
                  textAnchor="middle"
                  fontSize="10"
                  fill="rgba(255,255,255,0.5)"
                >
                  {index + 1}
                </text>
              </g>
            );
          })}

          <line
            x1={padLeft}
            y1={height - padBottom}
            x2={width - padRight}
            y2={height - padBottom}
            stroke="rgba(255,255,255,0.15)"
          />
          <line
            x1={padLeft}
            y1={padTop}
            x2={padLeft}
            y2={height - padBottom}
            stroke="rgba(255,255,255,0.15)"
          />

          {series.map((item) =>
            data.length > 0 ? (
              <path
                key={item.key}
                d={getPath(item.key)}
                fill="none"
                stroke={item.stroke}
                strokeWidth="2.5"
                strokeLinejoin="round"
                strokeLinecap="round"
              />
            ) : null
          )}

          {series.map((item) =>
            data.map((row) => {
              const index = data.findIndex((d) => d.round === row.round);
              const value = Number(row[item.key] ?? 0);
              const x = getX(index);
              const y = getY(value);

              return (
                <g key={`${item.key}-${row.round}`}>
                  <circle cx={x} cy={y} r="3.5" fill={item.stroke} />
                  <title>{`${item.label} - Round ${row.round}: ${value.toFixed(3)} ms`}</title>
                </g>
              );
            })
          )}

          <text
            x={16}
            y={height / 2}
            transform={`rotate(-90 16 ${height / 2})`}
            textAnchor="middle"
            fontSize="11"
            fill="rgba(255,255,255,0.55)"
          >
            {yLabel}
          </text>

          <text
            x={width / 2}
            y={height - 2}
            textAnchor="middle"
            fontSize="11"
            fill="rgba(255,255,255,0.55)"
          >
            Round Number
          </text>
        </svg>
      </div>
    </div>
  );
}
