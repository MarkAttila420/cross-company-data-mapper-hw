import React, { FC } from 'react';
import { ValidationResult } from '../types';

type Props = {
  results: ValidationResult[];
};

const ValidationResults: FC<Props> = ({ results }) => {
  return (
    <div>
      <h3>Validation Results</h3>
      {results.length === 0 ? (
        <p>No validation results.</p>
      ) : (
        <ul>
          {results.map((r, i) => (
            <li key={i} style={{ color: r.passed ? 'green' : 'red' }}>
              {r.message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default ValidationResults;
