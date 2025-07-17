import request from "@/utils/request";
import type { TaskStatus } from "./type";

export async function newTask({
  file,
  type,
}: {
  file: File;
  type: "STYLED_IMAGE" | "VIDEO_EFFECT";
}): Promise<{ name: string }> {
  const formData = new FormData();
  formData.append("file", file);
  return request.post(`/api/tasks/${type}/new`, formData);
}

export interface TaskOutput {
  type: "image" | "video";
  url: string;
}
export async function getTaskStatus({
  name,
  type,
}: {
  name: string;
  type: "STYLED_IMAGE" | "VIDEO_EFFECT";
}): Promise<{ status: TaskStatus; outputs: TaskOutput; name: string }> {
  return request.get(`/api/tasks/${type}/${name}/query`);
}

export async function printImageTask({
  name,
  type,
}: {
  name: string;
  type: "STYLED_IMAGE" | "VIDEO_EFFECT";
}): Promise<{ status: TaskStatus; outputs: TaskOutput }> {
  return request.post(`/api/tasks/${type}/${name}/print`);
}
