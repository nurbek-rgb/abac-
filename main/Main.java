package kz.abac;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/** Консольдік демо. Іске қосу: mvn compile exec:java [-Dexec.args="policies.json"] */
public final class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--hospital")) {
            try { HospitalDemo.main(java.util.Arrays.copyOfRange(args, 1, args.length)); }
            catch (Exception e) { System.err.println("Қате: " + e.getMessage()); System.exit(1); }
            return;
        }
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        Path file = Path.of(args.length > 0 ? args[0] : "policies.json");

        List<Policy> policies;
        try {
            policies = PolicyLoader.load(file);
        } catch (PolicyException e) {
            out.println("Қате: " + e.getMessage());
            System.exit(1);
            return;
        }
        out.println(policies.size() + " саясат жүктелді: " + file.toAbsolutePath());
        out.println();

        PolicyEngine engine = new PolicyEngine(policies);

        run(out, engine, "1. Дәрігер, 10:00, корпоративтік құрылғы, оқу",
                AttributeContext.builder().role("doctor").time("10:00").device("corporate")
                        .location("office").action("read").build());

        run(out, engine, "2. Дәрігер, 20:00 (жұмыс уақытынан тыс)",
                AttributeContext.builder().role("doctor").time("20:00").device("corporate")
                        .location("office").action("read").build());

        run(out, engine, "3. Әкімші, 23:30, белгісіз орын (P2 PERMIT және P3 DENY қатар сәйкес — DENY басым)",
                AttributeContext.builder().role("admin").time("23:30").device("corporate")
                        .location("unknown").action("read").build());

        run(out, engine, "4. Әкімші, жеке құрылғы",
                AttributeContext.builder().role("admin").time("12:00").device("personal")
                        .location("home").action("read").build());

        run(out, engine, "5. Стажер, жою әрекеті",
                AttributeContext.builder().role("intern").time("11:00").device("corporate")
                        .location("office").action("delete").build());

        run(out, engine, "6. Дәрігер, құрылғы көрсетілмеген (атрибут жоқ)",
                AttributeContext.builder().role("doctor").time("10:00")
                        .location("office").action("read").build());
    }

    private static void run(PrintStream out, PolicyEngine engine, String title, AttributeContext ctx) {
        out.println(title);
        out.println("  Сұраныс: " + ctx);
        out.println("  Шешім:   " + engine.evaluate(ctx));
        out.println();
    }
}

