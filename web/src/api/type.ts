export type TaskType = "STYLED_IMAGE" | "VIDEO_EFFECT";

export enum TaskStatus {
  SUBMITTED = "SUBMITTED",
  PROCESSING = "PROCESSING",
  SUCCEED = "SUCCEED",
  FAILED = "FAILED",
}

export enum TaskOutputType {
  IMAGE = "IMAGE",
  VIDEO = "VIDEO",
}

export interface TaskOutput {
  type: TaskOutputType;
  url: string;
}
