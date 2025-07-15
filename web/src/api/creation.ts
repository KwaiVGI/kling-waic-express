import request from '@/utils/request'


export async function newTask({file, type}: {file: File, type: 'STYLED_IMAGE' | 'VIDEO_EFFECT'}): Promise<{name: string}> {
    const formData = new FormData();
    formData.append('file', file);
  return request.post(`/api/tasks/${type}/new`, formData)
}


export enum TaskStatus {
    SUBMITTED = 'SUBMITTED',
    PROCESSING = 'PROCESSING',
    SUCCEED = 'SUCCEED',
    FAILED = 'FAILED'
}
export interface TaskOutput {
    type: 'image' | 'video',
    url: string
}
export async function getTaskStatus({name, type}: {name: string, type: 'STYLED_IMAGE' | 'VIDEO_EFFECT'}): Promise<{status: TaskStatus, outputs: TaskOutput}> {
  return request.get(`/api/tasks/${type}/${name}`)
}
