//package com.example.agent_rnd.client;
//
//import com.example.agent_rnd.config.ExternalDataGoProperties;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestClient;
//
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class BusinessVerifyClient {
//
//    private final ExternalDataGoProperties props;
//    private final ObjectMapper objectMapper;
//
//    private RestClient restClient() {
//        return RestClient.builder()
//                .baseUrl(props.getBaseUrl()) // ex) https://api.odcloud.kr/api/nts-businessman/v1
//                .build();
//    }
//
//    /** 진위확인 (validate) */
//    public ValidationApiResponse validate(String bno, String startDt, String pnm) {
//        ValidationApiRequest req = new ValidationApiRequest(
//                List.of(new BusinessDescription(bno, startDt, pnm))
//        );
//
//        String raw = restClient().post()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/validate")
//                        .queryParam("serviceKey", props.getServiceKey())
//                        .build())
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .body(req)
//                .retrieve()
//                .body(String.class);
//
//        try {
//            return objectMapper.readValue(raw, ValidationApiResponse.class);
//        } catch (Exception e) {
//            throw new IllegalStateException("사업자 진위확인(validate) 응답 파싱 실패: " + e.getMessage());
//        }
//    }
//
//    /** 상태조회 (status) - validate 응답에 status가 없을 때 fallback */
//    public StatusApiResponse status(String bno) {
//        StatusApiRequest req = new StatusApiRequest(List.of(bno));
//
//        String raw = restClient().post()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/status")
//                        .queryParam("serviceKey", props.getServiceKey())
//                        .build())
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .body(req)
//                .retrieve()
//                .body(String.class);
//
//        try {
//            return objectMapper.readValue(raw, StatusApiResponse.class);
//        } catch (Exception e) {
//            throw new IllegalStateException("사업자 상태조회(status) 응답 파싱 실패: " + e.getMessage());
//        }
//    }
//
//    // ====== DTOs ======
//    public record ValidationApiRequest(List<BusinessDescription> businesses) {}
//
//    public record BusinessDescription(
//            String b_no,
//            String start_dt,
//            String p_nm
//    ) {}
//
//    public record ValidationApiResponse(
//            String status_code,
//            List<BusinessValidation> data
//    ) {}
//
//    public record BusinessValidation(
//            String b_no,
//            String valid,
//            String valid_msg,
//            Status status
//    ) {}
//
//    /** validate 응답에 종종 포함되는 status */
//    public record Status(
//            String b_stt,
//            String b_stt_cd,
//            String tax_type,
//            String tax_type_cd
//    ) {}
//
//    public record StatusApiRequest(List<String> b_no) {}
//
//    public record StatusApiResponse(
//            String status_code,
//            List<StatusItem> data
//    ) {}
//
//    public record StatusItem(
//            String b_no,
//            String b_stt,
//            String b_stt_cd,
//            String tax_type,
//            String tax_type_cd
//    ) {}
//}
package com.example.agent_rnd.client;

import com.example.agent_rnd.config.ExternalDataGoProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BusinessVerifyClient {

    private final ExternalDataGoProperties props;
    private final ObjectMapper objectMapper;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(props.getBaseUrl()) // https://api.odcloud.kr/api/nts-businessman/v1
                .build();
    }

    /** 진위확인 (validate) - 단건 편의 */
    public ValidationApiResponse validate(String bno, String startDt, String pnm) {
        return validate(List.of(new BusinessDescription(
                bno, startDt, pnm,
                null, null, null, null, null, null
        )));
    }

    /** 진위확인 (validate) - 확장: optional 필드까지 포함(스웨거 스펙 전체) */
    public ValidationApiResponse validate(BusinessDescription business) {
        return validate(List.of(business));
    }

    /** 진위확인 (validate) - 최대 100개 */
    public ValidationApiResponse validate(List<BusinessDescription> businesses) {
        ValidationApiRequest req = new ValidationApiRequest(businesses);

        String raw = restClient().post()
                .uri(uriBuilder -> uriBuilder
                        .path("/validate")
                        .queryParam("serviceKey", props.getServiceKey())
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readValue(raw, ValidationApiResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("사업자 진위확인(validate) 응답 파싱 실패: " + e.getMessage());
        }
    }

    /** 상태조회 (status) - 단건 편의 */
    public StatusApiResponse status(String bno) {
        return status(List.of(bno));
    }

    /** 상태조회 (status) - 최대 100개 */
    public StatusApiResponse status(List<String> bnos) {
        StatusApiRequest req = new StatusApiRequest(bnos);

        String raw = restClient().post()
                .uri(uriBuilder -> uriBuilder
                        .path("/status")
                        .queryParam("serviceKey", props.getServiceKey())
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readValue(raw, StatusApiResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("사업자 상태조회(status) 응답 파싱 실패: " + e.getMessage());
        }
    }

    // ====== DTOs (Swagger 1.1.0 기준 전체 필드) ======

    public record ValidationApiRequest(List<BusinessDescription> businesses) {}

    /** validate request body: 필수 + optional */
    public record BusinessDescription(
            String b_no,      // 필수
            String start_dt,  // 필수 (YYYYMMDD)
            String p_nm,      // 필수
            String p_nm2,     // optional
            String b_nm,      // optional (상호)
            String corp_no,   // optional (법인등록번호)
            String b_sector,  // optional (주업태명)
            String b_type,    // optional (주종목명)
            String b_adr      // optional (사업장주소)
    ) {}

    /** validate response: 카운트 + data */
    public record ValidationApiResponse(
            String status_code,
            Integer request_cnt,
            Integer valid_cnt,
            List<BusinessValidation> data
    ) {}

    public record BusinessValidation(
            String b_no,
            String valid,
            String valid_msg,
            BusinessDescription request_param, // 요청 파라미터 echo
            BusinessStatus status               // validate에서 종종 함께 내려옴
    ) {}

    /** status response */
    public record StatusApiRequest(List<String> b_no) {}

    public record StatusApiResponse(
            String status_code,
            Integer match_cnt,
            Integer request_cnt,
            List<BusinessStatus> data
    ) {}

    /** 사업자 상태조회 결과(스웨거의 BusinessStatus 전체 필드) */
    public record BusinessStatus(
            String b_no,
            String b_stt,
            String b_stt_cd,
            String tax_type,
            String tax_type_cd,
            String end_dt,
            String utcc_yn,
            String tax_type_change_dt,
            String invoice_apply_dt,
            String rbf_tax_type,
            String rbf_tax_type_cd
    ) {}
}
