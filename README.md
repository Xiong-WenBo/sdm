# 🏠 宿舍管理系统 (Dormitory Management System)

<div align="center">

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-green.svg)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-latest-blue.svg)](https://element-plus.org/)

**一站式宿舍管理解决方案 | 角色权限控制 | 智能通知推送**

</div>

---

## 📖 项目简介

宿舍管理系统（SDM）是一款基于 Spring Boot + Vue 3 的现代化宿舍管理平台，专为高校宿舍管理设计。系统支持四种角色（超级管理员、宿管、辅导员、学生），提供住宿分配、查寝管理、报修处理、请假审批等核心功能，帮助学校实现宿舍管理的数字化和智能化。

### ✨ 主要特性

- 🔐 **多角色权限控制** - 基于 RBAC 的权限管理，支持四种角色
- 🏢 **楼栋房间管理** - 可视化楼栋、房间信息管理
- 🛏️ **智能住宿分配** - 支持分配、退宿、调宿等操作
- 📋 **查寝管理** - 批量录入查寝记录，自动筛选未归学生
- 🔔 **消息推送** - 定时任务自动推送查寝通知
- 🔧 **报修处理** - 完整的报修流程，状态跟踪
- 📊 **数据统计** - 多维度数据展示，支持导出

---

## 🛠️ 技术栈

### 后端技术

| 技术 | 版本 | 描述 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.2.4 | 核心框架 |
| Spring Security | 6.x | 安全框架 |
| JWT | 0.12.5 | Token 认证 |
| MyBatis | 3.5.15 | ORM 框架 |
| MySQL | 5.7+ | 数据库 |

### 前端技术

| 技术 | 版本 | 描述 |
|------|------|------|
| Vue | 3.5.30 | 渐进式框架 |
| Vite | 8.0.0 | 构建工具 |
| Element Plus | 2.13.5 | UI 组件库 |
| Vue Router | 4.6.4 | 路由管理 |
| Axios | 1.13.6 | HTTP 客户端 |
| ECharts | 6.0.0 | 数据可视化 |

---

## 🚀 快速开始

### 环境要求

- JDK 17+
- MySQL 5.7+
- Node.js 16+
- Maven 3.6+

### 1. 克隆项目

```bash
git clone https://github.com/Xiong-WenBo/sdm.git
cd sdm
```

### 2. 数据库配置

1. 创建数据库并导入初始化脚本：

```sql
CREATE DATABASE sdm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sdm;
-- 导入 数据库结构.sql 文件
```

2. 修改后端配置文件 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sdm?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

### 3. 后端启动

```bash
cd backend
# Maven 安装依赖
mvn clean install
# 启动应用
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

### 4. 前端启动

```bash
cd frontend
# 安装依赖
npm install
# 启动开发服务器
npm run dev
```

前端服务将在 `http://localhost:5173` 启动

---

## 📋 功能模块

### 角色权限说明

| 功能 | SUPER_ADMIN | DORM_ADMIN | COUNSELOR | STUDENT |
|------|-------------|------------|-----------|---------|
| 用户管理 | ✅ | ❌ | ❌ | ❌ |
| 楼栋管理 | ✅ | ❌ | ❌ | ❌ |
| 房间管理 | ✅ | ✅ (本楼) | ❌ | ❌ |
| 学生管理 | ✅ | ❌ | ✅ (本班) | ❌ |
| 宿舍分配 | ✅ | ✅ (本楼) | ❌ | ❌ |
| 查寝管理 | ✅ | ✅ (本楼) | ✅ (本班) | ❌ |
| 报修管理 | ✅ | ✅ (本楼) | ❌ | ✅ (我的) |
| 请假管理 | ✅ | ✅ (查看) | ✅ (审批) | ✅ (我的) |

### 核心功能

#### 1. 用户管理 👥
- 用户 CRUD 操作
- 角色分配
- 批量导入学生
- 权限控制

#### 2. 楼栋管理 🏢
- 楼栋信息管理
- 管理员绑定
- 楼栋数据展示

#### 3. 房间管理 🚪
- 房间 CRUD
- 入住人数统计
- 房间状态管理（可住/已满/维修）

#### 4. 宿舍分配 🛏️
- 学生入住分配
- 退宿处理
- 调宿操作
- 住宿记录查询

#### 5. 查寝管理 📝
- 批量录入查寝记录
- 查寝状态：正常/晚归/未归/请假
- 自动筛选未归学生
- 定时任务推送通知

#### 6. 报修管理 🔧
- 学生提交报修
- 宿管处理报修
- 状态跟踪：待处理/处理中/已完成/已拒绝
- 优先级分类

#### 7. 请假管理 📅
- 学生提交请假申请
- 辅导员审批
- 请假状态管理

#### 8. 消息中心 🔔
- 站内信通知
- 未读消息提醒
- 消息类型分类

---

## 📁 项目结构

```
sdm/
├── backend/                          # 后端项目
│   ├── src/main/java/com/sdm/backend/
│   │   ├── config/                   # 配置类
│   │   ├── controller/               # 控制器层
│   │   ├── dto/                      # 数据传输对象
│   │   ├── entity/                   # 实体类
│   │   ├── filter/                   # 过滤器
│   │   ├── mapper/                   # MyBatis Mapper
│   │   ├── service/                  # 服务层
│   │   └── util/                     # 工具类
│   ├── src/main/resources/
│   │   ├── mapper/                   # MyBatis XML
│   │   └── application.yml           # 配置文件
│   └── pom.xml                       # Maven 配置
│
├── frontend/                         # 前端项目
│   ├── src/
│   │   ├── components/               # 组件
│   │   ├── config/                   # 配置文件
│   │   ├── layouts/                  # 布局组件
│   │   ├── router/                   # 路由配置
│   │   ├── utils/                    # 工具函数
│   │   └── views/                    # 页面组件
│   ├── package.json                  # 依赖配置
│   └── vite.config.js               # Vite 配置
│
├── 数据库结构.sql                     # 数据库初始化脚本
└── README.md                         # 项目说明
```

---

## 🔒 安全说明

- 密码采用 BCrypt 加密存储
- JWT Token 认证，有效期 24 小时
- 基于角色的访问控制（RBAC）
- 前后端双重权限验证

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 超级管理员 | admin | admin123 |
| 宿管 | dormadmin | dorm123 |
| 辅导员 | counselor | counsel123 |
| 学生 | student | student123 |

**⚠️ 首次使用时请修改默认密码！**

---

## 📊 数据库设计

系统包含 8 张核心表：

1. `user` - 用户表（基础认证信息）
2. `building` - 宿舍楼表
3. `room` - 房间表
4. `student` - 学生信息表
5. `assignment` - 住宿分配表
6. `attendance` - 考勤查寝表
7. `repair` - 报修表
8. `leave_request` - 请假申请表
9. `message` - 消息通知表

详细结构请参考 `数据库结构.sql` 文件。

---

## 🧪 测试

### 后端测试

```bash
cd backend
mvn test
```

### 前端测试

```bash
cd frontend
npm run test
```

---

## 📝 开发规范

- 遵循阿里巴巴 Java 开发规范
- 前端遵循 Vue 3 组合式 API 风格
- 使用 ESLint + Prettier 统一代码格式
- Git 提交信息使用语义化格式

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 开源协议

本项目采用 MIT 协议开源 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

## 👨‍💻 作者

- **Xiong-WenBo** - [GitHub](https://github.com/Xiong-WenBo)

---

## 🙏 致谢

感谢以下开源项目：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)
- [MyBatis](https://mybatis.org/mybatis-3/)
- [JWT](https://jwt.io/)

---

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- Email: xwb1123727965@gmail.com
- GitHub Issues: [提交 Issue](https://github.com/Xiong-WenBo/sdm/issues)

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐️ Star 支持！**

Made with ❤️ by Xiong-WenBo

</div>
