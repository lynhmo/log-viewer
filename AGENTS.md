# AGENTS.md – Project Agent Configuration
 
> File này được đọc bởi **OpenCode** và **Claude Code** (tương thích hoàn toàn).  
> Cung cấp context chung cho mọi agent session trong project này.
 
---
 
## Agents có sẵn
 
Invoke bằng cách `@mention` trong chat:
 
| Agent | Lệnh gọi | Vai trò | Mode |
|-------|----------|---------|------|
| Coder | `@coder` | Viết code, convention, unit test, doc | primary |
| Tester | `@tester` | Test case, automation script, coverage | subagent |
| BA | `@ba` | Phân tích nghiệp vụ, User Story, AC | subagent |
| PM | `@pm` | Sprint planning, phân công task, status | subagent |
| Secretary | `@secretary` | Meeting notes, action items, brief | subagent |
| DevOps | `@devops` | CI/CD, Docker, deploy, monitoring | subagent |
| Security | `@security` | OWASP audit, vulnerability review | subagent |
 
> 💡 **Tip:** Dùng `Tab` để switch giữa primary agents (coder ↔ plan).  
> Dùng `@agent-name` để gọi trực tiếp subagent trong bất kỳ conversation nào.
 
---
 
## Hướng dẫn sử dụng nhanh
 
### Làm tính năng mới
```
1. @ba   → Clarify requirement, viết User Story
2. @pm   → Breakdown task, assign, estimate
3. @coder → Implement với convention + test
4. @tester → Viết test case đầy đủ
5. @security → Review trước khi merge
```
 
### Sau cuộc họp
```
@secretary [paste transcript hoặc ghi chú thô]
→ Nhận meeting notes chuẩn + action items với owner/deadline
```
 
### Debug / Review PR
```
@coder review file này và suggest improvements
@security check file này có lỗ hổng không
```
 
---
 
## Project Context
 
```
Stack:            Java 17 + Spring Boot + Thymeleaf
Build tool:       Maven
Test framework:   JUnit 5 + Mockito + Spring Boot Test
Branch strategy:  [GitFlow / Trunk-based – cập nhật theo team]
```
 
### Build & Test commands
```bash
# Build (bỏ qua test)
mvn clean package -DskipTests
 
# Build + chạy toàn bộ test
mvn clean verify
 
# Chỉ chạy test
mvn test
 
# Chạy một test class cụ thể
mvn test -Dtest=UserServiceTest
 
# Chạy một method cụ thể
mvn test -Dtest=UserServiceTest#shouldReturnUserById
 
# Chạy ứng dụng local
mvn spring-boot:run
 
# Chạy với profile cụ thể
mvn spring-boot:run -Dspring-boot.run.profiles=dev
 
# Check code style (nếu dùng Checkstyle)
mvn checkstyle:check
 
# Xem dependency tree
mvn dependency:tree
```
 
### Folder structure (Maven standard)
```
src/
  main/
    java/com/[company]/[project]/
      controller/       # @Controller, @RestController – nhận request
      service/          # @Service – business logic
      repository/       # @Repository – data access (JPA/JDBC)
      model/            # Entity, Domain object
      dto/              # Data Transfer Object (request/response)
      config/           # @Configuration – Spring config beans
      exception/        # Custom exceptions, GlobalExceptionHandler
      util/             # Utility classes
    resources/
      templates/        # Thymeleaf .html templates
      static/           # CSS, JS, images
      application.yml   # Config chính
      application-dev.yml
      application-prod.yml
  test/
    java/com/[company]/[project]/
      controller/       # MockMvc tests
      service/          # Unit tests với Mockito
      repository/       # @DataJpaTest
      integration/      # @SpringBootTest – full context
    resources/
      application-test.yml
```
 
---
 
## Conventions chung
 
**Ngôn ngữ & Java style:**
- Java 17 – dùng record, sealed class, text block khi phù hợp
- Naming: `PascalCase` cho class, `camelCase` cho method/variable, `UPPER_SNAKE_CASE` cho constant
- Mỗi class một trách nhiệm – Controller chỉ handle HTTP, Service chứa business logic, Repository chỉ data access
- Dùng `final` cho field inject qua constructor (không dùng `@Autowired` trên field)
**Spring Boot:**
- Inject dependency qua constructor (không phải field injection)
- Dùng `@Slf4j` (Lombok) cho logging – không dùng `System.out.println`
- Dùng `application.yml` thay vì `application.properties`
- Tất cả config nhạy cảm đặt trong biến môi trường, không hardcode
**Thymeleaf:**
- Template đặt trong `resources/templates/`, phân cấp theo feature
- Dùng Thymeleaf fragments (`th:fragment`) cho layout tái sử dụng (header, footer, nav)
- Dùng `th:object` + `th:field` cho form binding
**Testing:**
- Unit test: JUnit 5 + Mockito – không load Spring context
- Controller test: `@WebMvcTest` + MockMvc
- Repository test: `@DataJpaTest` – dùng H2 in-memory
- Integration test: `@SpringBootTest` – đánh dấu rõ `@Tag("integration")`
- Test method name: `should[ExpectedBehavior]_when[Condition]`
**Error handling:**
- Tất cả exception xử lý tập trung tại `GlobalExceptionHandler` (`@ControllerAdvice`)
- Không bao giờ return `null` – dùng `Optional<T>` hoặc throw exception rõ ràng
- Log đầy đủ context khi catch exception (không chỉ `e.getMessage()`)
---
 
## Không làm trong project này
 
- ❌ `System.out.println` trong production code – dùng `log.info()` / `log.error()`
- ❌ Hardcode URL, password, API key – dùng `application.yml` + env variable
- ❌ Business logic trong Controller – chỉ delegate sang Service
- ❌ `@Autowired` trên field – dùng constructor injection
- ❌ Merge vào `main` mà không có test
- ❌ `--force push` lên shared branch
- ❌ Dùng `Optional.get()` mà không check `isPresent()` trước
---
 
*Agents được định nghĩa trong `.opencode/agents/`*  
*Tương thích: OpenCode ≥ v1.0 | Claude Code ≥ v1.0*