import request from "@/utils/request";
import type { TaskOutput, TaskStatus, TaskType } from "./type";

interface Task {
  id: number;
  name: string;
  taskIds: string[];
  status: TaskStatus;
  type: TaskType;
  filename: string;
  outputs: TaskOutput;
  createTime: Date; // or string if you prefer to keep it as ISO string
}

export interface CastingImage {
  id: number;
  name: String;
  task: Task;
  score: number;
  originScore: number;
}

interface CastingListResult {
  total: number;
  score: number; // pcursor
  hasMore: boolean;
  castings: CastingImage[];
}

// 获取作品列表
export async function getCastings({
  type,
  ...params
}: {
  type: TaskType;
  keyword: string;
  score: number | null;
  pageNum: number;
  pageSize: number;
}): Promise<CastingListResult> {
  return request.get(`/api/castings/${type}/list`, {
    params,
  });
}

enum TaskOperateAction {
  PIN = "PIN",
  UNPIN = "UNPIN",
  PROMOTE = "PROMOTE",
  DELETE = "DELETE",
}

// 操作
export async function operateCasting({
  type,
  ...params
}: {
  type: TaskType;
  input: TaskOperateAction;
}): Promise<CastingListResult> {
  return request.post(`/api/castings/${type}/operate`, {
    params,
  });
}

// 获取固定的作品
export async function getPined({
  type,
}: {
  type: TaskType;
}): Promise<CastingListResult> {
  return request.post(`/api/castings/${type}/pined`);
}

// 获取大屏列表
export async function getScreenList({
  type,
  ...params
}: {
  type: TaskType;
  num: number;
}): Promise<TaskOutput[]> {
  return request.get(`/api/castings/${type}/screen`, { params });
}
