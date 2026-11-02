# Persistence

## jOOQ 코드 생성

Flyway 마이그레이션이 적용된 개발용 PostgreSQL에서 jOOQ 코드를 생성합니다.
현재 설정의 기본 접속 정보는 `application.yaml`의 로컬 DB 설정과 같습니다.
다른 DB를 사용하려면 다음 환경 변수를 지정하세요.

- `JOOQ_JDBC_URL` (기본값: `jdbc:postgresql://localhost:5432/vai`)
- `JOOQ_JDBC_USER` (기본값: `postgres`)
- `JOOQ_JDBC_PASSWORD` (기본값: `postgres`)

프로젝트 루트에서 다음 명령을 각각 실행합니다.

```sh
# Flyway 마이그레이션을 아직 적용하지 않았다면 실행
./gradlew :persistence:bootRun

# 마이그레이션 적용 후 코드 생성
./gradlew :persistence:jooqCodegen

# 생성된 코드를 포함하여 컴파일 및 테스트
./gradlew :persistence:test
```

`bootRun`은 Spring의 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD` 설정을 사용합니다. `JOOQ_JDBC_*` 값을 바꾼 경우
두 명령이 같은 개발용 DB를 가리키도록 Spring 설정도 맞춰주세요.
운영 DB가 아닌 코드 생성용 DB를 사용하세요. 코드 생성은 스키마를 읽기만 하며,
자동으로 Flyway 마이그레이션을 실행하지 않습니다.

생성 파일은 `modules/persistence/build/generated-src/jooq/main`에 저장되며
Git에 포함되지 않습니다. `clean` 실행 후에는 다시 생성해야 합니다.
