import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Hello {
    @GetMapping("/")
    public Map<String, String> hello() {
        Map<String, String> map = new HashMap<>();
        map.put("message", "Hello World");
        map.put("status", "success");
        map.put("code", "200");
        return map;
    }
}
