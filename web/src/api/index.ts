import request from '@/utils/request'

export async function queryProse(): Promise<any> {
  return request('/prose')
}

export async function getLatestToken(): Promise<{name: string}> {
  return request('/api/tokens/latest', {
    headers: {
      'Authorization': 'Token wEJvopXEvl6OnNUHl8DbAd-8Ixkjef9'
    }
  })
}