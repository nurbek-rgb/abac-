package kz.abac.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AuditExporter {
    private static final ObjectMapper JSON=new ObjectMapper();
    private AuditExporter() {}
    public static void toCsv(List<AuditRecord> records, Path file) {
        StringBuilder csv=new StringBuilder("id,timestamp,role,action,effect,policyId,reason\r\n");
        for(AuditRecord r:records) csv.append(List.of(String.valueOf(r.id()),r.timestamp().toString(),r.role(),r.action(),
                r.effect().name(),r.policyId()==null?"":r.policyId(),r.reason()).stream().map(AuditExporter::cell).collect(Collectors.joining(","))).append("\r\n");
        write(file,csv.toString());
    }
    private static String cell(String value) {
        // RFC 4180 quoting plus protection against spreadsheet formula execution.
        String stripped=value.stripLeading();
        if(!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0))>=0) value="'"+value;
        return "\""+value.replace("\"","\"\"")+"\"";
    }
    public static void toJson(List<AuditRecord> records, Path file) {
        List<Map<String,Object>> rows=records.stream().map(r->{
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("id",r.id());row.put("timestamp",r.timestamp().toString());row.put("attributes",r.attributes());
            row.put("effect",r.effect().name());row.put("policyId",r.policyId());row.put("reason",r.reason());return row;
        }).toList();
        try { write(file,JSON.writerWithDefaultPrettyPrinter().writeValueAsString(rows)); }
        catch(IOException e) { throw new AuditException("Cannot serialize audit report",e); }
    }
    private static void write(Path file,String content) {
        try {
            Path parent=file.toAbsolutePath().getParent(); Files.createDirectories(parent);
            Files.writeString(file,content,StandardCharsets.UTF_8);
        } catch(IOException e) { throw new AuditException("Cannot write report: "+file,e); }
    }
}
