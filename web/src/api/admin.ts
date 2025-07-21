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
  name: string;
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

export enum TaskOperateAction {
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
  name: string;
  action: TaskOperateAction;
}): Promise<CastingListResult> {
  return request.post(`/api/castings/${type}/operate`, params);
}

// 获取固定的作品
export async function getPined({
  type,
}: {
  type: TaskType;
}): Promise<CastingImage> {
  return request.get(`/api/castings/${type}/pinned`);
}

// 获取大屏列表
export async function getScreenList({
  type,
  ...params
}: {
  type: TaskType;
  num: number;
}): Promise<CastingImage[]> {
  return request.get(`/api/castings/${type}/screen`, { params });
}

// 获取打印队列列表
export async function getPrintList(params: {
  keyword?: string;
}): Promise<CastingImage[]> {
  return request.get(`/api/printings/queryAll`, {
    params,
  });
}

// 管理后台打印照片
export async function printImageFromConsole({
  name,
  type,
}: {
  name: string;
  type: "STYLED_IMAGE" | "VIDEO_EFFECT";
}): Promise<{ status: TaskStatus; outputs: TaskOutput }> {
  return request.post(`/api/tasks/${type}/${name}/printFromConsole`);
}
