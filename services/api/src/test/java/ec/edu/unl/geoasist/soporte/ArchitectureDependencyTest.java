package ec.edu.unl.geoasist.soporte;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ArchitectureDependencyTest {
  @Test
  void domainModulesHaveNoDependencyCycles() throws Exception {
    Path root = Path.of("src/main/java/ec/edu/unl/geoasist");
    Map<String, Set<String>> graph = new TreeMap<>();
    var reference = Pattern.compile("ec[.]edu[.]unl[.]geoasist[.]([a-z]+)");
    try (var files = Files.walk(root)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file);
        var declaration =
            Pattern.compile("(?m)^package ec[.]edu[.]unl[.]geoasist[.]([a-z]+)").matcher(source);
        if (!declaration.find()) continue;
        String owner = declaration.group(1);
        var edges = graph.computeIfAbsent(owner, k -> new TreeSet<>());
        var refs = reference.matcher(source);
        while (refs.find()) if (!refs.group(1).equals(owner)) edges.add(refs.group(1));
        if (file.toString().contains("/dominio/")) {
          assertFalse(source.contains("jakarta.persistence"), file.toString());
          assertFalse(source.contains("org.springframework.web"), file.toString());
        }
      }
    }
    Set<String> finished = new HashSet<>();
    for (String node : graph.keySet()) visit(node, graph, new LinkedHashSet<>(), finished);
  }

  private static void visit(
      String node, Map<String, Set<String>> graph, Set<String> active, Set<String> finished) {
    if (finished.contains(node)) return;
    assertFalse(active.contains(node), "Dependencias cíclicas: " + active + " -> " + node);
    active.add(node);
    for (String next : graph.getOrDefault(node, Set.of())) visit(next, graph, active, finished);
    active.remove(node);
    finished.add(node);
  }
}
