import client from './axiosClient'

export const getSummary = (params) =>
  client.get('/dashboard/summary', { params }).then((r) => r.data)