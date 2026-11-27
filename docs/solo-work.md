# Жеке жұмыс жоспары және GitHub рәсімі

Формат: **соло**. Бір автор зерттеу, модельдеу, код, тест, құжат және қорғауға жауап береді.
Командалық мүшелер/бөлінген жалған рөлдер көрсетілмейді.

| Кезең | Нәтиже |
|---|---|
| 1 | research.md, requirements.md |
| 2 | UML use-case/class/sequence, SQLite схема |
| 3 | JSON, RuleNode, deny-overrides, консоль демо |
| 4 | NOT қауіпсіздігі, аудит/CRUD/Stream/batch, JUnit, README |
| 5 | defense.md сценарийімен жеке қорғау |

Пакетке нақты репозиторийде жасалмаған commit/branch/PR тарихы қосылған жоқ.
GitHub-қа жариялау пайдаланушыға қалдырылды. Жүктегенде түбірдегі ескі Java көшірмелерін
жаңа Maven құрылымымен ауыстыру керек (migration.md).

Өз компьютеріңіздегі repository үшін ұсынылатын, **бұл сессияда орындалмаған** әрекеттер:

```bash
git switch -c fix/not-missing-attributes
# Пакет файлдарын көшіріп, қайталанған ескі Java файлдарын тексеріп алып тастаңыз.
mvn clean verify
git add src pom.xml policies.json policies-hospital.json data docs README.md .gitignore run.sh run.bat
git diff --cached
git commit -m "Fix missing-attribute negation and complete tested ABAC project"
git push -u origin fix/not-missing-attributes
```

Содан соң GitHub-та PR ашып, тест есебі мен нақты өзгерістерді сипаттауға болады.
Бір студент өз PR-ін қарап, кейін біріктіреді. Бұрынғы күндермен commit жасауға немесе
бір сессиядағы жұмысты бірнеше апта орындалды деп көрсетуге болмайды.

PR сипаттамасының үлгісі:

> Missing attributes previously became true under NOT and could authorize a PERMIT policy.
> Preserve INDETERMINATE through nested rules, block unresolved DENY policies, and retain
> valid boolean cases. Complete Maven structure, hospital sample data, SQLite audit CRUD,
> concurrent batch evaluation and CSV/JSON reports. Validation: see docs/verification/RESULTS.md.
