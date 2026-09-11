import { useMemo } from 'react';
import type { Job } from '../types';

interface DagViewProps {
  jobs: Job[];
  selectedJobId?: string;
  onSelect?: (jobId: string) => void;
}

interface LayoutNode {
  job: Job;
  x: number;
  y: number;
  layer: number;
}

const NODE_W = 148;
const NODE_H = 44;
const LAYER_GAP_X = 210;
const ROW_GAP_Y = 64;
const PAD = 32;

/**
 * Lays the dependency graph out left-to-right by "layer" (longest-path
 * from a root), then draws directed edges as smooth cubic curves between
 * nodes. Pure client-side layout — no external graph library needed for
 * a workflow graph of this size.
 */
export function DagView({ jobs, selectedJobId, onSelect }: DagViewProps) {
  const { nodes, edges, width, height } = useMemo(() => buildLayout(jobs), [jobs]);

  if (jobs.length === 0) {
    return (
      <div className="flex h-56 items-center justify-center text-sm text-muted">
        No jobs yet — create one to see the workflow graph.
      </div>
    );
  }

  return (
    <div className="overflow-auto">
      <svg width={width} height={height} className="min-w-full">
        <defs>
          <marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
            <path d="M0,0 L8,4 L0,8 Z" fill="#5B6478" />
          </marker>
        </defs>

        {edges.map((e, i) => {
          const from = nodes.find((n) => n.job.id === e.from)!;
          const to = nodes.find((n) => n.job.id === e.to)!;
          const x1 = from.x + NODE_W;
          const y1 = from.y + NODE_H / 2;
          const x2 = to.x;
          const y2 = to.y + NODE_H / 2;
          const midX = (x1 + x2) / 2;
          return (
            <path
              key={i}
              d={`M ${x1} ${y1} C ${midX} ${y1}, ${midX} ${y2}, ${x2 - 8} ${y2}`}
              stroke="#2A3142"
              strokeWidth={1.5}
              fill="none"
              markerEnd="url(#arrow)"
            />
          );
        })}

        {nodes.map((n) => {
          const isSelected = n.job.id === selectedJobId;
          return (
            <g
              key={n.job.id}
              transform={`translate(${n.x}, ${n.y})`}
              onClick={() => onSelect?.(n.job.id)}
              className="cursor-pointer"
            >
              <rect
                width={NODE_W}
                height={NODE_H}
                rx={6}
                fill={isSelected ? '#1D2330' : '#161B24'}
                stroke={isSelected ? '#E8A33D' : '#2A3142'}
                strokeWidth={isSelected ? 1.5 : 1}
              />
              <circle cx={16} cy={NODE_H / 2} r={4} fill={statusColor(n.job.status)} />
              <text x={30} y={NODE_H / 2 + 4} fontSize={12} fill="#E7E9EE" fontFamily="Inter, sans-serif">
                {truncate(n.job.name, 15)}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}

function statusColor(status: Job['status']) {
  if (status === 'ACTIVE') return '#3DDC97';
  if (status === 'PAUSED') return '#E8A33D';
  return '#5B6478';
}

function truncate(text: string, max: number) {
  return text.length > max ? `${text.slice(0, max - 1)}…` : text;
}

function buildLayout(jobs: Job[]): { nodes: LayoutNode[]; edges: { from: string; to: string }[]; width: number; height: number } {
  const byName = new Map(jobs.map((j) => [j.name, j]));
  const edges: { from: string; to: string }[] = [];
  for (const job of jobs) {
    for (const depName of job.dependsOn) {
      const dep = byName.get(depName);
      if (dep) edges.push({ from: dep.id, to: job.id });
    }
  }

  // longest-path layering (topological layers)
  const layer = new Map<string, number>();
  const incoming = new Map<string, string[]>();
  for (const j of jobs) incoming.set(j.id, []);
  for (const e of edges) incoming.get(e.to)!.push(e.from);

  function resolveLayer(id: string, seen: Set<string>): number {
    if (layer.has(id)) return layer.get(id)!;
    if (seen.has(id)) return 0; // cycle guard, shouldn't happen (backend rejects cycles)
    seen.add(id);
    const preds = incoming.get(id) ?? [];
    const l = preds.length === 0 ? 0 : Math.max(...preds.map((p) => resolveLayer(p, seen))) + 1;
    layer.set(id, l);
    return l;
  }
  for (const j of jobs) resolveLayer(j.id, new Set());

  const layerGroups = new Map<number, Job[]>();
  for (const j of jobs) {
    const l = layer.get(j.id) ?? 0;
    if (!layerGroups.has(l)) layerGroups.set(l, []);
    layerGroups.get(l)!.push(j);
  }

  const nodes: LayoutNode[] = [];
  const maxLayer = Math.max(0, ...Array.from(layerGroups.keys()));
  const maxRows = Math.max(1, ...Array.from(layerGroups.values()).map((g) => g.length));

  for (const [l, group] of layerGroups) {
    group.forEach((job, row) => {
      nodes.push({
        job,
        layer: l,
        x: PAD + l * LAYER_GAP_X,
        y: PAD + row * ROW_GAP_Y
      });
    });
  }

  return {
    nodes,
    edges,
    width: PAD * 2 + (maxLayer + 1) * LAYER_GAP_X,
    height: PAD * 2 + maxRows * ROW_GAP_Y
  };
}
