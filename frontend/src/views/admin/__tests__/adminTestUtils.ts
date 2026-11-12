import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, type Component } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'

const DialogStub = defineComponent({
  props: { modelValue: Boolean, title: String },
  template: `
    <section v-if="modelValue" class="el-dialog" role="dialog" :aria-label="title">
      <h2>{{ title }}</h2>
      <slot />
      <footer class="el-dialog__footer"><slot name="footer" /></footer>
    </section>
  `
})

export async function mountAdminPage(component: Component, path: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path, component: { template: '<div />' }, meta: { title: 'Admin test' } }]
  })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(component, {
    global: {
      plugins: [router],
      stubs: { teleport: true, ElDialog: DialogStub }
    }
  })
  await flushPromises()
  return wrapper
}

export function buttonByText(wrapper: ReturnType<typeof mount>, label: string, index = 0) {
  const matches = wrapper.findAll('button').filter((item) => item.text().trim() === label)
  if (!matches[index]) throw new Error(`Missing button: ${label} at index ${index}`)
  return matches[index]
}

export function plain<T>(value: T): T {
  return JSON.parse(JSON.stringify(value))
}
