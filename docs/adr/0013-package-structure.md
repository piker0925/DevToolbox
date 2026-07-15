# ADR-0013 패키지 구조 — 기능별 레이어 분리

## 상태

확정

## 배경

초기 구현에서 `domain/job/`과 `domain/tool/` 두 패키지에 HTTP 컨트롤러, 응답 DTO, 도메인 모델, 서비스, 비동기 워커가 모두 평탄하게 혼재하였다.

`domain/job/`은 15개, `domain/tool/`은 8개였으나 9·10단계에서 ToolModule 구현체(PDF, 이미지, 보안 등) 33개 이상이 추가될 예정이어서 구조 정리가 필요했다.

검토 과정에서 다음 두 가지 핵심 문제가 드러났다:

1. **역할 혼재** — 같은 패키지에 `@Entity`, `@Repository`, `@Service`, `@RestController`, DTO가 공존해 탐색이 어렵다.
2. **`tool/`의 이중 역할** — `tool/`이 "도구 실행 플랫폼(ToolModule 인터페이스 + 라우팅)"과 "구현체 33개" 두 역할을 동시에 맡는다.

## 검토한 선택지

### 안 A — 현행 유지 + `dto/` 서브패키지만 추가

```
domain/job/
  dto/
  Job.java, JobController.java, JobService.java ...  ← 나머지 flat
domain/tool/
  dto/
  ToolModule.java, ToolController.java, PdfMerge.java ...  ← 40+ 파일 혼재
```

**탈락 이유** — DTO는 분리되나 컨트롤러·서비스·엔티티가 여전히 같은 공간에 있고, 9·10단계 이후 `domain/tool/`이 40개 이상 파일로 부풀어 파악이 불가능해진다.

---

### 안 B — 전체 레이어 분리 (최상위에 controller/, service/, repository/)

```
controller/
  JobController.java, ToolController.java ...
service/
  JobService.java, ToolService.java ...
repository/
  JobRepository.java ...
```

**탈락 이유** — Job 상태 변경 하나를 수정할 때 `controller/`, `service/`, `repository/` 세 디렉토리를 오가야 한다(샷건 서저리). 기능이 늘수록 각 레이어 폴더도
비대해진다.

---

### 안 C — `api/` + `domain/` + `module/` 3분할

```
api/job/, api/tool/    ← HTTP 레이어
domain/job/, domain/tool/  ← 비즈니스 레이어
module/pdf/, module/image/ ...  ← 구현체
```

**탈락 이유** — `api/`라는 추가 계층이 기능 탐색 시 분산을 유발한다. Job 관련 코드를 보려면 `api/job/`과 `domain/job/` 두 곳을 모두 봐야 한다. 기능별 응집보다 레이어별 분리를
우선시하는 구조가 된다.

---

### 안 D — 기능별 + 기능 내 레이어 분리 (채택)

```
job/
  entity/     ← @Entity, enum
  repository/ ← JpaRepository, DB 프로젝션
  service/    ← 비즈니스 로직 + 비동기 워커·스케줄러
  controller/ ← @RestController
  dto/        ← HTTP 응답 record

tool/
  model/      ← ToolModule 인터페이스, ToolInput, ToolResult, ToolProcessingException
  service/    ← ToolService (모듈 라우팅)
  controller/ ← ToolController
  dto/        ← ModuleResponse, RunResponse

module/
  pdf/, image/, security/, codegen/, generator/
  formatter/, converter/, text/, network/, devops/

global/
  exception/, storage/, config/, response/
```

## 결정

**안 D — 기능별 + 기능 내 레이어 분리**

## 이유

### 기능별 최상위 분리

`job/`과 `tool/`을 최상위 패키지로 두면 "이 기능 관련 코드는 전부 이 폴더 안에 있다"가 보장된다. 안 B처럼 레이어를 최상위에 두면 기능 하나를 파악하기 위해 여러 폴더를 순회해야 한다.

### 기능 내 레이어 분리

각 기능 패키지 안에서 레이어를 나누는 이유는 파일 역할이 명확히 달라서다:

| 레이어        | 어노테이션                    | 관심사         |
|------------|--------------------------|-------------|
| entity     | `@Entity`                | DB 매핑, 생명주기 |
| repository | `JpaRepository`          | 쿼리, 트랜잭션    |
| service    | `@Service`, `@Component` | 비즈니스 로직     |
| controller | `@RestController`        | HTTP 요청·응답  |
| dto        | `record`                 | 직렬화 형태      |

이 분리로 "어떤 역할의 파일을 찾는다면 어느 서브패키지에 있는지"를 즉시 알 수 있다.

### `tool/model/` 명명

