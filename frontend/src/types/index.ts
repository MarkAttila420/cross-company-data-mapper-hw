export type DataFile = {
  name: string;
  content: string;
};

export type Mapping = {
  id: string;
  source: string;
  target: string;
  rules?: Record<string, any>;
  confidence?: number; // 0..1
};

export type ValidationResult = {
  passed: boolean;
  message: string;
};

export type TransformPreview = {
  sample?: string;
  transformedCount?: number;
  original?: any;
  transformed?: any;
};
