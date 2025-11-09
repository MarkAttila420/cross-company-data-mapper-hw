import React, { FC, useEffect, useState } from 'react';
import { Mapping } from '../types';
import * as api from '../services/api';

type Props = {
  mappings: Mapping[];
  editable?: boolean;
  onChange?: (mappings: Mapping[]) => void;
};

const MappingDisplay: FC<Props> = ({ mappings, editable = false, onChange }) => {
  const [local, setLocal] = useState<Mapping[]>(mappings || []);
  const [templates, setTemplates] = useState<{ name: string; mappings: Mapping[] }[]>([]);

  useEffect(() => setLocal(mappings || []), [mappings]);

  const update = (id: string, patch: Partial<Mapping>) => {
    const next = local.map((m) => (m.id === id ? { ...m, ...patch } : m));
    setLocal(next);
    onChange?.(next);
  };

  const saveTemplate = async () => {
    const name = window.prompt('Template name to save as');
    if (!name) return;
    try {
      await api.saveTemplate(name, local);
      alert('Template saved');
    } catch (err) {
      console.error(err);
      alert('Failed to save template');
    }
  };

  const loadTemplates = async () => {
    try {
      const t = await api.getTemplates();
      // assume API returns Mapping[] or named templates; adapt if API differs
      if (Array.isArray(t) && (t as any)[0] && (t as any)[0].id) {
        // returned plain mapping list, treat as single template
        setTemplates([{ name: 'Default', mappings: t as Mapping[] }]);
      } else {
        // try to treat as named templates
        setTemplates((t as any) || []);
      }
    } catch (err) {
      console.error(err);
      alert('Failed to load templates');
    }
  };

  const applyTemplate = (m: Mapping[]) => {
    setLocal(m);
    onChange?.(m);
  };

  return (
    <div>
      <h3>Mappings</h3>
      <div style={{ marginBottom: 8 }}>
        <button onClick={loadTemplates} style={{ marginRight: 8 }}>
          Load templates
        </button>
        <button onClick={saveTemplate}>Save template</button>
      </div>

      {templates.length > 0 && (
        <div style={{ marginBottom: 8 }}>
          <strong>Available templates:</strong>
          <ul>
            {templates.map((t) => (
              <li key={t.name}>
                {t.name} <button onClick={() => applyTemplate(t.mappings)}>Apply</button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {local.length === 0 ? (
        <p>No mappings available.</p>
      ) : (
        <table style={{ borderCollapse: 'collapse', width: '100%' }}>
          <thead>
            <tr>
              <th style={{ border: '1px solid #eee', padding: 8 }}>Source</th>
              <th style={{ border: '1px solid #eee', padding: 8 }}>Target</th>
              <th style={{ border: '1px solid #eee', padding: 8 }}>Confidence</th>
              <th style={{ border: '1px solid #eee', padding: 8 }}>Rules</th>
            </tr>
          </thead>
          <tbody>
            {local.map((m) => (
              <tr key={m.id}>
                <td style={{ border: '1px solid #eee', padding: 8 }}>{m.source}</td>
                <td style={{ border: '1px solid #eee', padding: 8 }}>
                  {editable ? (
                    <input value={m.target} onChange={(e) => update(m.id, { target: e.target.value })} />
                  ) : (
                    m.target
                  )}
                </td>
                <td style={{ border: '1px solid #eee', padding: 8 }}>
                  {editable ? (
                    <input
                      type="number"
                      min={0}
                      max={1}
                      step={0.01}
                      value={typeof m.confidence === 'number' ? String(m.confidence) : ''}
                      onChange={(e) => update(m.id, { confidence: Number(e.target.value) })}
                    />
                  ) : (
                    m.confidence != null ? `${Math.round((m.confidence || 0) * 100)}%` : '—'
                  )}
                </td>
                <td style={{ border: '1px solid #eee', padding: 8, whiteSpace: 'pre-wrap' }}>{JSON.stringify(m.rules || {}, null, 2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default MappingDisplay;
