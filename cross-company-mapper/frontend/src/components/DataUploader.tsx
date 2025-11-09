import React, { ChangeEvent, FC, useState } from 'react';
import { DataFile } from '../types';

type Props = {
  onUpload: (file: DataFile) => void;
};

const DataUploader: FC<Props> = ({ onUpload }) => {
  const [parsed, setParsed] = useState<any | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const text = String(reader.result);
      // try parse JSON
      try {
        const json = JSON.parse(text);
        setParsed(json);
        setError(null);
      } catch (err) {
        // not valid JSON, keep raw text
        setParsed(null);
        setError('Uploaded file is not valid JSON');
      }
      onUpload({ name: file.name, content: text });
    };
    reader.readAsText(file);
  };

  return (
    <div>
      <label htmlFor="data-upload">Upload data file</label>
      <input id="data-upload" type="file" accept=".json,.txt,application/json" onChange={handleFileChange} />
      {error && <div style={{ color: 'darkred' }}>{error}</div>}
      {parsed && (
        <div style={{ marginTop: 8, fontSize: 13, color: '#333' }}>
          <strong>Top-level keys:</strong>
          <ul>
            {Object.keys(parsed).length === 0 ? (
              <li>(empty object)</li>
            ) : (
              Object.keys(parsed).map((k) => <li key={k}>{k}</li>)
            )}
          </ul>
          <details>
            <summary>View JSON preview</summary>
            <pre style={{ maxHeight: 240, overflow: 'auto', background: '#fafafa', padding: 8 }}>{JSON.stringify(parsed, null, 2)}</pre>
          </details>
        </div>
      )}
    </div>
  );
};

export default DataUploader;
