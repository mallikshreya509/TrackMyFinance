import client from './axiosClient'

export const listCategories = () => client.get('/categories').then((r) => r.data)
