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

const META_BY_ID = new Map(MOCK_MODULES.map(m => [m.id, m]))

// 통합 도구(인코더/데이터 변환/텍스트 유틸/다중 해시)로 흡수되어 목록에서 숨기는 백엔드 모듈.
// 모듈 자체는 백엔드에 남아 있으며 통합 페이지가 내부적으로 호출한다.
const HIDDEN_MODULE_IDS = new Set([
    'sha256', 'html-entity', 'json-yaml', 'json-toml', 'json-xml', 'csv-json', 'case-converter',
])

export function normalizeApiModules(data: Module[]): Module[] {
    const backendModules = data
        .filter(m => !HIDDEN_MODULE_IDS.has(m.id))
        .map(m => ({
            ...m,
            category: CATEGORY_MAP[m.category] ?? m.category,
            description: m.description ?? META_BY_ID.get(m.id)?.description,
            keywords: m.keywords ?? META_BY_ID.get(m.id)?.keywords,
        }))
    const frontendOnly = MOCK_MODULES.filter(m => m.isFrontendOnly)
    return [...backendModules, ...frontendOnly]
}
