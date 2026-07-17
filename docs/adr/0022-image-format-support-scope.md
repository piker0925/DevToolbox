# ADR-0022 이미지 포맷 지원 범위 (TwelveMonkeys 도입, AVIF/HEIC 제외)

## 상태

확정 (2026-07-17)

## 배경

"AVIF나 여러 이미지 포맷을 못 읽어서 불편하다, 최대한 많이 지원할 수 없나"는 요청.
기존 백엔드는 `javax.imageio` 기본 플러그인만 사용해 사실상 JPEG/PNG/GIF/BMP만 읽고 쓸 수 있었다.

조사 결과 포맷 지원 확장은 성격이 다른 두 축으로 나뉜다.

**읽기(입력) 확장** — [TwelveMonkeys ImageIO](https://github.com/haraldk/TwelveMonkeys)는 `javax.imageio.*` 서비스 등록(SPI) 방식 플러그인이라, 순수 JVM으로 동작하고 `ImageIO.read`/`ImageIO.write`를 쓰는 기존 코드는 수정 없이 새 리더를 자동으로 인식한다. WebP(읽기 전용)·TIFF(읽기·쓰기 모두 지원)·PSD·ICNS 등을 이 방식으로 얻을 수 있다.

**쓰기(출력) 확장 · AVIF/HEIC** — WebP *쓰기*, AVIF(읽기/쓰기 모두), HEIC(아이폰 사진) 전부 자유 라이선스의 순수 JVM 구현이 존재하지 않는다. 가능한 방법은 두 갈래뿐이다.
1. 네이티브 코덱 바인딩(JNI/JNA) 또는 CLI 바이너리 shellout(`cwebp`/`avifenc`/`heif-convert` 등) — 어느 쪽이든 Docker 배포 이미지, 로컬 개발 환경(이 프로젝트는 백엔드를 Docker가 아니라 로컬 Gradle로 직접 구동한다), CI 전부에 시스템 패키지를 추가로 설치해야 한다. ADR-0004/ADR-0008에서 이미 webp-imageio(JNI)를 Oracle Cloud ARM64 `UnsatisfiedLinkError` 위험으로 배제한 것과 같은 종류의 비용이며, 대상 포맷만 늘어난 것이다.
2. 상용 라이브러리(예: JDeli) — 순수 JVM이지만 유료 라이선스·기업 지원 판매 모델이라 예산 없는 개인 포트폴리오 프로젝트에 맞지 않는다.

## 결정

**읽기 확장은 도입한다. 쓰기 확장(WebP 쓰기·AVIF·HEIC)은 도입하지 않는다.**

추가한 의존성 (`back/build.gradle.kts`):
- `com.twelvemonkeys.imageio:imageio-webp:3.14.0` — WebP **읽기 전용**
- `com.twelvemonkeys.imageio:imageio-tiff:3.14.0` — TIFF **읽기·쓰기**
- `com.twelvemonkeys.imageio:imageio-jpeg:3.14.0` — JDK 기본 JPEG 리더보다 CMYK 등 변형에 더 안정적인 대체 리더(포맷 목록 자체는 안 늘지만 "이 JPEG은 왜 안 열리지" 류 실패를 줄인다)

영향받는 모듈 (전부 `ImageIO.getImageReaders`/`ImageIO.read`로 리더를 동적 탐색하므로 모듈 코드 변경 없이 자동으로 적용됨):
- `image-format` — WebP/TIFF 업로드 → PNG/JPG/TIFF로 변환 가능. 출력 포맷에 `tiff` 추가(TwelveMonkeys가 쓰기까지 지원하는 유일한 신규 포맷이라 공짜로 얻은 이득)
- `image-resize` — WebP/TIFF 업로드 후 리사이즈 가능. 단, WebP는 쓰기 리더가 없어 결과를 원본 확장자(.webp)로 그대로 쓰면 실패한다 — `ImageResizeModule.extension()`이 쓰기 가능한 라이터가 있는지 확인해 없으면 `.png`로 대체하도록 수정했다 (무손실이라 안전한 기본값)
- `gif-create` — 프레임 입력으로 WebP/TIFF 허용(출력은 항상 GIF라 쓰기 가능 여부와 무관)

프론트 `fileAccept` 목록(`front/src/config/toolConfigs/image.ts`)도 세 모듈 모두 `.webp,.tiff,.tif`를 추가해 파일 선택창에서부터 노출한다.

**AVIF·HEIC는 입력으로도 받지 않는다.** 특히 HEIC(아이폰 기본 사진 포맷)는 "내가 가진 사진을 못 올린다"는 실사용 불편의 큰 축이지만, 이번 결정에서 명시적으로 제외한다.

## 재검토 조건

이 결정은 "영원히 불가능"이 아니라 "지금 비용 대비 이익이 안 맞는다"는 판단이다. 다음 중 하나가 성립하면 다시 검토한다.

- HEIC/AVIF 업로드 실패가 실제로 반복 관측되는 정도의 트래픽이 생긴다 (지금은 포트폴리오 단계라 트래픽 자체가 목표가 아님 — 관련 결정: 포트폴리오 우선순위)
- 순수 JVM 무료 AVIF/HEIC 구현이 새로 등장한다
- 백엔드 배포 방식이 바뀌어(예: 이미 Docker 이미지를 쓰는 워커 전용 프로세스 분리 등) 시스템 바이너리 설치 비용이 지금보다 낮아진다

## 결과

- `back/build.gradle.kts`에 TwelveMonkeys 3개 모듈 추가
- `ImageResizeModule.extension()` — 쓰기 불가 포맷은 png로 대체하는 방어 로직 추가 (WebP 입력 시 무한정 500 에러로 실패하던 잠재 버그를 사전에 막음)
- `ImageFormatModule` — targetFormat에 `tiff` 추가
- 테스트: `back/src/test/resources/samples/test.webp`(체크인된 40x40 2색 WebP 픽스처, 쓰기 라이브러리가 없어 코드로 생성 불가해 로컬 `cwebp`로 1회 생성) 기반 콘텐츠 검증 테스트 추가
- ADR-0008 라이브러리 표에 TwelveMonkeys 반영, ADR-0004 상태에 GIF/WebP 관련 후속 조치 완료 표시
