import { createRouter, createWebHistory } from 'vue-router'
import IndexPage from '../pages/IndexPage.vue'
import { estaLogado } from '../services/auth.js'

const routes = [
  { path: '/', name: 'analise', component: IndexPage },
  {
    path: '/historico',
    name: 'historico',
    component: () => import('../pages/HistoricoPage.vue'),
    meta: { exigeLogin: true }
  },
  {
    path: '/entrar',
    name: 'entrar',
    component: () => import('../pages/EntrarPage.vue'),
    meta: { somenteVisitante: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  if (to.meta.exigeLogin && !estaLogado.value) {
    return { name: 'entrar', query: { redirect: to.fullPath } }
  }
  if (to.meta.somenteVisitante && estaLogado.value) {
    return { path: '/' }
  }
  return true
})

export default router
