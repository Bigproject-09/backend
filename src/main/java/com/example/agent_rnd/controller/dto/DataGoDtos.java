package com.example.agent_rnd.service.dto;

import java.util.List;

public class DataGoDtos {

    // 요청 바디: businesses 배열
    public record VerifyRequest(List<BusinessItem> businesses) {
        public record BusinessItem(String b_no, String start_dt, String p_nm) {}
    }

    // 응답: 형태가 꽤 다양한데 일단 "raw"로 받고 필요한 필드만 뽑는 방식이 안전
}
