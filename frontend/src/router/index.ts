import { createRouter, createWebHistory } from 'vue-router'

import EditorView from '@/views/editor/index.vue'
import FormPreviewView from '@/views/form-preview/index.vue'
import FormManageView from '@/views/form-manage/index.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/form-manage',
    },
    {
      path: '/form-manage',
      name: 'form-manage',
      component: FormManageView,
    },
    {
      path: '/editor',
      name: 'editor',
      component: EditorView,
    },
    {
      path: '/form-preview',
      name: 'form-preview',
      component: FormPreviewView,
    },
  ],
})

export default router
