import request from "@/utils/request";

export async function queryProse(): Promise<any> {
  return request("/prose");
}

export async function getLatestToken(): Promise<{ value: string }> {
  return request("/api/tokens/latest");
}
