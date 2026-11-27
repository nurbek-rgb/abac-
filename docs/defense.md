# Қорғауға арналған қысқа сценарий

«Менің жеке жобам — ABAC Policy Engine. Ол қолжетімділікті рөл, құрылғы, уақыт,
бөлім және әрекет атрибуттарына қарап шешеді. Негізгі мақсат — ережелерді Java кодын
қайта жазбай JSON арқылы өзгерту және әр шешімнің себебін көрсету».

1. **Main.java**: саясат файлын алады, PolicyLoader арқылы оқиды, сценарийлерді іске қосады.
2. **PolicyLoader**: JSON-ды тексеріп, RuleNode интерфейсімен жұмыс істейтін ағаш жасайды.
3. **ConditionNode, AndNode, OrNode, NotNode**: Composite және полиморфизм. Өрістер private final.
4. **MatchResult**: TRUE/FALSE/INDETERMINATE. Бұрын missing=false, NOT=false→true болатын.
   Қазір NOT(INDETERMINATE) өзгермейді, рұқсатқа дәлел бола алмайды.
5. **PolicyEngine**: нақты DENY басым; анықталмаған DENY да рұқсатты бөгейді.
6. **HospitalDemo**: JSON сұраныстары төрт ағынмен бағаланады. Нәтиже реті сақталады.
7. **Repository<T>**: memory және SQLite іске асырулары бір интерфейспен қолданылады.
   SQLite JDBC және PreparedStatement арқылы CRUD орындайды.
8. **AuditStats/AuditExporter**: Stream API есептері, CSV/JSON файлдары.
9. **JUnit**: missing NOT, қабаттасу, deny-overrides, файл/ДҚ/көпағындылық тесттері.

Демо командалары:

```bash
java -jar dist/abac-policy-engine-1.1.0-all.jar
java -jar dist/abac-policy-engine-1.1.0-all.jar --hospital
mvn test
```

Main ішіндегі соңғы сұраныста device жоқ екенін және DENY шыққанын көрсетіңіз.
HospitalDemo-дан кейін output/audit.json мен audit.csv ашыңыз. SQLite файлында
audit_records кестесі пайда болады; келесі іске қосуда жазбалар сақталады.
Тест саны мен нәтижесін docs/verification/RESULTS.md арқылы түсіндіріңіз.

«Бұл оқу моделі. Аутентификацияны және шұғыл жағдайдың шын екенін сыртқы жүйе растауы
керек. Қазіргі жобада консоль, саясат қозғалтқышы, аудит және тесттер іске асырылған».
