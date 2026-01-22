package com.example.agent_rnd.domain.notice;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_notices") // 1. 테이블명 소문자+복수형 일치 시킴
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProjectNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id") //  중요: DB가 snake_case이므로 명시 필수
    private Long noticeId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "link", columnDefinition = "TEXT")
    private String link;

    @Column(name = "seq")
    private String seq;

    @Column(name = "author")
    private String author;

    //  중요: DB가 CamelCase이므로, name을 명시하여 자동 변환 방지
    @Column(name = "excInsttNm")
    private String excInsttNm;

    @Lob // longtext 대응
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    //  DB 컬럼명 그대로 매핑
    @Column(name = "pubDate")
    private String pubDate;

    //  DB 컬럼명 그대로 매핑
    @Column(name = "reqstDt")
    private String reqstDt;

    @Column(name = "trgetNm", columnDefinition = "TEXT")
    private String trgetNm;

    @Column(name = "printFlpthNm", columnDefinition = "TEXT")
    private String printFlpthNm;

    @Column(name = "printFileNm", columnDefinition = "TEXT")
    private String printFileNm;

    @Column(name = "hashTags", columnDefinition = "TEXT")
    private String hashTags;
}