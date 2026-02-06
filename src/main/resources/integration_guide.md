# 🔗 백엔드-프론트엔드 통합 연동 가이드

## 📋 목차
1. [전체 구조](#전체-구조)
2. [백엔드 API 엔드포인트 추가](#백엔드-api-엔드포인트-추가)
3. [프론트엔드 연동 코드](#프론트엔드-연동-코드)
4. [파일 처리 흐름](#파일-처리-흐름)
5. [실행 순서](#실행-순서)

---

## 🏗️ 전체 구조

```
프론트엔드 (React)
    ↓ HTTP Request
Spring Boot (Java)
    ↓ HTTP Request (필요시)
FastAPI (Python) - 모델링 코드 실행
    ↓
결과 반환
```

### 4가지 주요 기능

| 순서 | 기능 | 프론트엔드 | Spring Boot | FastAPI (Python) |
|------|------|-----------|------------|------------------|
| 1 | 공고문 분석 | ProcessPage → Analysis | `/api/notices/{id}/analyze` | `rfp_analysis_checklist` |
| 2 | 유관 RFP 검색 | ProcessPage → RFP | `/api/notices/{id}/search-rfp` | `rnd_search` |
| 3 | PPT 제작 | ProcessPage → Announce | `/api/notices/{id}/generate-ppt` | `ppt_maker` |
| 4 | 스크립트 생성 | ProcessPage → Script | `/api/notices/{id}/generate-script` | `ppt_script` |

---

## 🔧 백엔드 API 엔드포인트 추가

### Step 1: FastApiClient 확장

```java
// src/main/java/com/example/agent_rnd/client/FastApiClient.java

package com.example.agent_rnd.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final WebClient webClient = WebClient.builder().build();

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;  // http://localhost:8000

    /**
     * [Step 1] 공고문 분석 (체크리스트 + 심층분석)
     */
    public Map<String, Object> analyzeNotice(Long noticeId, Long companyId) {
        try {
            return webClient.post()
                    .uri(fastApiBaseUrl + "/api/analyze/step1")
                    .bodyValue(Map.of(
                        "notice_id", noticeId,
                        "company_id", companyId != null ? companyId : 1
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("FastAPI 공고문 분석 실패", e);
        }
    }

    /**
     * [Step 2] 유관 RFP 검색
     */
    public Map<String, Object> searchSimilarRfp(Long noticeId) {
        try {
            return webClient.post()
                    .uri(fastApiBaseUrl + "/api/analyze/step2")
                    .bodyValue(Map.of("notice_id", noticeId))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("FastAPI RFP 검색 실패", e);
        }
    }

    /**
     * [Step 3] PPT 생성
     */
    public Map<String, Object> generatePpt(Long noticeId) {
        try {
            return webClient.post()
                    .uri(fastApiBaseUrl + "/api/analyze/step3")
                    .bodyValue(Map.of("notice_id", noticeId))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("FastAPI PPT 생성 실패", e);
        }
    }

    /**
     * [Step 4] 스크립트 생성
     */
    public Map<String, Object> generateScript(Long noticeId) {
        try {
            return webClient.post()
                    .uri(fastApiBaseUrl + "/api/analyze/step4")
                    .bodyValue(Map.of("notice_id", noticeId))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("FastAPI 스크립트 생성 실패", e);
        }
    }
}
```

### Step 2: NoticeAnalysisController 생성

```java
// src/main/java/com/example/agent_rnd/controller/NoticeAnalysisController.java

package com.example.agent_rnd.controller;

import com.example.agent_rnd.client.FastApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices/{noticeId}")
public class NoticeAnalysisController {

    private final FastApiClient fastApiClient;

    /**
     * [1] 공고문 분석 (자격요건 체크리스트 + 심층 전략 분석)
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeNotice(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "companyId", required = false) Long companyId
    ) {
        System.out.println("🔍 [Step 1] 공고문 분석 요청 - noticeId: " + noticeId);
        
        Map<String, Object> result = fastApiClient.analyzeNotice(noticeId, companyId);
        
        return ResponseEntity.ok(result);
    }

    /**
     * [2] 유관 RFP 검색
     */
    @PostMapping("/search-rfp")
    public ResponseEntity<Map<String, Object>> searchRfp(
            @PathVariable("noticeId") Long noticeId
    ) {
        System.out.println("🔍 [Step 2] 유관 RFP 검색 요청 - noticeId: " + noticeId);
        
        Map<String, Object> result = fastApiClient.searchSimilarRfp(noticeId);
        
        return ResponseEntity.ok(result);
    }

    /**
     * [3] PPT 생성
     */
    @PostMapping("/generate-ppt")
    public ResponseEntity<Map<String, Object>> generatePpt(
            @PathVariable("noticeId") Long noticeId
    ) {
        System.out.println("📊 [Step 3] PPT 생성 요청 - noticeId: " + noticeId);
        
        Map<String, Object> result = fastApiClient.generatePpt(noticeId);
        
        return ResponseEntity.ok(result);
    }

    /**
     * [4] 스크립트 생성
     */
    @PostMapping("/generate-script")
    public ResponseEntity<Map<String, Object>> generateScript(
            @PathVariable("noticeId") Long noticeId
    ) {
        System.out.println("📝 [Step 4] 스크립트 생성 요청 - noticeId: " + noticeId);
        
        Map<String, Object> result = fastApiClient.generateScript(noticeId);
        
        return ResponseEntity.ok(result);
    }
}
```

---

## 🐍 FastAPI 엔드포인트 추가 (main.py)

```python
# modeling/main.py

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import Optional
import os
import sys

# 프로젝트 루트 경로 추가
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# 모델링 모듈 import
from features.rfp_analysis_checklist.main_analysis import main as run_checklist_analysis
from features.rnd_search.main_search import main as run_search
from features.ppt_maker.main_ppt import run_ppt_generation
from features.ppt_script.main_script import main as run_script_gen

from utils.db_lookup import get_notice_info_by_id
from utils.document_parsing import parse_docx_to_blocks, extract_text_from_pdf

app = FastAPI()

class AnalyzeRequest(BaseModel):
    notice_id: int
    company_id: Optional[int] = 1


# =========================================================
# [Step 1] 공고문 분석 (자격요건 체크리스트 + 심층분석)
# =========================================================
@app.post("/api/analyze/step1")
def analyze_notice(req: AnalyzeRequest):
    """
    공고문 분석
    - DB에서 notice_id로 파일 조회
    - 자격요건 체크리스트 생성 (checklist.json)
    - 심층 전략 분석 (analysis.json)
    
    Returns:
        {
            "status": "success",
            "data": {
                "checklist": {...},
                "analysis": {...}
            }
        }
    """
    print(f"[Step 1] 공고문 분석 요청: notice_id={req.notice_id}")
    
    try:
        # 1. DB에서 공고 정보 조회
        notice_info = get_notice_info_by_id(req.notice_id)
        
        if not notice_info:
            raise HTTPException(status_code=404, detail="공고를 찾을 수 없습니다")
        
        # 2. 분석 실행 (임시: 파일 경로는 고정)
        # 실제로는 DB에서 파일 경로 조회 후 처리
        result = run_checklist_analysis()
        
        # 3. 결과 JSON 파일 읽기
        import json
        checklist_path = "data/analysis/checklist.json"
        analysis_path = "data/analysis/analysis.json"
        
        checklist = {}
        analysis = {}
        
        if os.path.exists(checklist_path):
            with open(checklist_path, 'r', encoding='utf-8') as f:
                checklist = json.load(f)
        
        if os.path.exists(analysis_path):
            with open(analysis_path, 'r', encoding='utf-8') as f:
                analysis = json.load(f)
        
        return {
            "status": "success",
            "data": {
                "checklist": checklist,
                "analysis": analysis
            }
        }
        
    except Exception as e:
        print(f"[오류] {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


# =========================================================
# [Step 2] 유관 RFP 검색 (기존 코드 유지)
# =========================================================
@app.post("/api/analyze/step2")
def search_similar_rfp(req: AnalyzeRequest):
    """
    유관 RFP 검색
    - ChromaDB 벡터 검색
    - LLM 분석 보고서 생성
    
    Returns:
        {
            "status": "success",
            "data": {
                "summary_opinion": "...",
                "track_a_comparison": [...],
                "track_b_comparison": [...],
                "strategies": [...]
            }
        }
    """
    print(f"[Step 2] 유관 RFP 검색: notice_id={req.notice_id}")
    
    try:
        result = run_search(notice_id=req.notice_id)
        
        return {
            "status": "success",
            "data": result
        }
        
    except Exception as e:
        print(f"[오류] {e}")
        raise HTTPException(status_code=500, detail=str(e))


# =========================================================
# [Step 3] PPT 생성
# =========================================================
@app.post("/api/analyze/step3")
def generate_ppt(req: AnalyzeRequest):
    """
    발표 자료 제작 (PPT 생성)
    - LangGraph 워크플로우 실행
    - PPT 파일 생성
    
    Returns:
        {
            "status": "success",
            "data": {
                "ppt_path": "...",
                "slides_count": 15
            }
        }
    """
    print(f"[Step 3] PPT 생성 요청: notice_id={req.notice_id}")
    
    try:
        # 1. DB에서 공고 정보 조회 (RFP 텍스트)
        notice_info = get_notice_info_by_id(req.notice_id)
        
        # 2. PPT 생성 실행
        # rfp_text는 빈 문자열 전달 (main_ppt.py 내부에서 파일 자동 로드)
        final_state = run_ppt_generation(rfp_text="")
        
        if final_state and final_state.get('final_ppt_path'):
            return {
                "status": "success",
                "data": {
                    "ppt_path": final_state['final_ppt_path'],
                    "slides_count": len(final_state.get('slides', []))
                }
            }
        else:
            raise Exception("PPT 생성 실패")
            
    except Exception as e:
        print(f"[오류] {e}")
        raise HTTPException(status_code=500, detail=str(e))


# =========================================================
# [Step 4] 스크립트 생성 (기존 코드 유지)
# =========================================================
@app.post("/api/analyze/step4")
def generate_script():
    """
    스크립트 및 예상질문 생성
    - PPT 파일 읽기
    - 발표 대본 생성
    - 예상 Q&A 생성
    
    Returns:
        {
            "status": "success",
            "message": "대본 생성 완료"
        }
    """
    print("[Step 4] 대본 생성 요청")
    
    try:
        run_script_gen()
        
        return {
            "status": "success",
            "message": "대본 생성 완료"
        }
        
    except Exception as e:
        print(f"[오류] {e}")
        raise HTTPException(status_code=500, detail=str(e))


# =========================================================
# 파일 파싱 (기존 코드 유지)
# =========================================================
@app.post("/parse")
async def parse_notice(file: UploadFile = File(...)):
    """
    파일 파싱 (PDF, DOCX)
    """
    import uuid
    
    os.makedirs("tmp", exist_ok=True)
    ext = os.path.splitext(file.filename)[1].lower()
    tmp_path = os.path.join("tmp", f"{uuid.uuid4().hex}{ext}")

    try:
        content = await file.read()
        with open(tmp_path, "wb") as f:
            f.write(content)

        if ext == ".pdf":
            result = {
                "file_type": "pdf",
                "pages": extract_text_from_pdf(tmp_path)
            }
        elif ext == ".docx":
            result = {
                "file_type": "docx",
                "content": parse_docx_to_blocks(tmp_path, "tmp")
            }
        else:
            return JSONResponse(
                status_code=400,
                content={"error": f"Unsupported extension: {ext}"}
            )

        return JSONResponse(content=result, status_code=200)

    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={"error": str(e)}
        )
    finally:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)


@app.get("/health")
def health_check():
    return {"status": "ok", "message": "FastAPI is running"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
```

---

## ⚛️ 프론트엔드 연동 코드

### ProcessPage.tsx 수정

```typescript
// ProcessPage.tsx (질문 파일의 코드 기반)

import React, { useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";

const ProcessPage: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const noticeId = location.state?.noticeId as number | undefined;

    const [loading, setLoading] = useState(false);

    // ============================================
    // [1] 공고문 분석
    // ============================================
    const handleAnalysis = async (id: number) => {
        try {
            setLoading(true);
            
            const response = await fetch(`/api/notices/${id}/analyze`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    companyId: 1 // 로그인 유저의 회사 ID (임시: 1)
                })
            });

            const result = await response.json();

            if (result.status === 'success') {
                // 결과 페이지로 이동
                navigate('/process/analysis/result', {
                    state: {
                        noticeId: id,
                        checklist: result.data.checklist,
                        analysis: result.data.analysis
                    }
                });
            } else {
                alert('분석 실패: ' + (result.message || '알 수 없는 오류'));
            }
        } catch (error) {
            console.error('분석 오류:', error);
            alert('분석 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // ============================================
    // [2] 유관 RFP 검색
    // ============================================
    const handleRFPSearch = async (id: number) => {
        try {
            setLoading(true);
            
            const response = await fetch(`/api/notices/${id}/search-rfp`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            const result = await response.json();

            if (result.status === 'success') {
                navigate('/process/rfp/result', {
                    state: {
                        noticeId: id,
                        rfpData: result.data
                    }
                });
            }
        } catch (error) {
            console.error('RFP 검색 오류:', error);
            alert('RFP 검색 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // ============================================
    // [3] PPT 생성
    // ============================================
    const handleAnnounce = async (id: number) => {
        try {
            setLoading(true);
            
            const response = await fetch(`/api/notices/${id}/generate-ppt`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            const result = await response.json();

            if (result.status === 'success') {
                alert(`PPT 생성 완료!\n슬라이드 수: ${result.data.slides_count}장`);
                
                // PPT 다운로드 링크 제공 (추가 구현 필요)
                // window.open(result.data.ppt_path);
                
                navigate('/process/announce/result', {
                    state: {
                        noticeId: id,
                        pptPath: result.data.ppt_path,
                        slidesCount: result.data.slides_count
                    }
                });
            }
        } catch (error) {
            console.error('PPT 생성 오류:', error);
            alert('PPT 생성 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // ============================================
    // [4] 스크립트 생성
    // ============================================
    const handleScript = async (id: number) => {
        try {
            setLoading(true);
            
            const response = await fetch(`/api/notices/${id}/generate-script`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            const result = await response.json();

            if (result.status === 'success') {
                alert('스크립트 생성 완료!');
                
                navigate('/process/script/result', {
                    state: { noticeId: id }
                });
            }
        } catch (error) {
            console.error('스크립트 생성 오류:', error);
            alert('스크립트 생성 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // UI 렌더링 (기존 코드 유지)
    return (
        <div>
            {loading && <LoadingOverlay />}
            
            <ButtonGroup>
                <ProcessBtn onClick={() => handleAnalysis(noticeId)}>
                    <h3>공고문 분석</h3>
                </ProcessBtn>
                
                <ProcessBtn onClick={() => handleRFPSearch(noticeId)}>
                    <h3>유관 RFP 검색</h3>
                </ProcessBtn>
                
                <ProcessBtn onClick={() => handleAnnounce(noticeId)}>
                    <h3>발표자료 제작</h3>
                </ProcessBtn>
                
                <ProcessBtn onClick={() => handleScript(noticeId)}>
                    <h3>스크립트 생성</h3>
                </ProcessBtn>
            </ButtonGroup>
        </div>
    );
};

export default ProcessPage;
```

---

## 📂 파일 처리 흐름

### 방법 1: DB 기반 (권장)

```python
# utils/db_file_loader.py (새 파일 생성)

import os
import pymysql
from dotenv import load_dotenv

load_dotenv()

def get_notice_files_by_id(notice_id: int) -> dict:
    """
    DB에서 notice_id로 파일 경로 조회
    
    Returns:
        {
            "notice_file": "path/to/notice.pdf",
            "attachments": ["path/to/attachment1.docx", ...]
        }
    """
    conn = pymysql.connect(
        host=os.environ["DB_HOST"],
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"],
        db=os.environ["DB_NAME"],
        charset='utf8mb4'
    )
    
    try:
        with conn.cursor() as cursor:
            # notice_files 테이블에서 메인 파일 조회
            cursor.execute("""
                SELECT file_path FROM notice_files 
                WHERE notice_id = %s AND file_type = 'NOTICE'
                LIMIT 1
            """, (notice_id,))
            
            notice_file = cursor.fetchone()
            
            # 첨부파일 조회
            cursor.execute("""
                SELECT file_path FROM notice_attachments
                WHERE notice_id = %s AND status = 'DONE'
            """, (notice_id,))
            
            attachments = [row[0] for row in cursor.fetchall()]
            
            return {
                "notice_file": notice_file[0] if notice_file else None,
                "attachments": attachments
            }
    finally:
        conn.close()


def copy_files_to_temp(notice_id: int, target_dir: str) -> list:
    """
    DB에서 조회한 파일을 임시 폴더로 복사
    """
    import shutil
    
    files = get_notice_files_by_id(notice_id)
    os.makedirs(target_dir, exist_ok=True)
    
    copied = []
    
    # 메인 파일 복사
    if files['notice_file']:
        dest = os.path.join(target_dir, os.path.basename(files['notice_file']))
        shutil.copy(files['notice_file'], dest)
        copied.append(dest)
    
    # 첨부파일 복사
    for att in files['attachments']:
        dest = os.path.join(target_dir, os.path.basename(att))
        shutil.copy(att, dest)
        copied.append(dest)
    
    return copied
```

### FastAPI에서 활용

```python
# main.py에서 사용

from utils.db_file_loader import copy_files_to_temp

@app.post("/api/analyze/step1")
def analyze_notice(req: AnalyzeRequest):
    # 1. DB에서 파일 조회 및 복사
    temp_dir = f"data/notice_input/{req.notice_id}"
    copied_files = copy_files_to_temp(req.notice_id, temp_dir)
    
    if not copied_files:
        raise HTTPException(status_code=404, detail="공고 파일을 찾을 수 없습니다")
    
    # 2. 분석 실행
    result = run_checklist_analysis()
    
    # ...
```

---

## 🚀 실행 순서

### 1. FastAPI 서버 시작

```bash
# PowerShell (Windows)
cd C:\big_project\modeling
python -m venv venv
.\venv\Scripts\activate
pip install -r requirements.txt

# 서버 실행
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### 2. Spring Boot 서버 시작

```bash
# PowerShell
cd C:\big_project\agent_rnd
./gradlew bootRun

# 또는
java -jar build/libs/agent_rnd-0.0.1-SNAPSHOT.jar
```

### 3. 프론트엔드 실행

```bash
# PowerShell
cd C:\big_project\frontend
npm install
npm run dev
```

### 4. 테스트

```javascript
// 브라우저 콘솔에서 테스트
fetch('http://localhost:8080/api/notices/1/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ companyId: 1 })
})
.then(res => res.json())
.then(data => console.log(data));
```

---

## ⚙️ 설정 파일

### application.yml (Spring Boot)

```yaml
fastapi:
  base-url: http://localhost:8000
```

### .env (FastAPI)

```env
# DB 설정
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=randi_db

# API 키
GEMINI_API_KEY=your_gemini_key
ANTHROPIC_API_KEY=your_claude_key
```

---

## ✅ 체크리스트

- [ ] FastAPI `main.py`에 4개 엔드포인트 추가
- [ ] Spring Boot `FastApiClient.java` 확장
- [ ] Spring Boot `NoticeAnalysisController.java` 생성
- [ ] 프론트엔드 `ProcessPage.tsx` 수정
- [ ] DB 파일 조회 로직 구현 (`db_file_loader.py`)
- [ ] 서버 3개 모두 실행
- [ ] 테스트 완료

---

## 📌 주의사항

1. **CORS 설정**: Spring Boot에서 CORS 허용 필요
2. **파일 경로**: DB에 저장된 파일 경로가 실제 서버와 일치해야 함
3. **동시성**: 여러 사용자 동시 요청 시 파일 충돌 방지 (폴더 분리)
4. **에러 처리**: 모든 API에 try-catch 추가
5. **로딩 상태**: 프론트엔드에서 로딩 스피너 필수

---

질문이나 추가 구현이 필요한 부분이 있으면 말씀해주세요!
