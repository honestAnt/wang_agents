# Work Summary — Java Monolith Consolidation

**Date**: 2026-05-27
**Task**: 整合 14 个 Java 微服务为单一 agent-application 模块，通过 spring.factories 自注册

## 做了什么

1. **创建 agent-application 模块** — 单一 Spring Boot 可部署 JAR，依赖全部 13 个业务模块
2. **所有 service 模块改为 library** — 删除 `spring-boot-maven-plugin` 执行能力（改为 `skip=true`），添加 `spring-boot-autoconfigure` 依赖
3. **每个模块添加 AutoConfiguration** — `@AutoConfiguration + @ComponentScan + @EnableJpaRepositories + @EntityScan`
4. **每个模块创建 spring.factories** — `org.springframework.boot.autoconfigure.EnableAutoConfiguration=...`
5. **Flyway 迁移重命名** — 解决版本冲突，分配独立版本号范围（V10~V130）
6. **合并 gateway-service** — 安全配置吸收到 agent-application 的 WebSecurityConfig
7. **统一 application.yml** — 删除各模块的 server.port/spring.application.name/flyway.table

## 遇到什么问题

### 1. sed 损坏 POM 文件
- 问题：用 sed 批量修改 POM 时，skill/memory/prompt/billing-service 的 `<build>` 和 `</project>` 被裁剪掉
- 原因：sed 正则匹配到多行 `<plugin>...</plugin>` 时范围过大
- 解决：用 `Write` 工具重写这 4 个文件，后续用 perl 替 sed

### 2. auth-service SecurityConfig 冲突
- 问题：auth-service 的 SecurityFilterChain bean 与 agent-application 的 WebSecurityConfig 冲突
- 解决：auth-service SecurityConfig 保留 JwtAuthenticationConverter（@ConditionalOnMissingBean），移除 SecurityFilterChain/CorsConfigurationSource

### 3. Flyway 版本冲突
- 问题：每个模块都有 V1__init_*.sql，都声称版本 1
- 解决：重命名为 V<模块版本号>_0_1__*.sql（如 V60_0_1__init_agent_schema.sql）

## 沉淀经验

- 批量修改 XML 用 perl 的 `-0pe` 比 sed 更可靠（支持 .*? 非贪婪匹配）
- Spring Boot 3.x 中 `@AutoConfiguration` 注解 + `spring.factories` 已经替代旧 `@Configuration` + `AutoConfiguration.imports`
- 模块化 monolith 的关键：统一 SecurityFilterChain 在 application 层，业务模块只提供自己的 components