`ToolModule`은 `@Entity`가 아닌 인터페이스이므로 `entity/`라는 이름이 부정확하다. `model/`은 도메인의 핵심 계약(인터페이스·값 객체)을 담는 표준에 가까운 이름이다.
`ToolInput`, `ToolResult`도 DB와 무관한 값 객체이므로 `model/`이 적합하다.

### `service/`에 워커·스케줄러 포함

`JobWorker`, `TtlCleanupScheduler`는 HTTP가 아닌 스케줄러가 트리거하는 컴포넌트다. 이를 위한 별도 레이어 이름(`worker/`)은 비표준이다. 두 클래스 모두 비즈니스 로직(Job
상태 전이, 만료 정리)을 수행하므로 `service/`에 포함한다.

### `module/`을 `tool/` 외부에 분리하는 이유

`tool/`은 "도구를 실행하는 플랫폼"이고, `module/`의 각 구현체는 그 플랫폼에 꽂히는 플러그인이다. 두 역할을 같은 패키지에 두면 플랫폼 코드(인터페이스·라우팅, ~6개 파일)와 구현체(33개 이상)가
섞인다.

각 모듈 구현체는 `ToolModule` 인터페이스를 구현하는 **파일 1개**이며, 별도의 컨트롤러·서비스·레포지토리를 가지지 않는다. 모든 도구는 공유 API(
`POST /api/v1/tools/{moduleId}/run`, `POST /api/v1/tools/{moduleId}/upload`)를 사용하고 `{moduleId}`로만 라우팅이 달라진다. 따라서 구현체는
독립적으로 `module/{category}/`에 격리된다.

새 모듈 추가 시 `module/{category}/`에 파일 1개만 추가하면 되며, `tool/`이나 `job/`은 변경하지 않는다.

### 의존 방향

```
tool/controller → tool/service → tool/model
tool/controller → job/service, job/dto   (업로드 시 Job 생성)
job/service     → tool/model             (Worker가 ToolModule 호출)
module/*        → tool/model             (ToolModule 인터페이스 구현)
global/*        → (없음)
```

역방향 의존(`tool/model → job/*`, `global → job/*` 등)은 금지한다.

## 결과

### 최종 파일 분포

```
job/
  entity/     Job.java, JobStatus.java                               (2)
  repository/ JobRepository.java, BatchStats.java                   (2)
  service/    JobService.java, JobWorker.java, TtlCleanupScheduler  (3)
  controller/ JobController.java, BatchController.java, FileController (3)
  dto/        JobCreateResponse, JobStatusResponse, JobResultResponse,
              BatchCreateResponse, BatchProgressResponse             (5)

tool/
  model/      ToolModule.java, ToolInput.java, ToolResult.java,
              ToolProcessingException.java                           (4)
  service/    ToolService.java                                       (1)
  controller/ ToolController.java                                    (1)
  dto/        ModuleResponse.java, RunResponse.java                  (2)

module/       각 구현체 파일 1개씩, 카테고리별 서브패키지           (33+)

global/       exception(4), storage(2), config(3), response(1)      (10)
```

### 운영 규칙

- `service/`는 도메인 객체(`Job`, `ToolModule`)만 반환한다. DTO 변환은 `controller/` 책임.
- `module/`의 구현체는 `tool/model/`의 인터페이스만 의존한다. `job/`이나 `global/storage/`를 직접 참조하지 않는다.
- 레이어 내 파일이 10개를 초과하면 세분화를 검토한다.
- `module/{category}/`에 공유 유틸이 생기면 `module/{category}/support/`로 분리한다.

## 실제 구현과의 차이 (2026-07)

핵심 결정(기능별 최상위 분리 + 기능 내 `entity/repository/service/controller/dto` 레이어 분리)은 그대로 적용됐으나, 두 지점이 계획과 다르다:

1. **모듈 구현체 위치** — 계획은 최상위 `module/{category}/`였으나, 실제로는 도구 플랫폼과 같은 패키지 아래 `tool/{category}/`(예: `tool/pdf/`, `tool/util/`)에 배치됐다. `tool/`은 `model/·service/·controller/·dto/`(플랫폼)와 `{category}/`(구현체)를 함께 담는다.
2. **추가 기능 패키지** — 11·12단계에서 `comment/`, `stats/`, `suggestion/`, `admin/`이 같은 기능별-레이어 패턴으로 추가됐다(`admin/`은 컨트롤러 1개라 평면).
3. **`text/` 카테고리 소멸** — 계획 예시에 있던 `tool/text/`(CaseConverter·Diff·Regex)는 전부 프론트로 이전되어 백엔드에 남아있지 않다(ADR-0020).

`global/`에는 `config/·exception/·response/·storage/`에 더해 `util/`(공통 유틸)이 있다.
