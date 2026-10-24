import client from './axiosClient'

export const listExpenses = ({ from, to, categoryId, page = 0, size = 10 }) => {
  const params = { page, size, sort: 'date,desc' }
  if (from) params.from = from
  if (to) params.to = to
  if (categoryId) params.categoryId = categoryId
  return client.get('/expenses', { params }).then((r) => r.data)
}

export const createExpense = (body) => client.post('/expenses', body).then((r) => r.data)

export const updateExpense = (id, body) =>
  client.put(`/expenses/${id}`, body).then((r) => r.data)

export const deleteExpense = (id) => client.delete(`/expenses/${id}`)