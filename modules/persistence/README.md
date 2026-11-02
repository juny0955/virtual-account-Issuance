# Persistence

이 모듈은 DB 공통 설정, Flyway 마이그레이션과 jOOQ 생성 코드를 제공하는 라이브러리입니다.

## 실행 모듈에서 사용

```kotlin
implementation(project(":persistence"))
```

실행 모듈의 `application.yaml`에서 공통 설정을 가져옵니다.

```yaml
spring:
  config:
    import: classpath:persistence.yaml
```

공통 설정은 로컬 PostgreSQL의 `localhost:5432/vai`에 `postgres` 계정으로 접속합니다.
비밀번호도 로컬 설정에 고정되어 있으며, Flyway는 실행 모듈 시작 시 자동 실행됩니다.

## Flyway와 jOOQ 코드 생성

빈 개발용 PostgreSQL을 준비한 뒤 다음 환경 변수로 접속 대상을 지정합니다.
두 작업은 같은 접속 정보를 사용합니다.

- `JOOQ_JDBC_URL` (기본값: `jdbc:postgresql://localhost:5432/vai`)
- `JOOQ_JDBC_USER` (기본값: `postgres`)
- `JOOQ_JDBC_PASSWORD` (기본값: `postgres`)

프로젝트 루트에서 다음 명령을 순서대로 **별도 실행**합니다.

```sh
./gradlew :persistence:flywayMigrate
./gradlew :persistence:jooqCodegen
./gradlew :persistence:build
```

`flywayMigrate`는 지정한 DB를 변경합니다. 개발용 DB를 사용하고,
`jooqCodegen` 전에 마이그레이션을 적용하세요. 생성 코드는
`modules/persistence/build/generated-src/jooq/main`에 저장되어 Git에 포함되지
않습니다. `clean` 후와 스키마 변경 후에는 위 순서대로 다시 생성해야 합니다.
생성 파일이 없으면 컴파일이 안내 메시지와 함께 실패합니다.
실행 모듈의 DB 설정과 코드 생성용 DB(`JOOQ_JDBC_*`)는 별도입니다.
