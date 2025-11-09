import React, { ChangeEvent, FC, useState } from 'react';
import { DataFile, Mapping, ValidationResult } from '../types';
import { suggestMappings, transformData, validateData } from '../services/api';
import ValidationResults from './ValidationResults';

const containerStyle: React.CSSProperties = { padding: 16, border: '1px solid #ddd', borderRadius: 6, marginTop: 16 };
const fileBoxStyle: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 12 };
const tableStyle: React.CSSProperties = { width: '100%', borderCollapse: 'collapse', tableLayout: 'fixed' };
const thtdStyle: React.CSSProperties = { border: '1px solid #eee', padding: '8px', wordBreak: 'break-word', verticalAlign: 'top' };

function readFileAsText(file?: File): Promise<string> {
  return new Promise((resolve, reject) => {
    if (!file) return resolve('');
    const r = new FileReader();
    r.onload = () => resolve(String(r.result));
    r.onerror = (e) => reject(e);
    r.readAsText(file);
  });
}

const MappingTool: FC = () => {
  const [sourceFile, setSourceFile] = useState<DataFile | null>(null);
  const [targetFile, setTargetFile] = useState<DataFile | null>(null);
  const [mappings, setMappings] = useState<Mapping[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const [transformed, setTransformed] = useState<DataFile | null>(null);
  const [validationResults, setValidationResults] = useState<ValidationResult[]>([]);

  const handleSourceChange = async (e: ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (!f) return;
    try {
      const text = await readFileAsText(f);
      setSourceFile({ name: f.name, content: text });
    } catch (err) {
      setMessage('Failed to read source file');
    }
  };

  const handleTargetChange = async (e: ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (!f) return;
    try {
      const text = await readFileAsText(f);
      setTargetFile({ name: f.name, content: text });
    } catch (err) {
      setMessage('Failed to read target file');
    }
  };

  const onGenerate = async () => {
    setMessage(null);
    if (!sourceFile || !targetFile) {
      setMessage('Please upload both source and target JSON files first.');
      return;
    }
    setLoading(true);
    try {
      const suggested = await suggestMappings(sourceFile, targetFile);
      setMappings(suggested || []);
      if (!(suggested && suggested.length)) setMessage('No suggestions returned. You can add mappings manually.');
    } catch (err: any) {
      console.error(err);
      setMessage(err?.message || 'Failed to get suggestions');
    } finally {
      setLoading(false);
    }
  };

  const updateMapping = (id: string, patch: Partial<Mapping>) => {
    setMappings((prev) => prev.map((m) => (m.id === id ? { ...m, ...patch } : m)));
  };

  const addMapping = () => {
    const id = Math.random().toString(36).slice(2, 9);
    setMappings((prev) => [...prev, { id, source: '', target: '', rules: {} }]);
  };

  const removeMapping = (id: string) => {
    setMappings((prev) => prev.filter((m) => m.id !== id));
  };

  const onTransformAndValidate = async () => {
    setMessage(null);
    if (!sourceFile) {
      setMessage('Upload source file first');
      return;
    }
    setLoading(true);
    try {
      // transform
      const transformedFile = await transformData(sourceFile, mappings);
      setTransformed(transformedFile);
      // validate
      const results = await validateData(transformedFile);
      setValidationResults(results || []);
    } catch (err: any) {
      console.error(err);
      setMessage(err?.message || 'Transform/Validate failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={containerStyle}>
      <h2>Mapping Tool</h2>

      <div style={fileBoxStyle}>
        <div>
          <label>Source JSON:</label>
          <input type="file" accept="application/json,.json,.txt" onChange={handleSourceChange} />
          {sourceFile && <div style={{ fontSize: 12, color: '#666' }}>Loaded: {sourceFile.name}</div>}
        </div>

        <div>
          <label>Target JSON:</label>
          <input type="file" accept="application/json,.json,.txt" onChange={handleTargetChange} />
          {targetFile && <div style={{ fontSize: 12, color: '#666' }}>Loaded: {targetFile.name}</div>}
        </div>

        <div>
          <button onClick={onGenerate} disabled={loading} style={{ marginRight: 8 }}>
            {loading ? 'Working...' : 'Generate mappings'}
          </button>
          <button onClick={addMapping} style={{ marginRight: 8 }}>
            Add mapping
          </button>
          <button onClick={onTransformAndValidate} disabled={loading}>
            Transform & Validate
          </button>
        </div>
      </div>

      {message && <div style={{ color: 'darkred', marginBottom: 8 }}>{message}</div>}

      <div>
        <h3>Mappings</h3>
        {mappings.length === 0 ? (
          <p>No mappings yet.</p>
        ) : (
          <table style={tableStyle}>
            <thead>
              <tr>
                <th style={thtdStyle}>Source</th>
                <th style={thtdStyle}>Target (editable)</th>
                <th style={thtdStyle}>Rules (JSON)</th>
                <th style={thtdStyle}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {mappings.map((m) => (
                <tr key={m.id}>
                  <td style={thtdStyle}>{m.source}</td>
                  <td style={thtdStyle}>
                    <input
                      value={m.target}
                      onChange={(e) => updateMapping(m.id, { target: e.target.value })}
                      style={{ width: '100%' }}
                    />
                  </td>
                  <td style={thtdStyle}>
                    <textarea
                      value={JSON.stringify(m.rules || {}, null, 2)}
                      onChange={(e) => {
                        try {
                          const parsed = JSON.parse(e.target.value || '{}');
                          updateMapping(m.id, { rules: parsed });
                        } catch (err) {
                          // ignore parse errors while editing
                        }
                      }}
                      rows={3}
                      // allow only vertical resize and ensure textarea doesn't change table width
                      style={{ width: '100%', boxSizing: 'border-box', resize: 'vertical', maxWidth: '100%' }}
                    />
                  </td>
                  <td style={thtdStyle}>
                    <button onClick={() => removeMapping(m.id)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div style={{ marginTop: 16 }}>
        <h3>Transformation result</h3>
        {transformed ? (
          <pre style={{ maxHeight: 240, overflow: 'auto', background: '#fafafa', padding: 8 }}>
            {transformed.content}
          </pre>
        ) : (
          <p>No transformed data yet.</p>
        )}
      </div>

      <div style={{ marginTop: 16 }}>
        <ValidationResults results={validationResults} />
      </div>
    </div>
  );
};

export default MappingTool;
