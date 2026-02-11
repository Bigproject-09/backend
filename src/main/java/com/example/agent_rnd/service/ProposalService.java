package com.example.agent_rnd.service;

import com.example.agent_rnd.domain.notice.ProjectNotice;
import com.example.agent_rnd.domain.proposal.Proposal;
import com.example.agent_rnd.domain.user.User;
import com.example.agent_rnd.dto.ProposalRequest;
import com.example.agent_rnd.repository.ProjectNoticeRepository;
import com.example.agent_rnd.repository.ProposalRepository;
import com.example.agent_rnd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    @Transactional
    public Long processProposalFile(MultipartFile file, ProposalRequest request) {
        validateFileExtension(file);

        String parsedJson = sendFileToPythonServer(file);

        ProjectNotice notice = projectNoticeRepository.findById(request.getNoticeId())
                .orElseThrow(() -> new IllegalArgumentException("notice_id not found: " + request.getNoticeId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("user_id not found: " + request.getUserId()));

        Proposal proposal = Proposal.builder()
                .notice(notice)
                .user(user)
                .title(request.getTitle())
                .fileName(file.getOriginalFilename())
                .parsedJson(parsedJson)
                .build();

        Proposal saved = proposalRepository.save(proposal);
        return saved.getProposalId();
    }

    private void validateFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = StringUtils.getFilenameExtension(filename);
        if (extension == null) {
            throw new IllegalArgumentException("Missing extension");
        }

        List<String> allowedExtensions = Arrays.asList("pdf", "docx", "doc");
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file format (pdf, docx, doc)");
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
                    fastApiBaseUrl + "/parse",
                    requestEntity,
                    String.class
            );

            return response.getBody();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        } catch (Exception e) {
            System.out.println("FastAPI parse call failed. Returning fallback payload.");
            return "{\"summary\": \"FastAPI parse call failed\", \"pages\": []}";
        }
    }

    public Proposal getProposal(Long proposalId) {
        return proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found"));
    }
}
