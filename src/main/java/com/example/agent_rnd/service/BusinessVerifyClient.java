package com.example.agent_rnd.service;

import com.example.agent_rnd.config.ExternalDataGoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BusinessVerifyClient {

    private final ExternalDataGoProperties props;

    private final RestClient restClient = RestClient.builder().build();

    // ====== 요청/응답 DTO ======
    public record ValidationApiRequest(List<BusinessDescription> businesses) {}
    public record BusinessDescription(
            String b_no,      // 필수
            String start_dt,  // 필수 (YYYYMMDD)
            String p_nm,      // 필수
            String p_nm2,     // optional
            String b_nm,      // optional
            String corp_no,   // optional
            String b_sector,  // optional
            String b_type,    // optional
            String b_adr      // optional
    ) {}

    public record ValidationApiResponse(
            String status_code,
            Integer request_cnt,
            Integer valid_cnt,
            List<BusinessValidation> data
    ) {}

    public record BusinessValidation(
            String b_no,
            String valid,      // "01" valid, "02" invalid
            String valid_msg
    ) {}

    // ====== 진위확인 ======
    public ValidationApiResponse validate(String businessRegNo10, String openDateYYYYMMDD, String ceoName) {
        // 문서상: POST /validate, serviceKey는 querystring, body는 businesses[]  :contentReference[oaicite:2]{index=2}
        String url = props.baseUrl()
                + "/validate"
                + "?serviceKey=" + props.serviceKey()
                + "&returnType=JSON";

        ValidationApiRequest body = new ValidationApiRequest(
                List.of(new BusinessDescription(
                        businessRegNo10,
                        openDateYYYYMMDD,
                        ceoName,
                        "", "", "", "", "", ""
                ))
        );

        return restClient.post()
                .uri(URI.create(url))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ValidationApiResponse.class);
    }
}
