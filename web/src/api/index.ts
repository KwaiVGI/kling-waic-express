import request from "@/utils/request";
import type { TaskType } from "./type";

export async function queryProse(): Promise<any> {
  return request("/prose");
}

export async function getLatestToken(
  type: TaskType
): Promise<{ value: string }> {
  return request(`/api/tokens/${type}/latest`);
}
