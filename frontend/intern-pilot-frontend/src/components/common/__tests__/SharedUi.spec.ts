import { mount } from '@vue/test-utils'
import { Document } from '@element-plus/icons-vue'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { h, markRaw } from 'vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import AiInsightPanel from '@/components/common/AiInsightPanel.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import PageContainer from '@/components/common/PageContainer.vue'
import PageHero from '@/components/common/PageHero.vue'
import StatCard from '@/components/common/StatCard.vue'
import TableShell from '@/components/common/TableShell.vue'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/', component: { template: '<div />' } }]
})

describe('shared product UI primitives', () => {
  it('keeps PageContainer action and hero slots available around page content', () => {
    const wrapper = mount(PageContainer, {
      props: { title: '页面标题', eyebrow: '概览', width: 'wide' },
      slots: {
        actions: '<button>创建</button>',
        hero: '<p>目标说明</p>',
        default: '<div>页面内容</div>'
      },
      global: { plugins: [router] }
    })

    expect(wrapper.text()).toContain('创建')
    expect(wrapper.text()).toContain('目标说明')
    expect(wrapper.text()).toContain('页面内容')
    expect(wrapper.classes()).toContain('page-container--wide')
  })

  it('uses the hero as the only page-level heading when it supplies a PageHero', () => {
    const wrapper = mount(PageContainer, {
      props: { title: '后备标题' },
      slots: {
        hero: () => h(PageHero, { title: '页面目标' })
      },
      global: { plugins: [router] }
    })

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.get('h1').text()).toBe('页面目标')
  })

  it('renders AppEmpty action content in its default slot', () => {
    const wrapper = mount(AppEmpty, {
      props: { title: '还没有目标岗位' },
      slots: { default: '<button>添加岗位</button>' }
    })

    expect(wrapper.text()).toContain('添加岗位')
  })

  it('renders optional StatCard context without changing its base props', () => {
    const wrapper = mount(StatCard, {
      props: { label: '准备度', value: '78%', icon: markRaw(Document), trend: '+6%', hint: '较上周' }
    })

    expect(wrapper.text()).toContain('+6%')
    expect(wrapper.text()).toContain('较上周')
  })

  it('renders PageHero action and content slots', () => {
    const wrapper = mount(PageHero, {
      props: { title: '下一步行动', description: '先完成资料准备' },
      slots: { actions: '<button>开始</button>', default: '<span>补充内容</span>' }
    })

    expect(wrapper.get('h1').text()).toBe('下一步行动')
    expect(wrapper.text()).toContain('开始')
    expect(wrapper.text()).toContain('补充内容')
  })

  it('emits reset from the generic filter action', async () => {
    const wrapper = mount(FilterBar, {
      slots: { filters: '<input aria-label="关键词" />', actions: '<button>筛选</button>' }
    })

    await wrapper.get('[data-filter-reset]').trigger('click')

    expect(wrapper.emitted('reset')).toHaveLength(1)
    expect(wrapper.text()).toContain('筛选')
  })

  it('shows empty actions only after loading has finished with no data', () => {
    const loading = mount(TableShell, {
      props: { loading: true, empty: true, emptyTitle: '没有记录' },
      slots: { 'empty-actions': '<button>新建记录</button>' }
    })
    const empty = mount(TableShell, {
      props: { loading: false, empty: true, emptyTitle: '没有记录', emptyHint: '请调整筛选条件' },
      slots: { 'empty-actions': '<button>新建记录</button>' }
    })

    expect(loading.text()).not.toContain('新建记录')
    expect(empty.text()).toContain('没有记录')
    expect(empty.text()).toContain('新建记录')
    expect(empty.text().match(/没有记录/g)).toHaveLength(1)
    expect(empty.text()).toContain('请调整筛选条件')
  })

  it('exposes AI insight title as a semantic heading', () => {
    const wrapper = mount(AiInsightPanel, {
      props: { title: '匹配优势', tone: 'strength' },
      slots: { default: '<p>核心技能匹配</p>' }
    })

    expect(wrapper.get('h2').text()).toBe('匹配优势')
    expect(wrapper.text()).toContain('核心技能匹配')
  })
})
