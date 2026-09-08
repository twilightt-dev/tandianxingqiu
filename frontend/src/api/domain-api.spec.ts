import { expectTypeOf, describe, it } from 'vitest'
import type { User } from '@/types/api'
import { getBlogLikes } from './blog'
import { seckillVoucher } from './shop'

describe('domain API return contracts', () => {
  it('returns users who liked a blog and an order ID for seckill', () => {
    expectTypeOf(getBlogLikes).returns.toEqualTypeOf<Promise<User[]>>()
    expectTypeOf(seckillVoucher).returns.toEqualTypeOf<Promise<number>>()
  })
})
