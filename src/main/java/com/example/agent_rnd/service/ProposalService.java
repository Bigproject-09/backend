package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.notice.ProjectNotice;
import com.example.agent_rnd.domain.proposal.Proposal;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.dto.ProposalRequest;
import com.example.agent_rnd.repository.ProjectNoticeRepository;
import com.example.agent_rnd.repository.ProposalRepository;
import com.example.agent_rnd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ProjectNoticeRepository projectNoticeRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    // 파이썬 서버 주소 (실서비스는 application.yml로 빼는게 맞음)
    private static final String PYTHON_SERVER_URL = "http://localhost:5000/parse";

    @Transactional
    public Long processProposalFile(MultipartFile file, ProposalRequest request) {

        // 1) 파일 확장자 검사 (DOCX, PDF만 허용)
        validateFileExtension(file);

        // 2) 파이썬 서버로 파일 전송 -> JSON 결과 받기
        String parsedJson = sendFileToPythonServer(file);

        // 3) FK 엔티티 조회 (notice_id / user_id)
        ProjectNotice notice = projectNoticeRepository.findById(request.getNoticeId())
                .orElseThrow(() -> new IllegalArgumentException("공고(notice_id)가 없습니다. id=" + request.getNoticeId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자(user_id)가 없습니다. id=" + request.getUserId()));

        // 4) 결과 DB 저장 (Proposal은 notice/user 연관관계로 저장)
        Proposal proposal = Proposal.builder()
                .notice(notice)
                .user(user)
                .title(request.getTitle())
                .fileName(file.getOriginalFilename())
                .parsedJson(parsedJson)
                .build();

        Proposal saved = proposalRepository.save(proposal);

        // ✅ PK getter 이름은 엔티티에 맞춰야 함 (대부분 getProposalId())
        return saved.getProposalId();
    }

    private void validateFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("파일 이름이 유효하지 않습니다.");
        }

        String extension = StringUtils.getFilenameExtension(filename);
        if (extension == null) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }

        List<String> allowedExtensions = Arrays.asList("pdf", "docx", "doc");
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (pdf, docx만 가능)");
        }
    }

    private String sendFileToPythonServer(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    PYTHON_SERVER_URL,
                    requestEntity,
                    String.class
            );

            return response.getBody();

        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패", e);
        } catch (Exception e) {
            System.out.println("파이썬 서버 연결 실패. 더미 데이터를 저장합니다.");
            return "{\"summary\": \"파이썬 서버 연결 실패 - 테스트용 더미 JSON\", \"pages\": []}";
        }
    }

    public Proposal getProposal(Long proposalId) {
        return proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("제안서가 없습니다."));
    }
}
