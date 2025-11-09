import React, { FC } from 'react';
import { TransformPreview as TP } from '../types';

type Props = {
  preview?: TP | null;
};

function lineDiff(a: string, b: string) {
  const aLines = a.split('\n');
  const bLines = b.split('\n');
  const len = Math.max(aLines.length, bLines.length);
  const rows: Array<{ left: string; right: string; changed: boolean }> = [];
  for (let i = 0; i < len; i++) {
    const left = aLines[i] ?? '';
    const right = bLines[i] ?? '';
    rows.push({ left, right, changed: left !== right });
  }
  return rows;
}

const TransformPreview: FC<Props> = ({ preview }) => {
  if (!preview) return <div>No preview available.</div>;
  const original = preview.original ?? {};
  const transformed = preview.transformed ?? {};
  const left = JSON.stringify(original, null, 2);
  const right = JSON.stringify(transformed, null, 2);
  const rows = lineDiff(left, right);

  return (
    <div>
      <h3>Transform Preview</h3>
      <div style={{ display: 'flex', gap: 12 }}>
        <div style={{ flex: 1 }}>
          <h4>Original</h4>
          <div style={{ background: '#fafafa', padding: 8, maxHeight: 300, overflow: 'auto' }}>
            {rows.map((r, i) => (
              <div key={i} style={{ background: r.changed ? '#fff2f0' : 'transparent', fontFamily: 'monospace' }}>
                {r.left}
              </div>
            ))}
          </div>
        </div>

        <div style={{ flex: 1 }}>
          <h4>Transformed</h4>
          <div style={{ background: '#fafafa', padding: 8, maxHeight: 300, overflow: 'auto' }}>
            {rows.map((r, i) => (
              <div key={i} style={{ background: r.changed ? '#e6ffed' : 'transparent', fontFamily: 'monospace' }}>
                {r.right}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default TransformPreview;
