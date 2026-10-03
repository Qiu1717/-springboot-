# 学生选课管理系统

基于 Spring Boot + Vue 的课程设计项目，实现学生在线选课、成绩录入、公告发布与基于角色的权限控制。

系统面向管理员、教师、学生三类角色，覆盖从课程公告发布、学生选课退课、教师成绩录入到后台权限分配的完整教务流程，并在基础业务之上集成 Redis 缓存、RabbitMQ 异步邮件、Spring Security + JWT 鉴权与 AI 智能问答。

## 技术栈

| 层次 | 技术 | 版本 |
| --- | --- | --- |
| 后端框架 | Spring Boot | 2.7.14 |
| 持久层 | MyBatis-Plus | 3.5.3 |
| 安全框架 | Spring Security + JWT | - |
| 缓存 | Redis（Spring Cache） | - |
| 消息队列 | RabbitMQ（异步发邮件） | - |
| 数据库 | MySQL | 8.0 |
| 数据库驱动 | mysql-connector-java | 8.0.33 |
| 前端框架 | Vue | 2.7.14 |
| UI 组件库 | Element UI | 2.15.14 |
| 前端路由/状态 | Vue Router / Vuex | 3.6.5 / 3.6.2 |
| HTTP 客户端 | Axios | 1.6.0 |
| 构建工具 | Maven / Vue CLI | 3.6+ / 5.0.8 |
| JDK | - | 1.8 |

## 功能模块

- **学生**：查看公告、浏览可选课程、选课/退课、查看成绩与学分统计
- **教师**：查看自己教授的课程、按学期/课程/学生筛选选课记录、单个或批量录入成绩
- **管理员**：菜单管理、公告管理、教师管理、学生管理、课程管理、角色权限分配
- **公共**：登录认证、动态菜单、修改密码、忘记密码、站内消息、AI 助手问答、课程资料上传下载

核心业务规则：

- 选课需校验课程状态、容量余量、重复选课；退课仅允许未出成绩的课程
- 选课/退课在同一事务内同步课程已选人数，避免超选
- 教师只能操作自己教授的课程和自己发布的公告（数据权限二次校验）
- 满员课程每日凌晨定时检查并邮件提醒任课教师

## 目录结构

```
.
├── course-selection-backend/          # 后端工程
│   ├── src/main/java/com/edu/course/
│   │   ├── common/       # 统一返回结果、全局异常处理
│   │   ├── config/       # 配置类（Security / Redis / CORS / AI / RabbitMQ）
│   │   ├── controller/   # 7 个控制器，共 58 个 REST 接口
│   │   ├── dto/          # 请求/响应传输对象
│   │   ├── entity/       # 9 个实体类，与 9 张数据表一一对应
│   │   ├── mapper/       # MyBatis-Plus Mapper 接口
│   │   ├── security/     # JWT 过滤器、自定义 UserDetailsService
│   │   ├── service/      # 业务层
│   │   ├── task/         # 定时任务
│   │   └── util/         # JWT 工具、密码工具
│   ├── src/main/resources/
│   │   ├── application.yml   # 主配置（敏感项已改为环境变量占位符）
│   │   └── init.sql          # 建表脚本 + 初始化数据
│   └── pom.xml
├── course-selection-front/           # 前端工程
│   ├── src/
│   │   ├── views/
│   │   │   ├── admin/      # 管理端页面
│   │   │   ├── student/    # 学生端页面
│   │   │   ├── teacher/    # 教师端页面
│   │   │   └── common/     # 公共页面
│   │   ├── utils/           # axios 封装、请求拦截器
│   │   ├── router/          # 路由与权限守卫
│   │   └── App.vue
│   ├── public/
│   └── package.json
├── docs/                   # 课程设计文档（报告、答辩 PPT、ER 图等）
├── .env.example            # 环境变量模板
└── README.md
```

## 数据库设计

共 9 张表：

