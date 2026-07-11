import type {Module} from '../types'

export const MOCK_MODULES: Module[] = [
    // PDF (Heavy)
    {id: 'image-to-pdf', name: 'Image → PDF', category: 'PDF', isHeavy: true, description: '이미지를 하나의 PDF로 묶기'},
    {id: 'pdf-merge', name: 'PDF 병합', category: 'PDF', isHeavy: true, description: '여러 PDF를 하나로 병합'},
    {id: 'pdf-split', name: 'PDF 분할', category: 'PDF', isHeavy: true, description: 'PDF를 페이지 단위로 분할'},
    {id: 'markdown-to-pdf', name: 'Markdown → PDF', category: 'PDF', isHeavy: true, description: 'Markdown 문서를 PDF로 변환'},

    // 이미지 (Heavy)
    {id: 'image-resize', name: '이미지 리사이즈', category: '이미지', isHeavy: true, description: '이미지 크기 및 해상도 조정'},
    {id: 'image-format', name: '이미지 포맷 변환', category: '이미지', isHeavy: true, description: 'PNG, JPG, WebP 등 포맷 변환'},
    {id: 'gif-create', name: 'GIF 생성', category: '이미지', isHeavy: true, description: '이미지 시퀀스를 GIF로 변환'},

    // 생성기
    {id: 'qr-code', name: 'QR 코드 생성', category: '생성기', isHeavy: false, description: '텍스트/URL을 QR 코드로 생성'},
    {id: 'barcode', name: '바코드 생성', category: '생성기', isHeavy: false, description: 'Code 128 등 바코드 생성'},
    {id: 'json-schema-to-dto', name: 'JSON Schema → DTO', category: '생성기', isHeavy: true, description: 'JSON Schema로 Java DTO 클래스 생성'},
    {id: 'openapi-to-code', name: 'OpenAPI → 코드', category: '생성기', isHeavy: true, description: 'OpenAPI 스펙으로 클라이언트 코드 생성'},

    // 보안
    {id: 'rsa-key', name: 'RSA 키 생성', category: '보안', isHeavy: false, description: 'RSA 공개키/개인키 쌍 생성'},
    {id: 'bcrypt', name: 'Bcrypt 해시', category: '보안', isHeavy: false, description: '비밀번호 Bcrypt 해시 생성 및 검증'},
    {id: 'vuln-scan', name: '취약점 스캔', category: '보안', isHeavy: true, description: '의존성 파일(Gradle/Maven) CVE 취약점 검사'},

    // 포맷터
    {id: 'sql-formatter', name: 'SQL 포맷터', category: '포맷터', isHeavy: false, description: 'SQL 쿼리 정렬 및 포맷'},
    {id: 'xml-formatter', name: 'XML 포맷터', category: '포맷터', isHeavy: false, description: 'XML 문서 들여쓰기 정렬'},
    {id: 'html-entity', name: 'HTML Entity', category: '포맷터', isHeavy: false, description: 'HTML 특수문자 인코딩/디코딩'},

    // 변환기
    {id: 'json-yaml', name: 'JSON ↔ YAML', category: '변환기', isHeavy: false, description: 'JSON ↔ YAML 상호 변환'},
    {id: 'json-toml', name: 'JSON ↔ TOML', category: '변환기', isHeavy: false, description: 'JSON ↔ TOML 상호 변환'},
    {id: 'json-xml', name: 'JSON ↔ XML', category: '변환기', isHeavy: false, description: 'JSON ↔ XML 상호 변환'},
    {id: 'csv-json', name: 'CSV ↔ JSON', category: '변환기', isHeavy: false, description: 'CSV ↔ JSON 상호 변환'},

    // 텍스트
    {id: 'case-converter', name: '케이스 변환', category: '텍스트', isHeavy: false, description: 'camelCase, snake_case 등 변환'},
    {id: 'text-diff', name: 'Diff 비교', category: '텍스트', isHeavy: false, description: '두 텍스트 차이 시각화'},
    {id: 'regex-tester', name: 'Regex 테스터', category: '텍스트', isHeavy: false, description: '정규표현식 실시간 테스트'},

    // 네트워크
    {id: 'url-parser', name: 'URL 파서', category: '네트워크', isHeavy: false, description: 'URL 구성 요소 분해 및 파싱'},
    {id: 'subnet-calc', name: '서브넷 계산기', category: '네트워크', isHeavy: false, description: 'IP 서브넷 마스크 계산'},
    {id: 'html-fetch', name: 'HTML 가져오기', category: '네트워크', isHeavy: false, description: 'URL에서 HTML 소스 가져오기'},

    // DevOps
    {id: 'cron', name: 'Cron 표현식', category: 'DevOps', isHeavy: false, description: 'Cron 표현식 파싱 및 다음 실행 시각'},
    {id: 'docker-compose', name: 'Docker Compose 변환', category: 'DevOps', isHeavy: false, description: 'docker run 명령어 → docker-compose.yml 변환'},

    // 유틸
    {id: 'sha256', name: 'SHA-256 해시', category: '유틸', isHeavy: false, description: '텍스트 SHA-256 해시 생성'},
    {id: 'multi-hash', name: '다중 해시', category: '유틸', isHeavy: false, description: 'MD5, SHA-1, SHA-256, SHA-512 동시 생성'},
    {id: 'hmac', name: 'HMAC 서명', category: '유틸', isHeavy: false, description: 'HMAC-SHA256/SHA512 서명 생성'},
    {id: 'aes', name: 'AES 암호화', category: '유틸', isHeavy: false, description: 'AES-256 CBC 암호화/복호화'},
    {id: 'totp', name: 'TOTP 생성', category: '유틸', isHeavy: false, description: 'TOTP 일회용 코드 생성 (RFC 6238)'},

    // 프론트엔드 전용 도구 (브라우저에서 직접 처리)
    {id: 'json-formatter', name: 'JSON 포맷터', category: '포맷터', isHeavy: false, isFrontendOnly: true, description: 'JSON 정렬 및 미니파이'},
    {id: 'base64', name: 'Base64', category: '포맷터', isHeavy: false, isFrontendOnly: true, description: '텍스트 ↔ Base64 인코딩/디코딩'},
    {id: 'url-encode', name: 'URL 인코딩', category: '포맷터', isHeavy: false, isFrontendOnly: true, description: '텍스트 ↔ URL 인코딩'},
    {id: 'jwt-decoder', name: 'JWT 디코더', category: '포맷터', isHeavy: false, isFrontendOnly: true, description: 'JWT 토큰 Header·Payload 파싱'},
    {id: 'timestamp', name: '타임스탬프', category: '포맷터', isHeavy: false, isFrontendOnly: true, description: 'Unix timestamp ↔ 날짜/시간 변환'},
    {id: 'color-code', name: '색상 코드', category: '포맷터', isHeavy: false, isFrontendOnly: true, description: 'HEX ↔ RGB ↔ HSL 변환'},
    {id: 'uuid', name: 'UUID 생성기', category: '생성기', isHeavy: false, isFrontendOnly: true, description: 'UUID v4 무작위 생성'},
    {id: 'char-count', name: '글자 수 카운터', category: '텍스트', isHeavy: false, isFrontendOnly: true, description: '문자 수·단어 수·바이트 수 계산'},
    {id: 'keyboard-convert', name: '한영 변환', category: '텍스트', isHeavy: false, isFrontendOnly: true, description: '한→영, 영→한 키보드 레이아웃 변환'},
    {id: 'whitespace', name: '공백 정규화', category: '텍스트', isHeavy: false, isFrontendOnly: true, description: '연속 공백·탭·줄바꿈 정규화'},
]
