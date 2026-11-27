package kz.abac;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import kz.abac.audit.*;
import kz.abac.repo.SqliteAuditRepository;

/** Real JSON -> concurrent Java evaluation -> SQLite -> Stream statistics -> CSV/JSON. */
public final class HospitalDemo {
    private HospitalDemo() {}
    public static void main(String[] args) throws java.io.IOException {
        Path policies=Path.of(args.length>0?args[0]:"policies-hospital.json");
        Path requests=Path.of(args.length>1?args[1]:"data/requests-hospital.json");
        Path output=Path.of(args.length>2?args[2]:"output");
        Files.createDirectories(output);
        PolicyEngine engine=new PolicyEngine(PolicyLoader.load(policies));
        List<AttributeContext> contexts=RequestLoader.load(requests);
        try(var repository=new SqliteAuditRepository("jdbc:sqlite:"+output.resolve("audit.db").toAbsolutePath())) {
            var audited=new AuditedEngine(engine,repository);
            try(var batch=new BatchEvaluator(audited::evaluate,4)) {
                List<Decision> decisions=batch.evaluateAll(contexts);
                for(int i=0;i<decisions.size();i++) System.out.println((i+1)+". "+decisions.get(i));
            }
            var records=repository.findAll();
            System.out.println("Stored audit totals (includes previous runs): "+AuditStats.from(records));
            AuditExporter.toCsv(records,output.resolve("audit.csv"));
            AuditExporter.toJson(records,output.resolve("audit.json"));
            System.out.println("SQLite, CSV, JSON: "+output.toAbsolutePath());
        }
    }
}
