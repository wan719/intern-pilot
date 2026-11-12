import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StatusTag from '@/components/common/StatusTag.vue'

describe('StatusTag', () => {
  it.each([
    ['COMPLETED', '已完成'],
    ['FAILED', '失败'],
    ['RUNNING', '进行中'],
    ['PENDING', '待处理']
  ])('renders %s as %s', (status, label) => {
    expect(mount(StatusTag, { props: { status } }).text()).toContain(label)
  })
})
