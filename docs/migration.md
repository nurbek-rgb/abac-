# Ескі құрылымнан көшу

1. Алдымен бар жобаңыздың көшірмесін сақтаңыз. ZIP-ті бөлек папкаға шығарыңыз.
2. IntelliJ-де ZIP-тен шыққан pom.xml жобасын ашып, тест пен демоны орындаңыз.
3. Ескі репозиторий түбіріндегі *.java файлдары Maven-де src/main/java және src/test/java
   ішіне көшірілді. Екі нұсқаны қатар source root деп белгілемеңіз — duplicate class шығады.
4. Бастапқы тесттер логикасы өзгертілген жоқ. Репозиторийде болмаған модульдер/JSON қосылды.
5. RuleNode, MatchResult, ConditionNode, AndNode, OrNode, NotNode, Policy, Effect,
   PolicyEngine файлдарын бірге жаңартыңыз. Тек NotNode.java-ны ауыстыру жеткіліксіз.
6. RuleNode-ді өзіңіз іске асырған класс болса, boolean evaluate орнына MatchResult
   evaluateResult іске асырыңыз. Жоқ/жарамсыз дерекке INDETERMINATE беріңіз.
7. Default working directory — pom.xml тұрған папка. JSON жолдары соған қатысты.
8. Өз саясатыңыздағы U DENY енді рұқсатты бұғаттайтынын ескеріп, контекстті толық беріңіз.
9. Дайын JAR ZIP-те dist ішінде. Код өзгерген соң mvn clean package арқылы жаңа JAR жинаңыз;
   run.sh/run.bat ескі dist JAR-ды пайдаланбас үшін target/*-all.jar-ды dist-ке көшіріңіз.

ZIP Git тарихын қамтымайды. GitHub-қа жүктеу, repository атауын өзгерту, PR ашу орындалған жоқ.
