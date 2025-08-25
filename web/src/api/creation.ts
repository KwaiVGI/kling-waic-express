import request from "@/utils/request";
import type { TaskType, TaskStatus } from "./type";
import { STORAGE_TOKEN_KEY } from "@/stores/mutation-type";

export async function uploadFile({
  file,
  type,
}: {
  file: File;
  type: TaskType;
}): Promise<string> {
  const formData = new FormData();
  formData.append("file", file);
  return request.post(`/api/tasks/${type}/upload_image`, formData);
}

export async function newTask({
  url,
  type,
}: {
  url: string;
  type: TaskType;
}): Promise<{ name: string }> {
  return request.post(`/api/tasks/${type}/new`, { url });
}

export interface TaskOutput {
  type: "image" | "video";
  url: string;
}
export interface TaskInput {
  type: TaskType;
  image: string;
}
export async function getTaskStatus({
  name,
  type,
  ...params
}: {
  name: string;
  type: TaskType;
  locale: "CN" | "US";
}): Promise<{
  status: TaskStatus;
  outputs: TaskOutput;
  input: TaskInput;
  name: string;
}> {
  return request.get(`/api/tasks/${type}/${name}/query`, { params });
}

export async function printImageTask({
  name,
  type,
}: {
  name: string;
  type: TaskType;
}): Promise<{ status: TaskStatus; outputs: TaskOutput }> {
  return request.post(`/api/tasks/${type}/${name}/print`);
}

export enum PrintingStatus {
  READY = "READY", // 打印机还没拉走，在程序自己的queue中排队，aheadCount返回打印机内count+程序中前面有几个
  QUEUING = "QUEUING", // 打印机拉走了，在打印机排队，aheadCount返回打印机内count
  PRINTING = "PRINTING", // 打印机正在打印，aheadCount返回0
  COMPLETED = "COMPLETED",
  FAILED = "FAILED",
  CANCELLED = "CANCELLED",
}

export async function getPrintingStatus(name: string): Promise<{
  id: number;
  name: string;
  task: any;
  status: PrintingStatus;
  aheadCount: number | null;
}> {
  return request.get(`/api/printings/printing:${name}`, {
    headers: {
      Authorization: "Token " + localStorage.getItem(STORAGE_TOKEN_KEY),
    },
  });
}
