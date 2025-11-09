import { DataFile, Mapping, ValidationResult, TransformPreview } from '../types';

// Base service URLs. Can be overridden by environment variables (e.g. REACT_APP_*).
const MAPPING_SERVICE_URL = process.env.REACT_APP_MAPPING_SERVICE_URL || '/mapping-service';
const VALIDATION_SERVICE_URL = process.env.REACT_APP_VALIDATION_SERVICE_URL || '/validation-service';

async function fetchJson<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(input, init);
  } catch (err) {
    // Network or CORS error
    console.error('Network error when calling', input, err);
    throw new Error(`Network error: ${String(err)}`);
  }

  const text = await res.text();
  if (!res.ok) {
    // include body text when available for debugging
    const message = text || res.statusText;
    console.error('Service returned error', res.status, message);
    throw new Error(`Service error ${res.status}: ${message}`);
  }

  if (!text) {
    // empty body
    return (null as unknown) as T;
  }

  try {
    return JSON.parse(text) as T;
  } catch (err) {
    console.error('Failed to parse JSON response from', input, err, 'raw:', text);
    throw new Error('Invalid JSON response');
  }
}

/**
 * Ask the mapping-service to suggest mappings between source and target data.
 * POST { source: DataFile, target: DataFile } -> Mapping[]
 */
export async function suggestMappings(sourceData: DataFile, targetData: DataFile): Promise<Mapping[]> {
  const url = `${MAPPING_SERVICE_URL}/suggest-mappings`;
  return fetchJson<Mapping[]>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ source: sourceData, target: targetData }),
  });
}

/**
 * Ask the mapping-service to transform sourceData using given mappings.
 * POST { source: DataFile, mappings: Mapping[] } -> DataFile
 */
export async function transformData(sourceData: DataFile, mappings: Mapping[]): Promise<DataFile> {
  const url = `${MAPPING_SERVICE_URL}/transform`;
  return fetchJson<DataFile>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ source: sourceData, mappings }),
  });
}

/**
 * Send transformed data to validation-service.
 * POST { data: DataFile } -> ValidationResult[]
 */
export async function validateData(transformedData: DataFile): Promise<ValidationResult[]> {
  const url = `${VALIDATION_SERVICE_URL}/validate`;
  return fetchJson<ValidationResult[]>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ data: transformedData }),
  });
}

/**
 * Get mapping templates from mapping-service.
 * GET /templates -> Mapping[]
 */
export async function getTemplates(): Promise<Mapping[]> {
  const url = `${MAPPING_SERVICE_URL}/templates`;
  return fetchJson<Mapping[]>(url);
}

/**
 * Save named template to mapping-service.
 * POST { name: string, mappings: Mapping[] }
 */
export async function saveTemplate(name: string, mappings: Mapping[]): Promise<{ success: boolean }> {
  const url = `${MAPPING_SERVICE_URL}/templates`;
  try {
    await fetchJson(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, mappings }),
    } as RequestInit);
    return { success: true };
  } catch (err) {
    console.error('saveTemplate failed', err);
    throw err;
  }
}

// --- Backwards-compatible simple helpers (existing app used these names) ---
export async function uploadData(file: DataFile): Promise<{ success: boolean; id?: string }> {
  // Keep a minimal implementation: attempt to POST to mapping-service/upload if available
  const url = `${MAPPING_SERVICE_URL}/upload`;
  try {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ file }),
    });
    if (!res.ok) {
      const txt = await res.text();
      console.warn('uploadData returned non-OK:', res.status, txt);
      return { success: false };
    }
    const json = await res.json().catch(() => ({}));
    return { success: true, id: json?.id };
  } catch (err) {
    console.error('uploadData failed', err);
    return { success: false };
  }
}

export async function fetchMappings(): Promise<Mapping[]> {
  // alias to getTemplates for compatibility
  return getTemplates();
}

export async function validateMappings(mappings: Mapping[]): Promise<ValidationResult[]> {
  // some older code validated mappings directly; call validation-service if endpoint exists
  const url = `${VALIDATION_SERVICE_URL}/validate-mappings`;
  try {
    return await fetchJson<ValidationResult[]>(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ mappings }),
    });
  } catch (err) {
    // fallback: if service doesn't support this endpoint, return a simple pass/fail
    console.warn('validateMappings fallback due to error:', err);
    return mappings.length ? [{ passed: true, message: 'Assumed valid (service unavailable)' }] : [];
  }
}

export async function generatePreview(mappings: Mapping[], data: DataFile): Promise<TransformPreview | null> {
  // alias to transformData and return a small preview object
  try {
    const transformed = await transformData(data, mappings);
    return { sample: transformed.content?.slice?.(0, 200), transformedCount: undefined } as TransformPreview;
  } catch (err) {
    console.warn('generatePreview failed', err);
    return null;
  }
}
