package com.example.agent_rnd.controller;

import com.example.agent_rnd.client.BusinessVerifyClient;
import com.example.agent_rnd.domain.enums.UserEntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev")
public class DevBusinessController {

    private final BusinessVerifyClient businessVerifyClient;

    @PostMapping("/business/validate")
    public ResponseEntity<?> validate(@RequestBody Map<String, String> req) {
        String bno = req.get("bno");
        String startDt = req.get("startDt");
        String pnm = req.get("pnm");

        var r = businessVerifyClient.validate(bno, startDt, pnm);

        if (r == null || r.data() == null || r.data().isEmpty()) {
            return ResponseEntity.ok(Map.of("ok", false, "reason", "empty response"));
        }

        var d = r.data().get(0);
        var st = d.status(); // ✅ tax_type은 여기 안에 있음

        String taxType = (st == null) ? null : st.tax_type();
        String taxTypeCd = (st == null) ? null : st.tax_type_cd();

        UserEntityType type = UserEntityType.fromTaxTypeCd(taxTypeCd);

        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        res.put("tax_type", taxType);
        res.put("tax_type_cd", taxTypeCd);
        res.put("user_entity_type", type.name());
        res.put("raw_valid", d.valid());
        res.put("valid_msg", d.valid_msg());
        return ResponseEntity.ok(res);
    }
}
