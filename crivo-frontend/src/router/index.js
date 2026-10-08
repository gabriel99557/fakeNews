import { createRouter, createWebHistory } from 'vue-router'
import IndexPage from '../pages/IndexPage.vue'

const routes = [
  { path: '/', name: 'analise', component: IndexPage },
  {
    path: '/historico',
    name: 'historico',
    component: () => import('../pages/HistoricoPage.vue')
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})
