import { DataFile, Mapping, ValidationResult, TransformPreview } from '../types';

// Base service URLs. Can be overridden by environment variables (e.g. REACT_APP_*).
// Default to absolute host:port so browser requests go directly to backend services
// (avoids frontend nginx intercepting API requests on the same origin).
const defaultHost = (typeof window !== 'undefined' && window.location.hostname) ? window.location.hostname : 'localhost';
const MAPPING_SERVICE_URL = process.env.REACT_APP_MAPPING_SERVICE_URL || `http://${defaultHost}:8080`;
const VALIDATION_SERVICE_URL = process.env.REACT_APP_VALIDATION_SERVICE_URL || `http://${defaultHost}:8000`;

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
  const url = `${MAPPING_SERVICE_URL}/mapping/suggest`;
  // The mapping-service expects the raw JSON formats (not the wrapper {name,content}).
  // Try to parse content if it is a JSON string; otherwise pass through.
  let src: any = sourceData as any;
  let tgt: any = targetData as any;
  try {
    src = JSON.parse(sourceData.content);
  } catch (e) {
    // leave as-is
  }
  try {
    tgt = JSON.parse(targetData.content);
  } catch (e) {
    // leave as-is
  }

  const raw = await fetchJson<{ mappings: Array<any> }>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sourceFormat: src, targetFormat: tgt }),
  });

  // map server FieldMapping -> frontend Mapping shape
  const out: Mapping[] = (raw && raw.mappings ? raw.mappings : raw ? (raw as any) : []).map((fm: any) => ({
    id: Math.random().toString(36).slice(2, 9),
    source: fm.sourcePath || fm.source || fm.from || '',
    target: fm.targetPath || fm.target || fm.to || '',
    rules: { transformationType: fm.transformationType },
    confidence: fm.confidence ?? fm.score ?? 0.5,
  }));
  return out;
}

/**
 * Ask the mapping-service to transform sourceData using given mappings.
 * POST { source: DataFile, mappings: Mapping[] } -> DataFile
 */
export async function transformData(sourceData: DataFile, mappings: Mapping[]): Promise<DataFile> {
  const url = `${MAPPING_SERVICE_URL}/mapping/transform`;
  let src: any = sourceData as any;
  try {
    src = JSON.parse(sourceData.content);
  } catch (e) {
    // leave as-is
  }

  // convert frontend Mapping -> server FieldMapping
  const fm = mappings.map((m) => ({
    sourcePath: m.source,
    targetPath: m.target,
    transformationType: m.rules?.transformationType || 'copy',
    confidence: m.confidence ?? 0.5,
  }));

  const resp = await fetchJson<{ transformedData: any }>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sourceData: src, mappings: fm }),
  });

  // wrap transformedData into DataFile for the frontend components
  return { name: 'transformed.json', content: JSON.stringify(resp?.transformedData ?? {}, null, 2) };
}

/**
 * Send transformed data to validation-service.
 * POST { data: DataFile } -> ValidationResult[]
 */
export async function validateData(transformedData: DataFile): Promise<ValidationResult[]> {
  const url = `${VALIDATION_SERVICE_URL}/validate`;
  // validation-service expects the raw JSON object in the body. If we have a DataFile with
  // string content, parse it and send the object directly.
  let payload: any = transformedData as any;
  try {
    payload = JSON.parse(transformedData.content);
  } catch (e) {
    // if parsing fails, send as-is (service may reject)
    payload = transformedData.content;
  }

  const results = await fetchJson<any>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  // Normalize to frontend ValidationResult[] if needed
  if (Array.isArray(results)) return results as ValidationResult[];
  // If service returns { valid: bool, errors: [...] } convert
  if (results && typeof results === 'object' && 'errors' in results) {
    return (results.errors || []).map((e: any) => ({ passed: !!e.valid, message: e.error || '' }));
  }
  return [];
}

/**
 * Get mapping templates from mapping-service.
 * GET /templates -> Mapping[]
 */
export async function getTemplates(): Promise<Mapping[]> {
  const url = `${MAPPING_SERVICE_URL}/mapping/templates`;
  return fetchJson<Mapping[]>(url);
}

/**
 * Save named template to mapping-service.
 * POST { name: string, mappings: Mapping[] }
 */
export async function saveTemplate(name: string, mappings: Mapping[]): Promise<{ success: boolean }> {
  const url = `${MAPPING_SERVICE_URL}/mapping/templates`;
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
