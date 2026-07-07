import type {Module} from '../types'
import {MOCK_MODULES} from './mock'

const CATEGORY_MAP: Record<string, string> = {
    pdf: 'PDF',
    image: '이미지',
    generator: '생성기',
    codegen: '생성기',
    security: '보안',
    formatter: '포맷터',
    converter: '변환기',
    text: '텍스트',
    network: '네트워크',
    devops: 'DevOps',
    util: '유틸',
}

export function normalizeApiModules(data: Module[]): Module[] {
    const backendModules = data.map(m => ({
        ...m,
        category: CATEGORY_MAP[m.category] ?? m.category,
    }))
    const frontendOnly = MOCK_MODULES.filter(m => m.isFrontendOnly)
    return [...backendModules, ...frontendOnly]
}
