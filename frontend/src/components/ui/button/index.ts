import type { VariantProps } from 'class-variance-authority'
import { cva } from 'class-variance-authority'

export { default as Button } from './Button.vue'

// Adapted from the shadcn-vue new-york-v4 Button for the prefixed workspace utilities.
export const buttonVariants = cva(
  'ip:inline-flex ip:items-center ip:justify-center ip:gap-2 ip:whitespace-nowrap ip:rounded-md ip:border-0 ip:cursor-pointer ip:text-sm ip:font-medium ip:transition-colors ip:disabled:pointer-events-none ip:disabled:opacity-50 ip:focus-visible:ring-3 ip:focus-visible:ring-ring/50',
  {
    variants: {
      variant: {
        default: 'ip:bg-primary ip:text-primary-foreground ip:hover:bg-primary/90'
      },
      size: {
        default: 'ip:h-9 ip:px-4 ip:py-2'
      }
    },
    defaultVariants: { variant: 'default', size: 'default' }
  }
)
export type ButtonVariants = VariantProps<typeof buttonVariants>
