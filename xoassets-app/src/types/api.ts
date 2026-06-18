export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
  traceId?: string;
}

export interface ApiPage<T> {
  records?: T[];
  list?: T[];
  total?: number;
  current?: number;
  size?: number;
}
