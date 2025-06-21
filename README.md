# YouTube Lecture Helper

유튜브 링크를 입력하면 **강의 내용을 요약하고 퀴즈를 자동 생성해주는 서비스**입니다.  
공부할 때 유용하게 활용할 수 있도록 설계되었습니다.

🔗 [https://cpyt.sytes.net](https://cpyt.sytes.net)

## 기술 스택

<p align="left">
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/OpenAI-412991?style=for-the-badge&logo=openai&logoColor=white" alt="OpenAI"/>
  <img src="https://img.shields.io/badge/Google_OAuth-4285F4?style=for-the-badge&logo=google&logoColor=white" alt="Google OAuth"/>
</p>

## 개발 환경

- Java 17 (JDK)
- Spring Boot 3.4.4
- Gradle 8.13

---

## 기능
- 유튜브 영상의 자막을 분석해 자동으로 **요약** 제공
- 내용을 바탕으로 **퀴즈 생성**
- 난이도 조절 및 문제 개수 선택
- 공동 퀴즈 (여러 명이 풀 수 있는 퀴즈) 생성 가능

---

## 로컬 실행 방법

### 1. `secret.properties` 파일 생성

`src/main/resources/secret.properties` 경로에 아래 내용을 포함한 파일을 생성:

<pre><code>db.username=username
db.password=password

app.jwt.secret=jwtsecret
app.jwt.expiration-ms=86400000
app.jwt.cookie-name=accessToken

google.client-id=clientId

transcript.api.url=transcript-server-url
</code></pre>

### 2. `.env` 파일 생성

루트 디렉토리에 다음 내용을 포함한 `.env` 파일을 생성하세요:

<pre><code>OPENAI_API_KEY=openai-api-key
</code></pre>

### 3. `.env` 파일 포함하여 실행
---
