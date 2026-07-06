import type {Module} from '../types'

export const MOCK_MODULES: Module[] = [
    {id: 'image-to-pdf', name: 'Image → PDF', category: 'PDF', isHeavy: true, description: '이미지를 하나의 PDF로 묶기'},
    {id: 'pdf-merge', name: 'PDF 병합', category: 'PDF', isHeavy: true, description: '여러 PDF를 하나로 병합'},
    {id: 'pdf-split', name: 'PDF 분할', category: 'PDF', isHeavy: true, description: 'PDF를 페이지 단위로 분할'},
    {
        id: 'markdown-to-pdf',
        name: 'Markdown → PDF',
        category: 'PDF',
        isHeavy: true,
        description: 'Markdown 문서를 PDF로 변환'
    },
    {id: 'image-resize', name: '이미지 리사이즈', category: '이미지', isHeavy: true, description: '이미지 크기 및 해상도 조정'},
    {id: 'image-format', name: '이미지 포맷 변환', category: '이미지', isHeavy: true, description: 'PNG, JPG, WebP 등 포맷 변환'},
    {id: 'qr-code', name: 'QR 코드 생성', category: '생성기', isHeavy: true, description: '텍스트/URL을 QR 코드로 생성'},
    {id: 'barcode', name: '바코드 생성', category: '생성기', isHeavy: true, description: 'Code 128 등 바코드 생성'},
    {id: 'rsa-key', name: 'RSA 키 생성', category: '보안', isHeavy: true, description: 'RSA 공개키/개인키 쌍 생성'},
    {id: 'bcrypt', name: 'Bcrypt 해시', category: '보안', isHeavy: false, description: '비밀번호 Bcrypt 해시 생성 및 검증'},
    {id: 'sql-formatter', name: 'SQL 포맷터', category: '포맷터', isHeavy: false, description: 'SQL 쿼리 정렬 및 포맷'},
    {id: 'xml-formatter', name: 'XML 포맷터', category: '포맷터', isHeavy: false, description: 'XML 문서 들여쓰기 정렬'},
    {id: 'html-entity', name: 'HTML Entity', category: '포맷터', isHeavy: false, description: 'HTML 특수문자 인코딩/디코딩'},
    {id: 'json-yaml', name: 'JSON ↔ YAML', category: '변환기', isHeavy: false, description: 'JSON ↔ YAML 상호 변환'},
    {id: 'json-toml', name: 'JSON ↔ TOML', category: '변환기', isHeavy: false, description: 'JSON ↔ TOML 상호 변환'},
    {id: 'json-xml', name: 'JSON ↔ XML', category: '변환기', isHeavy: false, description: 'JSON ↔ XML 상호 변환'},
    {id: 'csv-json', name: 'CSV ↔ JSON', category: '변환기', isHeavy: false, description: 'CSV ↔ JSON 상호 변환'},
    {id: 'case-converter', name: '케이스 변환', category: '텍스트', isHeavy: false, description: 'camelCase, snake_case 등 변환'},
    {id: 'diff', name: 'Diff 비교', category: '텍스트', isHeavy: false, description: '두 텍스트 차이 시각화'},
    {id: 'regex', name: 'Regex 테스터', category: '텍스트', isHeavy: false, description: '정규표현식 실시간 테스트'},
    {id: 'url-parser', name: 'URL 파서', category: '네트워크', isHeavy: false, description: 'URL 구성 요소 분해 및 파싱'},
    {id: 'subnet-calc', name: '서브넷 계산기', category: '네트워크', isHeavy: false, description: 'IP 서브넷 마스크 계산'},
    {id: 'cron', name: 'Cron 표현식', category: 'DevOps', isHeavy: false, description: 'Cron 표현식 파싱 및 설명'},
    {id: 'sha256', name: 'SHA-256 해시', category: '유틸', isHeavy: false, description: '텍스트 SHA-256 해시 생성'},
]
