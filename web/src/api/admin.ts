import request from '@/utils/request'



export async function getWorkList({type}: {name: string, type: 'STYLED_IMAGE' | 'VIDEO_EFFECT'}): Promise<{}> {
  return request.get(`/api/castings/${type}/list`)
}