| 表名 | 说明 |
| --- | --- |
| `user` | 用户表，管理员/教师/学生统一存储，用 `role` 字段区分（0/1/2） |
| `course` | 课程表，含学分、授课教师、容量、已选人数、学期 |
| `selection` | 选课记录表，学生与课程多对多中间表，含 `score` 成绩 |
| `role` | 角色表（ROLE_ADMIN / ROLE_TEACHER / ROLE_STUDENT） |
| `menu` | 菜单权限表，`permission` 存 `admin:xxx:manage` 权限码 |
| `role_menu` | 角色菜单关联表，RBAC 中间表 |
| `notice` | 公告表 |
| `course_file` | 课程资料表 |
| `message` | 站内消息表 |

## 环境准备

需要安装：

- JDK 8+
- Maven 3.6+
- MySQL 8.0
- Redis 5+
- RabbitMQ 3.8+
- Node.js 14+

## 启动步骤

### 1. 初始化数据库

用 MySQL 客户端执行建表脚本：

```bash
mysql -u root -p < course-selection-backend/src/main/resources/init.sql
```

脚本会自动创建 `course_selection` 数据库、9 张表，并写入 5 个演示账号。

### 2. 配置环境变量

复制模板并按需修改：

```bash
cp .env.example .env
```

`.env` 中包含数据库密码、邮箱授权码、DeepSeek API Key 等敏感信息，已被 `.gitignore` 排除，不会提交到仓库。

`application.yml` 中所有敏感项都使用了 `${ENV_VAR:默认值}` 形式，缺失时会回落到默认值（默认值仅适用于本地演示）。

### 3. 启动 Redis 与 RabbitMQ

```bash
redis-server
rabbitmq-server
```

### 4. 启动后端

```bash
cd course-selection-backend
mvn spring-boot:run
```

后端启动在 http://localhost:8080

### 5. 启动前端

```bash
cd course-selection-front
npm install
npm run serve
```

前端启动在 http://localhost:3000，已配置代理将 `/api` 请求转发到后端 8080 端口。

## 演示账号

初始化脚本中内置以下账号，密码均为 `123456`：

| 账号 | 姓名 | 角色 |
| --- | --- | --- |
| `admin` | 系统管理员 | 管理员 |
| `teacher1` | 张老师 | 教师 |
| `teacher2` | 王老师 | 教师 |
| `student1` | 李学生 | 学生 |
| `student2` | 赵学生 | 学生 |

> 正式部署前请立即修改所有默认密码。

## 权限模型

采用 RBAC（基于角色的访问控制）：

- **认证**：登录后签发 JWT，前端存储于 `sessionStorage`，后续请求通过 `Authorization: Bearer <token>` 头携带
- **鉴权**：`SecurityConfig` 负责 URL 级粗粒度拦截，`@PreAuthorize` 注解负责方法级细粒度校验
- **数据权限**：教师只能操作自己教授的课程和自己发布的公告，代码中通过二次校验实现

## 说明

- AI 对话功能依赖 DeepSeek API，未配置 `DEEPSEEK_API_KEY` 时该功能不可用，其余功能不受影响
- 课程资料上传依赖 `file.upload-dir` 配置的目录，默认 `./uploads`，需保证进程有写权限
- 开发阶段如需查看 SQL 日志，把 `application.yml` 中 `com.edu.course.mapper` 的日志级别改为 `DEBUG`

## 配置项说明

所有敏感配置均通过环境变量注入，`application.yml` 中以 `${ENV_VAR:默认值}` 形式声明。**默认值仅适用于本地演示，生产环境必须全部覆盖。**

