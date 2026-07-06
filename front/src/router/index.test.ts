import {describe, expect, it} from 'vitest'
import {router} from './index'

describe('router', () => {
    it('/ 라우트가 존재한다', () => {
        const routes = router.getRoutes()
        expect(routes.some(r => r.path === '/')).toBe(true)
    })

    it('/tools/:moduleId 라우트가 존재한다', () => {
        const routes = router.getRoutes()
        expect(routes.some(r => r.path === '/tools/:moduleId')).toBe(true)
    })
})
