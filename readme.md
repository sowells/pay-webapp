### 소개
SpringMVC와 SprnigDataJPA를 이용한 샘플 웹 어플리케이션입니다.

### 실행방법
- IDE 환경<br>PayWebApplication을 실행합니다.
- Maven으로 실행<br>./mvnw spring-boot:run 

### 프로젝트 구성
- RestAPI를 제공을 위해 SpringBoot, SpringMVC을 사용합니다.
- 데이터베이스는 H2를 사용합니다.
<br> 테스트 용도로 빠르게 구성하기 위해 선택했습니다. 
- 데이터베이스 질의는 SpringDataJpa를 사용합니다.  
 
### 시스템 아키텍처
- Rest API 서버와 Database로 구성된 간단한 아키텍처입니다.
- Rest API 서버는 무상태성(Stateless)을 가지므로 수평 확장이 가능합니다. <br> 
다만 이를 위해서는 데이터베이스의 트래픽 대응 전략이 마련되어야 합니다.
 <br>(이 어플리케이션에는 데이터베이스 관련 전략이 생략되었습니다.) 

### 프로젝트 패키지 구조
- com.sowells.pay.webapp.gift 이하에 Gift 피쳐 관련 코드가 위치합니다.
- 각 피쳐 패키지 이하는 역할별로 패키지가 구분됩니다.

### 테스트 코드 구조
- GiftServiceTest에서는 정상 케이스, 엣지 케이스를 포함아여 테스트합니다.
- GitControllerTest에서는 Http 요청/응답이 잘 동작하는지를 테스트합니다. 
