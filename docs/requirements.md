# Фото талаптары және қабылдау өлшемдері

Фотода оң жақ баған толық көрінбейді. Көрінген атаулар: ООП; Коллекциялар және Generics;
Stream API; Деректер қоры (SQLite + JDBC); Файлдар; Көпағындылық; Тестілеу (Maven,
кемінде 3); Командалық жұмыс. Пайдаланушының нұсқауына сай соңғысы **жеке/соло жұмыс**.

| ID | Қабылдау өлшемі | Код/дәлел |
|---|---|---|
| R1 | NOT missing/blank/invalid деректен рұқсат жасамайды | MissingAttributeSecurityTest, ThreeValuedLogicTest |
| R2 | Толық дерекпен ескі операторлар жұмыс істейді | ConditionNodeTest, PolicyEngineTest |
| R3 | DENY басым; ықтимал DENY жоқта дәлелденген PERMIT рұқсат береді | PolicyEngine, ThreeValuedLogicTest |
| R4 | JSON оқу және қате конфигурацияны қабылдамау | PolicyLoaderTest, StrictPolicyInputTest |
| R5 | ООП: интерфейс, мұрагерлік, инкапсуляция, полиморфизм | RuleNode implementations; PolicyException/AuditException extends RuntimeException; Repository<T> implementations |
| R6 | List, Set, Map және generic repository іс жүзінде қолданылады | PolicyEngine, AttributeContext, PolicyLoader, AuditStats, Repository<T> |
| R7 | Stream API есеп/сүзу/топтастыруға қолданылады | AuditStats, findWhere, AuditExporter |
| R8 | SQLite + JDBC, CRUD және қайта іске қосқанда сақтау | SqliteAuditRepositoryTest, AuditIntegrationTest |
| R9 | Нақты CSV және JSON файлдары жазылады | AuditExporter, HospitalDemo, AuditIntegrationTest |
| R10 | Бірнеше worker, рет сақталуы, thread-safe аудит | BatchEvaluatorTest (200/500 сұраныс), AuditIntegrationTest (200 SQLite жазба) |
| R11 | Maven стандарт құрылымы және кемінде 3 JUnit тест | pom.xml; docs/verification/RESULTS.md |
| R12 | Research + Use Case/Class/Sequence + ДҚ схема | docs/research.md; docs/uml; docs/db |
| R13 | Жеке жұмыс кезеңдері, README, қорғау демосы | docs/solo-work.md; docs/defense.md |

Кесілген мәтінге қатысты жорамалдар: ДҚ жолындағы жалғасы CRUD болуы ықтимал болғандықтан
төрт операцияның бәрі іске асырылды; файлдар жолы CSV не JSON болуы ықтимал болғандықтан
екеуі де жасалды; көпағындылық жолындағы көрінбейтін шартты нақты API деп болжаған жоқпыз,
осы доменге орынды batch бағалау және синхрондалған аудит іске асырылды.

Фото астындағы кезеңдер бөлек орындалды: зерттеу/талаптар → UML/ДҚ → MVP → тесттер/README → демо.
Бұрыннан уақытқа бөлініп жасалған commit тарихы немесе ашылған PR ойдан жасалмады.
Репозиторийді `java-project-<тегі>-<нұсқа>` деп атау бөлек тапсырылса, тегі/нұсқасы нақты
болған соң автор өзгерте алады. Бұл пакет abac- репозиторийінің атауын өзгертпейді.
