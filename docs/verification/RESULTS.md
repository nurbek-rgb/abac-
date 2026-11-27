# Нақты тексеру нәтижелері — 2026-11-27

Орта: Linux x86_64, **Eclipse Temurin JDK 17.0.21.1+2**, Maven 3.9.9.
Java 17 bytecode: Maven compiler `release=17`; дайын JAR нақты Java 17-де орындалды.
Windows/macOS осы ортада орындалған жоқ; іске қосу командалары README-де бар.

## Негізгі нәтиже

| Нұсқа / тексеру | Тест | Өткен | Failed | Error | Skipped |
|---|---:|---:|---:|---:|---:|
| Бастапқы негізгі код + 14 бастапқы тест + 22 жаңа регрессия | 36 | 21 | 15 | 0 | 0 |
| Соның ішінде жаңа қауіпсіздік регрессиялары | 22 | 7 | 15 | 0 | 0 |
| Түзетілген толық жоба | **99** | **99** | **0** | **0** | **0** |

Түпнұсқа репозиторийде pom.xml және бірнеше модуль жоқ болғандықтан baseline тек
қолда бар core кластарынан құрастырылды. Бұл шектеу security-review.md ішінде түсіндірілген.
Бастапқы 7 тест класы (45 тест) финалдық пакетте сақталды; тек пакетке сәйкес каталогтарға көшірілді.
Жаңа 4 тест класы 54 тексеру қосты. Параметрленген әр іске қосу жеке тест ретінде саналады.

## Түзетілген нұсқа бойынша толық санақ

| Тест класы | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| kz.abac.AuditIntegrationTest | 6 | 0 | 0 | 0 |
| kz.abac.BatchEvaluatorTest | 5 | 0 | 0 | 0 |
| kz.abac.ConditionNodeTest | 8 | 0 | 0 | 0 |
| kz.abac.HospitalPoliciesTest | 9 | 0 | 0 | 0 |
| kz.abac.MissingAttributeSecurityTest | 22 | 0 | 0 | 0 |
| kz.abac.PolicyEngineTest | 6 | 0 | 0 | 0 |
| kz.abac.PolicyLoaderTest | 10 | 0 | 0 | 0 |
| kz.abac.StrictPolicyInputTest | 10 | 0 | 0 | 0 |
| kz.abac.ThreeValuedLogicTest | 16 | 0 | 0 | 0 |
| kz.abac.repo.InMemoryAuditRepositoryTest | 3 | 0 | 0 | 0 |
| kz.abac.repo.SqliteAuditRepositoryTest | 4 | 0 | 0 | 0 |

## Орындалған командалар

```bash
# Baseline scaffold: күтілетін сәтсіздік, 15 assertions fail.
mvn clean test
# Түзетілген жоба:
mvn clean verify
java -jar dist/abac-policy-engine-1.1.0-all.jar
java -jar dist/abac-policy-engine-1.1.0-all.jar --hospital
java -jar dist/abac-policy-engine-1.1.0-all.jar missing-file.json
```

Жұмыс ортасындағы Maven желілік proxy және Java CA truststore параметрлері тек
тәуелділіктерді жүктеуге қолданылды; проект pom.xml-іне енгізілмеді.
Алғашқы ортада толық JDK болмағандықтан Java 17 бөлек орнатылды; жоғарыдағы санақ
компиляциясы/тесті шынымен орындалған соңғы тексеруден алынған.

## Демо және интеграция

- Main: exit 0, device жоқ соңғы сценарий **DENY [P1]**.
- HospitalDemo: exit 0, жаңа дерекқорда **8 жазба, 3 PERMIT, 5 DENY**, H3 — 1.
- SQLite тікелей SELECT арқылы тексерілді, JSON-дағы 8 шешіммен сәйкес.
- Жоқ конфигурация файлы: exit **1**, түсінікті қате; ешқандай рұқсат берілмеді.
- Memory аудиті: 500 параллель жазба; бірегей ID тексерілді.
- SQLite аудиті: 200 параллель жазба, бірегей ID/requestId және reopen тексерілді.
- Нәтиже ретін тексеру: 200 аралас сұраныс, PERMIT/DENY күтілген ретімен.
- CSV quoting, Unicode, JSON null policyId, CRUD update/delete, SQL input binding тексерілді.
- UML PNG файлдары PlantUML арқылы жасалып, визуалды тексерілді.

Толық бастапқы нәтижелер: baseline-maven.log, fixed-maven.log, baseline/, fixed/.
Демо шығысы: main-demo.txt, hospital-demo.txt, invalid-config.txt.
Нақты демодан алынған SQLite/CSV/JSON көшірмелері: sample-output/.

## Қайта өндіру

Түзетілген жоба түбірінде `mvn clean verify` орындаңыз.
Baseline-ді бөлек тексеру үшін `baseline-source.zip` архивін басқа папкаға шығарып,
сол папкадағы pom.xml жанынан `mvn test` орындаңыз. Ол 15 қауіпсіздік тестін
әдейі құлатуы тиіс; оны дайын қауіпсіз жоба орнына пайдаланбаңыз.

Maven Shade бірнеше JAR ішіндегі ортақ LICENSE/MANIFEST/module-info ресурстары туралы
warning береді. Build SUCCESS; дайын classpath JAR демода орындалды. Бұл dependency
осалдықтарын толық тексеру немесе барлық платформаны тестілеу есебі емес.