| 环境变量 | 对应配置项 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `DB_URL` | `spring.datasource.url` | `jdbc:mysql://localhost:3306/course_selection?...` | MySQL 连接串 |
| `DB_USERNAME` | `spring.datasource.username` | `root` | 数据库账号 |
| `DB_PASSWORD` | `spring.datasource.password` | `123456` | 数据库密码，**必改** |
| `REDIS_HOST` / `REDIS_PORT` | `spring.redis.*` | `localhost` / `6379` | Redis 缓存连接 |
| `MAIL_HOST` / `MAIL_PORT` | `spring.mail.*` | `smtp.qq.com` / `587` | SMTP 服务器 |
| `MAIL_USERNAME` | `spring.mail.username` | 空 | 发件邮箱 |
| `MAIL_PASSWORD` | `spring.mail.password` | 空 | 邮箱 SMTP 授权码，**必填**（非 QQ 密码） |
| `MAIL_ADMIN_EMAIL` | `mail.admin-email` | `admin@example.com` | 接收教师密码重置申请 |
| `RABBITMQ_HOST` | `spring.rabbitmq.host` | `localhost` | RabbitMQ 地址 |
| `RABBITMQ_USERNAME` | `spring.rabbitmq.username` | `guest` | RabbitMQ 账号 |
| `RABBITMQ_PASSWORD` | `spring.rabbitmq.password` | `guest` | RabbitMQ 密码 |
| `JWT_SECRET` | `jwt.secret` | 占位串 | JWT 签名密钥，生产环境**必须改为不少于 32 位随机串** |
| `SERVER_PORT` | `server.port` | `8080` | 后端端口 |
| `FILE_UPLOAD_DIR` | `file.upload-dir` | `./uploads` | 上传文件存储目录 |
| `DEEPSEEK_API_URL` | `ai.deepseek.api-url` | `https://api.deepseek.com` | AI 接口地址 |
| `DEEPSEEK_API_KEY` | `ai.deepseek.api-key` | 空 | AI Key，留空则 AI 功能不可用 |

## 接口一览

共 7 个控制器、58 个 REST 接口，统一挂在 `/api` 前缀下。

| 模块 | 前缀 | 接口数 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `/api/admin` | 33 | 菜单、公告、教师、学生、课程、角色权限管理 |
| 认证 | `/api/auth` | 6 | 登录、用户信息、改密、忘记密码、动态菜单 |
| 学生 | `/api/student` | 6 | 公告、可选课程、选课、退课、成绩统计 |
| 课程资料 | `/api/files` | 5 | 我的课程、上传、列表、下载、删除 |
| 教师 | `/api/teacher` | 4 | 我的课程、选课查询、单个/批量录入成绩 |
| 站内消息 | `/api/messages` | 3 | 消息列表、未读数、标记已读 |
| AI 助手 | `/api/ai` | 1 | 智能问答 |

## 使用方法示例

### 登录获取 Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student1","password":"123456"}'
```

响应中的 `data.token` 即为 JWT，后续请求需携带：

```http
Authorization: Bearer <token>
```

### 学生选课

```bash
# 查看可选课程（已启用且未满员）
curl http://localhost:8080/api/student/available-courses \
  -H "Authorization: Bearer <token>"

# 选课
curl -X POST "http://localhost:8080/api/student/select?courseId=1" \
  -H "Authorization: Bearer <token>"

# 查看我的选课与成绩
curl http://localhost:8080/api/student/my-selections \
  -H "Authorization: Bearer <token>"

# 退课（仅未出成绩的课程可退）
curl -X POST "http://localhost:8080/api/student/drop?courseId=1" \
  -H "Authorization: Bearer <token>"
```

### 教师录入成绩

```bash
# 查询选课记录
curl "http://localhost:8080/api/teacher/selections?term=2025-2026-1" \
  -H "Authorization: Bearer <token>"

# 单个录入
curl -X PUT http://localhost:8080/api/teacher/score \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"selectionId":1,"score":88}'

# 批量录入
curl -X PUT http://localhost:8080/api/teacher/score/batch \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"scores":[{"selectionId":1,"score":88},{"selectionId":2,"score":92}]}'
```

## 注意事项

- **默认密码**：演示账号密码统一为 `123456`，正式使用前务必全部修改
- **JWT 密钥**：默认密钥为占位串，部署前必须替换为随机字符串，否则 anyone 都能伪造 Token
- **数据库初始化**：`init.sql` 会创建数据库和 9 张表，重复执行使用 `IF NOT EXISTS` 不会覆盖已有数据
- **AI 功能降级**：未配置 `DEEPSEEK_API_KEY` 时 AI 对话不可用，其余功能不受影响
- **文件权限**：上传目录需保证后端进程有写权限
- **端口占用**：后端 8080、前端 3000，如被占用可通过 `SERVER_PORT` 或 `vue.config.js` 修改

## License

MIT
