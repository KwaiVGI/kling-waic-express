import request from "@/utils/request";
import type { TaskType, TaskStatus } from "./type";

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
